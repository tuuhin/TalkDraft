import Foundation
internal import sherpa_onnx

@objc(IosZipFormer)
@objcMembers
public final class IosZipFormerImpl: NSObject, IosZipFormerProtocol {

    private var recognizer: OpaquePointer?
    private var stream: OpaquePointer?

    private var isReady: Bool = false
    private var currentSegment: String = ""
    private var segmentId: Int64 = 1

    private let lock = NSLock()

    public override init() {
        super.init()
    }

    deinit {
        cleanUp()
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

        cleanUpLocked()

        let fileManager = FileManager.default
        guard fileManager.fileExists(atPath: encoderPath),
              fileManager.fileExists(atPath: decoderPath),
              fileManager.fileExists(atPath: joinerPath),
              fileManager.fileExists(atPath: tokensPath)
        else {
            Self.setError(error, message: "One or more model files do not exist.")
            return false
        }

        var config = SherpaOnnxOnlineRecognizerConfig()
        memset(&config, 0, MemoryLayout<SherpaOnnxOnlineRecognizerConfig>.size)

        config.feat_config.sample_rate = 16000
        config.feat_config.feature_dim = 80
        config.model_config.num_threads = 2

        if let encoderMutblePtr = encoderPath.cStringCopy,
           let decoderMutablePtr = decoderPath.cStringCopy,
           let joinerMutablePtr = joinerPath.cStringCopy {
            config.model_config.transducer.encoder = UnsafePointer(encoderMutblePtr)
            config.model_config.transducer.decoder = UnsafePointer(decoderMutablePtr)
            config.model_config.transducer.joiner = UnsafePointer(joinerMutablePtr)
        }

        if let modelTypePtr = "zipformer2".cStringCopy, let modelProviderPtr = "cpu".cStringCopy,
           let decodringTypePtr = "modified_bean_search".cStringCopy {
            config.model_config.model_type = UnsafePointer(modelTypePtr)
            config.model_config.provider = UnsafePointer(modelProviderPtr)
            config.decoding_method = UnsafePointer(decodringTypePtr)
        }

        #if DEBUG
        config.model_config.debug = 1
        #else
        config.model_config.debug = 0
        #endif

        config.max_active_paths = 4
        config.enable_endpoint = 0
        config.blank_penalty = 0.0

        self.recognizer = SherpaOnnxCreateOnlineRecognizer(&config)

        guard let recognizer = self.recognizer else {
            Self.setError(error, message: "Failed to create SherpaOnnx Online Recognizer.")
            return false
        }

        self.stream = SherpaOnnxCreateOnlineStream(recognizer)
        if self.stream == nil {
            SherpaOnnxDestroyOnlineRecognizer(recognizer)
            self.recognizer = nil
            Self.setError(error, message: "Failed to create SherpaOnnx Online Stream.")
            return false
        }

        self.segmentId = 1
        self.isReady = true
        return true
    }

    @objc(transcribe:numFrames:error:)
    public func transcribe(melFeatures: [Float], numFrames: Int, error: NSErrorPointer)
        -> ZipFormerResultSegment? {
        lock.lock()
        defer {
            lock.unlock()
        }

        guard isReady, let recognizer = self.recognizer, let stream = self.stream else {
            Self.setError(
                error, message: "Recognizer or stream not initialized. Call doInit first.")
            return nil
        }

        guard !melFeatures.isEmpty && numFrames > 0 else {
            return nil
        }

        SherpaOnnxOnlineStreamAcceptWaveform(stream, 16000, melFeatures, Int32(numFrames))

        // Decode available frames
        while SherpaOnnxIsOnlineStreamReady(recognizer, stream) != 0 {
            SherpaOnnxDecodeOnlineStream(recognizer, stream)
        }

        var newText = ""
        if let resultPtr = SherpaOnnxGetOnlineStreamResult(recognizer, stream) {
            if let textPtr = resultPtr.pointee.text, strlen(textPtr) > 0 {
                newText = String(cString: textPtr)
            }
            SherpaOnnxDestroyOnlineRecognizerResult(resultPtr)
        }

        if !newText.isEmpty && newText != currentSegment {
            currentSegment = newText
            return ZipFormerResultSegment(segmentId: self.segmentId, segment: currentSegment)
        }

        return nil
    }

    /// Resets the current stream state for segment boundary processing
    public func reset() {
        lock.lock()
        defer {
            lock.unlock()
        }

        guard let recognizer = self.recognizer, let stream = self.stream else {
            return
        }

        SherpaOnnxOnlineStreamInputFinished(stream)

        while SherpaOnnxIsOnlineStreamReady(recognizer, stream) != 0 {
            SherpaOnnxDecodeOnlineStream(recognizer, stream)
        }

        if let finalResult = SherpaOnnxGetOnlineStreamResult(recognizer, stream) {
            SherpaOnnxDestroyOnlineRecognizerResult(finalResult)
        }

        currentSegment = ""
        segmentId += 1
        SherpaOnnxOnlineStreamReset(recognizer, stream)
    }

    @objc(cleanUp)
    public func cleanUp() {
        lock.lock()
        defer {
            lock.unlock()
        }
        cleanUpLocked()
    }

    private func cleanUpLocked() {
        if let stream = self.stream {
            SherpaOnnxDestroyOnlineStream(stream)
            self.stream = nil
        }
        if let recognizer = self.recognizer {
            SherpaOnnxDestroyOnlineRecognizer(recognizer)
            self.recognizer = nil
        }
        isReady = false
        currentSegment = ""
    }

    private static func setError(_ error: NSErrorPointer, message: String) {
        error?.pointee = NSError(
            domain: "IosZipFormerError",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}

extension String {
    /// Creates a heap-allocated `char *` using `strdup`.
    /// - Important: The caller is responsible for freeing the returned pointer using `free()`.
    var cStringCopy: UnsafeMutablePointer<CChar>? {
        strdup(self)
    }

    /// Safely executes a block passing a non-null `UnsafePointer<CChar>` (const char *).
    func withCStringPointer<Result>(_ body: (UnsafePointer<CChar>) throws -> Result) rethrows
        -> Result {
        try self.withCString(body)
    }
}
