import Foundation
internal import OnnxRuntimeBindings

@objc(IosZipFormer)
@objcMembers
public final class IosZipFormerImpl: NSObject, IosZipFormerProtocol {

    private var env: ORTEnv?
    private var encoderSession: ORTSession?
    private var decoderSession: ORTSession?
    private var joinerSession: ORTSession?

    private var tokensMap: [Int64: String] = [:]
    private var blankTokenId: Int64 = 0

    private let lock = NSLock()

    public override init() {
        super.init()
    }

    @objc(doInitWithEncoderPath:decoderPath:joinerPath:tokensPath:error:)
    public func doInit(
        encoderPath: String,
        decoderPath: String,
        joinerPath: String,
        tokensPath: String,
        error: NSErrorPointer
    ) -> Bool {
        lock.lock()
        defer {
            lock.unlock()
        }

        do {
            let env = try ORTEnv(loggingLevel: .warning)
            self.env = env

            let options = try ORTSessionOptions()
            do {
                try options.appendCoreMLExecutionProvider()
            } catch {
                #if DEBUG
                print(
                    "[IosZipFormer] CoreML provider unavailable, falling back to CPU: \(error.localizedDescription)"
                )
                #endif
            }

            self.encoderSession = try ORTSession(
                env: env, modelPath: encoderPath, sessionOptions: options)
            self.decoderSession = try ORTSession(
                env: env, modelPath: decoderPath, sessionOptions: options)
            self.joinerSession = try ORTSession(
                env: env, modelPath: joinerPath, sessionOptions: options)

            try loadTokens(from: tokensPath)
            return true
        } catch let catchedError {
            Self.setError(error, message: catchedError.localizedDescription)
            return false
        }
    }

    @objc(transcribe:numFrames:error:)
    public func transcribe(melFeatures: [Float], numFrames: Int, error: NSErrorPointer) -> String? {
        lock.lock()
        guard let encoderSession = self.encoderSession,
              let decoderSession = self.decoderSession,
              let joinerSession = self.joinerSession
        else {
            lock.unlock()
            Self.setError(error, message: "Sessions not initialized. Call doInit first.")
            return nil
        }
        lock.unlock()

        do {
            // 1. Run Encoder
            let encoderInput = try createTensor(
                data: melFeatures,
                shape: [1, NSNumber(value: numFrames), 80],
                type: .float
            )

            let encoderOutputs = try encoderSession.run(
                withInputs: ["speech": encoderInput],
                outputNames: ["encoder_out"],
                runOptions: nil
            )

            guard let encoderOut = encoderOutputs["encoder_out"] else {
                Self.setError(error, message: "Missing 'encoder_out' from encoder session.")
                return nil
            }

            // 2. Greedy Search Transducer Decoding Loop
            var resultTokens: [Int64] = []
            var decoderContext: [Int64] = [blankTokenId]

            for _ in 0..<numFrames {
                var emittedNonBlank = true
                var maxStepsPerFrame = 10

                while emittedNonBlank && maxStepsPerFrame > 0 {
                    maxStepsPerFrame -= 1

                    let decoderInput = try createTensor(
                        data: decoderContext,
                        shape: [1, NSNumber(value: decoderContext.count)],
                        type: .int64
                    )

                    let decoderOutputs = try decoderSession.run(
                        withInputs: ["y": decoderInput],
                        outputNames: ["decoder_out"],
                        runOptions: nil
                    )

                    guard let decoderOut = decoderOutputs["decoder_out"] else {
                        break
                    }

                    let joinerOutputs = try joinerSession.run(
                        withInputs: [
                            "encoder_out": encoderOut,
                            "decoder_out": decoderOut,
                        ],
                        outputNames: ["logit"],
                        runOptions: nil
                    )

                    guard let logits = joinerOutputs["logit"] else {
                        break
                    }
                    let predictedToken = try extractArgMaxToken(logitsTensor: logits)

                    if predictedToken != blankTokenId {
                        resultTokens.append(predictedToken)
                        decoderContext.append(predictedToken)
                        if decoderContext.count > 2 {
                            decoderContext.removeFirst()
                        }
                    } else {
                        emittedNonBlank = false
                    }
                }
            }

            return decodeTokensToText(resultTokens)

        } catch let catchedError {
            Self.setError(error, message: catchedError.localizedDescription)
            return nil
        }
    }

    @objc(cleanUp)
    public func cleanUp() {
        lock.lock()
        defer {
            lock.unlock()
        }
        self.encoderSession = nil
        self.decoderSession = nil
        self.joinerSession = nil
        self.env = nil
        tokensMap.removeAll()
    }

    private func loadTokens(from path: String) throws {
        let content = try String(contentsOfFile: path, encoding: .utf8)
        let lines = content.components(separatedBy: .newlines)
        for line in lines {
            let parts = line.trimmingCharacters(in: .whitespaces).components(separatedBy: .whitespaces)
            if parts.count >= 2, let id = Int64(parts[1]) {
                let token = parts[0]
                if token == "<blk>" || token == "<blank>" {
                    blankTokenId = id
                }
                tokensMap[id] = token
            }
        }
    }


    private func extractArgMaxToken(logitsTensor: ORTValue) throws -> Int64 {
        let tensorData = try logitsTensor.tensorData() as Data
        return tensorData.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) -> Int64 in
            let floatBuffer = buffer.bindMemory(to: Float.self)
            guard let maxElement = floatBuffer.enumerated().max(by: { $0.element < $1.element })
            else {
                return 0
            }
            return Int64(maxElement.offset)
        }
    }

    private func decodeTokensToText(_ tokens: [Int64]) -> String {
        tokens
        .compactMap {
            self.tokensMap[$0]
        }
        .joined()
        .replacingOccurrences(of: " ", with: " ")
        .trimmingCharacters(in: .whitespaces)
    }

    private func createTensor<T: BitwiseCopyable>(
        data: [T],
        shape: [NSNumber],
        type: ORTTensorElementDataType
    ) throws -> ORTValue {
        let tensorData = data.withUnsafeBytes { bufferPointer in
            Data(bufferPointer)
        }

        let nsData = NSMutableData(data: tensorData)

        guard let tensor = try? ORTValue(tensorData: nsData, elementType: type, shape: shape) else {
            throw NSError(
                domain: "IosZipFormerError",
                code: 2,
                userInfo: [NSLocalizedDescriptionKey: "Failed to create ORTValue tensor."]
            )
        }
        return tensor
    }

    private static func setError(_ error: NSErrorPointer, message: String) {
        error?.pointee = NSError(
            domain: "IosZipFormerError",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}
