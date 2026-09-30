import Accelerate
import Foundation
internal import sherpa_onnx

@objc(SherapVADImpl)
@objcMembers
public final class SherpaVadImpl: NSObject, VADProtocol {

    private var vad: OpaquePointer?
    private let lock = NSLock()

    public override init() {
        super.init()
    }

    deinit {
        // clear the vad if its exists
        if let vad = vad {
            SherpaOnnxDestroyVoiceActivityDetector(vad)
            self.vad = nil
        }
    }

    @objc(initializeWithConfig:error:)
    public func initialize(config: SherpaVADConfig, error: NSErrorPointer) -> Bool {

        lock.lock()
        defer {
            lock.unlock()
        }

        do {
            var sherpaConfig = SherpaOnnxVadModelConfig()
            var sherpaSileroConfig = SherpaOnnxSileroVadModelConfig()
            memset(&sherpaConfig, 0, MemoryLayout<SherpaVADConfig>.stride)
            memset(&sherpaSileroConfig, 0, MemoryLayout<SherpaOnnxSileroVadModelConfig>.stride)

            // set the fields for silero
            sherpaSileroConfig.max_speech_duration = config.maxSpeechSeconds
            sherpaSileroConfig.min_speech_duration = config.minSpeechSeconds
            sherpaSileroConfig.min_silence_duration = config.minSilenceSeconds
            sherpaSileroConfig.threshold = config.threshold

            if let path = getModelPath(), let modelPathPtr = strdup(path) {
                sherpaSileroConfig.model = UnsafePointer(modelPathPtr)
            }
            if let providerptr = strdup("cpu") {
                sherpaConfig.provider = UnsafePointer(providerptr)
            }

            sherpaSileroConfig.window_size =
                if config.sampleRate == 8_000 {
                    256
                } else {
                    512
                }

            // set the fields for model config
            sherpaConfig.num_threads = config.numThreads
            // mark to false in release
            sherpaConfig.debug = 0
            sherpaConfig.sample_rate = config.sampleRate
            sherpaConfig.silero_vad = sherpaSileroConfig

            self.vad = SherpaOnnxCreateVoiceActivityDetector(&sherpaConfig, config.bufferSeconds)
            return true

        } catch let catchedError {
            Self.setError(error, message: catchedError.localizedDescription)
            return false
        }
    }

    @objc(acceptWithSamples:)
    public func accept(samples: [Float]) -> Bool {
        lock.lock()
        defer {
            lock.unlock()
        }
        guard let vad = vad else {
            debugLog("accept(): VAD INSTANCE IS NOT SET, SET INSTANCE TO POP OUT THE SEGMENTS")
            return false
        }

        if samples.isEmpty {
            debugLog("SAMPLE BUFFER IS EMPTY")
            return false
        }

        SherpaOnnxVoiceActivityDetectorAcceptWaveform(vad, samples, Int32(samples.count))
        let isDetected = SherpaOnnxVoiceActivityDetectorDetected(vad) != 0
        debugLog("SPEECH IS ACTIVE: \(isDetected)")
        return isDetected
    }

    @objc(popSegment)
    public func popSegment() -> SherpaVadSegmentResult? {
        lock.lock()
        defer {
            lock.unlock()
        }

        guard let vad = vad else {
            debugLog("pop_segment(): VAD INSTANCE IS NOT SET, SET INSTANCE TO POP OUT THE SEGMENTS")
            return nil
        }

        if SherpaOnnxVoiceActivityDetectorEmpty(vad) != 0 {
            debugLog("SPEECH QUEUE IS EMPTY")
            return nil
        }

        guard let seg = SherpaOnnxVoiceActivityDetectorFront(vad) else {
            debugLog("NO SEGMENTS AT THE FRONT")
            return nil
        }

        let sampleCount = Int(seg.pointee.n)
        let samples: [Float]
        if let samplesPointer = seg.pointee.samples {
            let buffer = UnsafeBufferPointer(start: samplesPointer, count: sampleCount)
            samples = Array(buffer)
        } else {
            samples = []
        }

        let startSample = seg.pointee.start

        debugLog("POPPED SEGMENT: START=\(startSample), SIZE=\(sampleCount) samples")
        SherpaOnnxDestroySpeechSegment(seg)
        SherpaOnnxVoiceActivityDetectorPop(vad)
        return SherpaVadSegmentResult(samples: samples, startSample: startSample)
    }

    @objc(flush)
    public func flush() {
        lock.lock()
        defer {
            lock.unlock()
        }
        guard let vad = self.vad else {
            debugLog("flush(): VAD INSTANCE IS NOT SET, SET INSTANCE TO RESET")
            return
        }
        SherpaOnnxVoiceActivityDetectorFlush(vad)
        debugLog("FLUSHED SAMPLES TO BUFFER")
    }

    @objc(reset)
    public func reset() {
        lock.lock()
        defer {
            lock.unlock()
        }
        guard let vad = self.vad else {
            debugLog("reset(): VAD INSTANCE IS NOT SET, SET INSTANCE TO RESET")
            return
        }
        SherpaOnnxVoiceActivityDetectorReset(vad)
        debugLog("VAD DETECTOR REST")
    }

    @objc(close)
    public func close() {
        lock.lock()
        defer {
            lock.unlock()
        }
        guard let vad = self.vad else {
            debugLog("reset(): VAD INSTANCE IS NOT SET, SET INSTANCE TO RESET")
            return
        }
        SherpaOnnxDestroyVoiceActivityDetector(vad)
        self.vad = nil
    }

    private func debugLog(_ content: String) {
        #if !DEBUG
        NSLog("[SwiftVadService]: %@", content)
        #endif
    }

    private static func setError(_ error: NSErrorPointer, message: String) {
        error?.pointee = NSError(
            domain: "SherpaVAD",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }

    private func getModelPath() -> String? {
        Bundle.module.path(forResource: "silero_vad", ofType: "int8.onnx")
    }
}
