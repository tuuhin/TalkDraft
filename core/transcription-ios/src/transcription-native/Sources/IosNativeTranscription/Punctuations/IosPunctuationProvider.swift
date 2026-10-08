import Foundation
internal import sherpa_onnx

@objc
@objcMembers
public final class IosPunctuationProvider: NSObject, IosPunctuationProtocol {

    private var onlinePunctuation: OpaquePointer?
    private var offlinePunctuation: OpaquePointer?

    private var isOnlineMode = false
    private var isReady = false

    private let lock = NSLock()

    public override init() {
        super.init()
    }

    deinit {
        cleanUp()
    }

    public func doInit(modelPath: String, isOnline: Bool, vocabPath: String, error: NSErrorPointer) -> Bool {
        lock.lock()
        defer {
            lock.unlock()
        }

        cleanUpLocked()

        guard !modelPath.isEmpty else {
            Self.setError(error, message: "Model file path is empty.")
            return false
        }

        let fileManager = FileManager.default

        guard fileManager.fileExists(atPath: modelPath) else {
            Self.setError(error, message: "Punctuation model does not exist: \(modelPath)")
            return false
        }

        #if DEBUG
        let debug: Int32 = 1
        #else
        let debug: Int32 = 0
        #endif

        if isOnline {
            guard !vocabPath.isEmpty else {
                Self.setError(
                    error, message: "BPE vocabulary path is required for online punctuation.")
                return false
            }

            guard fileManager.fileExists(atPath: vocabPath) else {
                Self.setError(error, message: "BPE vocabulary file does not exist: \(vocabPath)")
                return false
            }

            guard let modelPathPointer = strdup(modelPath) else {
                Self.setError(error, message: "Failed to allocate model path.")
                return false
            }

            defer {
                free(modelPathPointer)
            }

            guard let vocabPathPointer = strdup(vocabPath) else {
                Self.setError(error, message: "Failed to allocate vocabulary path.")
                return false
            }

            defer {
                free(vocabPathPointer)
            }

            guard let cpuPointer = strdup("cpu") else {
                Self.setError(error, message: "Failed to allocate provider name.")
                return false
            }

            defer {
                free(cpuPointer)
            }

            var modelConfig = SherpaOnnxOnlinePunctuationModelConfig(
                cnn_bilstm: modelPathPointer,
                bpe_vocab: vocabPathPointer,
                num_threads: 2,
                debug: debug,
                provider: cpuPointer
            )

            var config = SherpaOnnxOnlinePunctuationConfig(model: modelConfig)

            let punctuation = withUnsafePointer(to: &config) {
                SherpaOnnxCreateOnlinePunctuation($0)
            }

            guard let punctuation else {
                Self.setError(error, message: "Failed to create online punctuation instance.")
                return false
            }

            onlinePunctuation = punctuation
            offlinePunctuation = nil
            isOnlineMode = true
            isReady = true
            return true
        }

        guard let modelPathPointer = strdup(modelPath) else {
            Self.setError(error, message: "Failed to allocate model path.")
            return false
        }

        defer {
            free(modelPathPointer)
        }

        guard let cpuPointer = strdup("cpu") else {
            Self.setError(error, message: "Failed to allocate provider name.")
            return false
        }

        defer {
            free(cpuPointer)
        }

        var modelConfig = SherpaOnnxOfflinePunctuationModelConfig(
            ct_transformer: modelPathPointer,
            num_threads: 2,
            debug: debug,
            provider: cpuPointer
        )

        var config = SherpaOnnxOfflinePunctuationConfig(model: modelConfig)

        let punctuation = withUnsafePointer(to: &config) {
            SherpaOnnxCreateOfflinePunctuation($0)
        }

        guard let punctuation else {
            Self.setError(error, message: "Failed to create offline punctuation instance.")
            return false
        }

        offlinePunctuation = punctuation
        onlinePunctuation = nil
        isOnlineMode = false
        isReady = true

        return true
    }

    public func processText(textFrame: String, error: NSErrorPointer) -> String? {

        lock.lock()
        defer {
            lock.unlock()
        }

        guard isReady else {
            Self.setError(error, message: "Punctuation provider is not initialized.")
            return nil
        }

        guard !textFrame.isEmpty else {
            return ""
        }

        if isOnlineMode {
            guard let punctuation = onlinePunctuation else {
                Self.setError(error, message: "Online punctuation instance is not available.")
                return nil
            }

            guard let textPointer = strdup(textFrame) else {
                Self.setError(error, message: "Failed to allocate input text.")
                return nil
            }

            defer {
                free(textPointer)
            }

            guard let result = SherpaOnnxOnlinePunctuationAddPunct(punctuation, textPointer)
            else {
                Self.setError(error, message: "Failed to process text with online punctuation.")
                return nil
            }
            return String(cString: result)
        }

        guard let punctuation = offlinePunctuation else {
            Self.setError(error, message: "Offline punctuation instance is not available.")
            return nil
        }

        guard let textPointer = strdup(textFrame) else {
            Self.setError(error, message: "Failed to allocate input text.")
            return nil
        }

        defer {
            free(textPointer)
        }

        guard let result = SherpaOfflinePunctuationAddPunct(punctuation, textPointer) else {
            Self.setError(error, message: "Failed to process text with offline punctuation.")
            return nil
        }

        return String(cString: result)
    }

    public func cleanUp() {
        lock.lock()
        defer {
            lock.unlock()
        }
        cleanUpLocked()
    }

    private func cleanUpLocked() {
        if let punctuation = onlinePunctuation {
            SherpaOnnxDestroyOnlinePunctuation(punctuation)
            onlinePunctuation = nil
        }

        if let punctuation = offlinePunctuation {
            SherpaOnnxDestroyOfflinePunctuation(punctuation)
            offlinePunctuation = nil
        }

        isReady = false
        isOnlineMode = false
    }

    private static func setError(
        _ error: NSErrorPointer,
        message: String
    ) {
        error?.pointee = NSError(
            domain: "IosPunctuationProvider",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}
