import Accelerate
import Foundation
internal import OnnxRuntimeBindings

@objc(VoiceActivityDetector)
@objcMembers
public final class SwiftVoiceActivityDetector: NSObject, IosVadProtocol {

    private let inferenceQueue = DispatchQueue(
        label: "com.sam.talkdraft.vad.inference",
        qos: .userInitiated
    )

    public override init() {
        super.init()
    }

    private var ortEnv: ORTEnv?
    private var ortSession: ORTSession?
    private var isInitialized = false

    // Silero VAD streaming contract
    private static let inputAudioName = "input"
    private static let inputStateName = "state"
    private static let inputSampleRateName = "sr"

    private static let outputProbName = "output"
    private static let outputStateName = "stateN"

    // Silero VAD state: [2, 1, 128] = 256 floats
    private var state = [Float](repeating: 0.0, count: 256)

    private let targetSampleRate: Double = 16000.0
    private var inputSampleRate: Double = 16000.0
    private var threshold: Float = 0.5

    // Silero streaming:
    // 512 new samples + 64 samples of previous context = 576 input samples
    private let windowSize = 512
    private let contextSize = 64

    private var context = [Float](repeating: 0.0, count: 64)

    private var pcmBuffer: [Float] = []

    private let lock = NSLock()

    // MARK: - API Methods

    @objc(initializeWithModelPath:sampleRate:threshold:error:)
    public func initialize(
        modelPath: String,
        sampleRate: Int32,
        threshold: Float,
        error: NSErrorPointer
    ) -> Bool {
        lock.lock()
        defer {
            lock.unlock()
        }

        do {
            self.inputSampleRate = Double(sampleRate)
            self.threshold = threshold

            guard FileManager.default.fileExists(atPath: modelPath) else {
                Self.setError(error, message: "Model file not found at path: \(modelPath)")
                return false
            }

            self.ortEnv = try ORTEnv(loggingLevel: .warning)

            let options = try ORTSessionOptions()
            try options.setIntraOpNumThreads(1)
            try options.setGraphOptimizationLevel(.all)

            guard let env = ortEnv else {
                Self.setError(error, message: "Failed to allocate ORT Environment.")
                return false
            }

            let session = try ORTSession(
                env: env,
                modelPath: modelPath,
                sessionOptions: options
            )

            let inputNames = try session.inputNames()
            let outputNames = try session.outputNames()

            debugLog("ONNX input names: \(inputNames)")
            debugLog("ONNX output names: \(outputNames)")

            guard inputNames.contains(Self.inputAudioName),
                  inputNames.contains(Self.inputStateName),
                  inputNames.contains(Self.inputSampleRateName)
            else {
                Self.setError(
                    error,
                    message:
                    "Model input names don't match expected Silero inputs. Expected: input, state, sr"
                )
                return false
            }

            guard outputNames.contains(Self.outputProbName),
                  outputNames.contains(Self.outputStateName)
            else {
                Self.setError(
                    error,
                    message:
                    "Model output names don't match expected Silero outputs. Expected: output, stateN"
                )
                return false
            }

            self.ortSession = session
            resetStateLocked()
            self.isInitialized = true

            debugLog(
                "Silero VAD initialized. sampleRate=\(sampleRate), threshold=\(threshold), window=\(windowSize), context=\(contextSize), modelInput=576"
            )
            return true

        } catch let catchedError {
            Self.setError(error, message: catchedError.localizedDescription)
            return false
        }
    }

    @objc(processFrameWithAudioFrame:error:)
    public func processFrame(
        audioFrame: [Float],
        error: NSErrorPointer
    ) -> Float {
        var localError: NSError? = nil
        var resultProbability: Float = 0.0

        self.inferenceQueue.sync {
            resultProbability = processFrameInternal(audioFrame: audioFrame, error: &localError)
        }

        if let localError = localError {
            error?.pointee = localError
            return 0.0
        }

        return resultProbability
    }

    @objc(resetState)
    public func resetState() {
        lock.lock()
        defer {
            lock.unlock()
        }
        resetStateLocked()
    }

    @objc(close)
    public func close() {
        lock.lock()
        defer {
            lock.unlock()
        }

        resetStateLocked()

        ortSession = nil
        ortEnv = nil
        isInitialized = false

        debugLog("Closed VAD session")
    }

    // MARK: - Internal Inference Processing

    private func processFrameInternal(
        audioFrame: [Float],
        error: NSErrorPointer
    ) -> Float {
        lock.lock()
        defer {
            lock.unlock()
        }

        guard isInitialized, let session = ortSession else {
            Self.setError(error, message: "VAD is not initialized. Call initialize(...) first.")
            return 0.0
        }

        guard !audioFrame.isEmpty else {
            debugLog("Frame size is zero or empty.")
            return 0.0
        }

        let incomingSamples = audioFrame
        logAudioLevel(samples: incomingSamples)

        // Resample to 16 kHz if necessary.
        let resampledSamples: [Float]

        if inputSampleRate != targetSampleRate {
            debugLog("Audio sample resampling as \(inputSampleRate) != \(targetSampleRate)")
            resampledSamples = resampleLinear(
                samples: incomingSamples,
                sourceRate: inputSampleRate,
                targetRate: targetSampleRate
            )
        } else {
            resampledSamples = incomingSamples
        }

        pcmBuffer.append(contentsOf: resampledSamples)

        var lastProbability: Float = 0.0

        while pcmBuffer.count >= windowSize {
            let chunk = Array(pcmBuffer.prefix(windowSize))
            pcmBuffer.removeFirst(windowSize)

            do {
                lastProbability = try runInferenceOnChunk(chunk: chunk, session: session)
            } catch let catchedError {
                Self.setError(error, message: catchedError.localizedDescription)
                return 0.0
            }
        }

        return lastProbability
    }

    private func runInferenceOnChunk(chunk: [Float], session: ORTSession) throws -> Float {
        guard chunk.count == windowSize else {
            throw NSError(
                domain: "SwiftVoiceActivityDetector",
                code: 1,
                userInfo: [
                    NSLocalizedDescriptionKey:
                    "Invalid chunk size: \(chunk.count), expected \(windowSize)"
                ]
            )
        }

        var inputWithContext = [Float](repeating: 0.0, count: contextSize + windowSize)

        // Previous context
        inputWithContext.withUnsafeMutableBufferPointer { buffer in
            context.withUnsafeBufferPointer { contextBuffer in
                buffer.baseAddress!.update(from: contextBuffer.baseAddress!, count: contextSize)
            }

            // Current 512-sample window
            chunk.withUnsafeBufferPointer { chunkBuffer in
                buffer.baseAddress!
                    .advanced(by: contextSize)
                    .update(from: chunkBuffer.baseAddress!, count: windowSize)
            }
        }

        let audioTensor = try makeFloatTensor(
            values: inputWithContext,
            shape: [NSNumber(value: 1), NSNumber(value: contextSize + windowSize)]
        )

        let stateTensor = try makeFloatTensor(
            values: state,
            shape: [NSNumber(value: 2), NSNumber(value: 1), NSNumber(value: 128)]
        )

        var srValue = Int64(targetSampleRate)
        let srData = Data(bytes: &srValue, count: MemoryLayout<Int64>.size)

        let srTensor = try ORTValue(
            tensorData: NSMutableData(data: srData),
            elementType: .int64,
            shape: [NSNumber(value: 1)]
        )

        let inputs: [String: ORTValue] = [
            Self.inputAudioName: audioTensor,
            Self.inputStateName: stateTensor,
            Self.inputSampleRateName: srTensor,
        ]

        debugInferenceInput(inputWithContext)

        let outputs = try session.run(
            withInputs: inputs,
            outputNames: [Self.outputProbName, Self.outputStateName],
            runOptions: nil
        )

        guard let probabilityValue = outputs[Self.outputProbName] else {
            throw NSError(
                domain: "SwiftVoiceActivityDetector",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Missing output '\(Self.outputProbName)'"]
            )
        }

        let probabilityData = try probabilityValue.tensorData() as Data

        guard probabilityData.count >= MemoryLayout<Float>.size else {
            throw NSError(
                domain: "SwiftVoiceActivityDetector",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Probability tensor data is empty."]
            )
        }

        let probability = probabilityData.withUnsafeBytes { rawBuffer -> Float in
            rawBuffer.load(fromByteOffset: 0, as: Float.self)
        }

        guard let nextStateValue = outputs[Self.outputStateName] else {
            debugLog("Missing output '\(Self.outputStateName)'. State will not be updated.")
            updateContext(inputWithContext)
            return probability
        }

        let nextStateData = try nextStateValue.tensorData() as Data
        let expectedStateBytes = state.count * MemoryLayout<Float>.size

        guard nextStateData.count >= expectedStateBytes else {
            debugLog(
                "State tensor size mismatch. got=\(nextStateData.count) expected=\(expectedStateBytes)"
            )
            updateContext(inputWithContext)
            return probability
        }

        nextStateData.withUnsafeBytes { rawBuffer in
            guard let baseAddress = rawBuffer.baseAddress else {
                return
            }
            let statePointer = baseAddress.assumingMemoryBound(to: Float.self)
            self.state = Array(UnsafeBufferPointer(start: statePointer, count: self.state.count))
        }

        updateContext(inputWithContext)
        return probability
    }

    // MARK: - Internal Helpers

    private func updateContext(_ input: [Float]) {
        guard input.count >= contextSize else {
            return
        }
        let startIndex = input.count - contextSize
        self.context = Array(input[startIndex..<input.count])
    }

    private func makeFloatTensor(values: [Float], shape: [NSNumber]) throws -> ORTValue {
        let data = Data(bytes: values, count: values.count * MemoryLayout<Float>.size)
        return try ORTValue(
            tensorData: NSMutableData(data: data),
            elementType: .float,
            shape: shape
        )
    }

    private func resampleLinear(samples: [Float], sourceRate: Double, targetRate: Double) -> [Float] {
        guard sourceRate > 0, targetRate > 0, !samples.isEmpty else {
            return []
        }
        let factor = targetRate / sourceRate
        let outputCount = Int(Double(samples.count) * factor)

        guard outputCount > 0 else {
            return []
        }

        var output = [Float](repeating: 0.0, count: outputCount)

        for i in 0..<outputCount {
            let srcIndex = Double(i) / factor
            let lowIndex = Int(floor(srcIndex))
            let highIndex = min(lowIndex + 1, samples.count - 1)

            let weight = Float(srcIndex - Double(lowIndex))
            output[i] = samples[lowIndex] * (1.0 - weight) + samples[highIndex] * weight
        }
        return output
    }

    private func resetStateLocked() {
        self.state = [Float](repeating: 0.0, count: 256)
        self.context = [Float](repeating: 0.0, count: contextSize)
        self.pcmBuffer.removeAll()

        debugLog("Silero VAD state + context reset")
    }

    private func logAudioLevel(samples: [Float]) {
        #if DEBUG
        var peak: Float = 0.0
        var rms: Float = 0.0

        vDSP_maxmgv(samples, 1, &peak, vDSP_Length(samples.count))
        vDSP_rmsqv(samples, 1, &rms, vDSP_Length(samples.count))

        debugLog(
            "Raw Input -> count=\(samples.count) peak=\(String(format: "%.5f", peak)) rms=\(String(format: "%.5f", rms))"
        )
        #endif
    }

    private func debugInferenceInput(_ input: [Float]) {
        #if DEBUG
        var peak: Float = 0.0
        var rms: Float = 0.0

        vDSP_maxmgv(input, 1, &peak, vDSP_Length(input.count))
        vDSP_rmsqv(input, 1, &rms, vDSP_Length(input.count))

        debugLog(
            "INFERENCE INPUT -> count=\(input.count) peak=\(String(format: "%.6f", peak)) rms=\(String(format: "%.6f", rms)) context=\(contextSize) window=\(windowSize)"
        )
        #endif
    }

    private func debugLog(_ content: String) {
        #if !DEBUG
        NSLog("[SwiftVadService]: %@", content)
        #endif
    }

    // MARK: - Error Handling Helper

    private static func setError(_ error: NSErrorPointer, message: String) {
        error?.pointee = NSError(
            domain: "SwiftVoiceActivityDetector",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }

    @objc public static func getModelPath() -> String? {
        return Bundle.module.path(forResource: "silero_vad", ofType: "onnx")
    }
}

