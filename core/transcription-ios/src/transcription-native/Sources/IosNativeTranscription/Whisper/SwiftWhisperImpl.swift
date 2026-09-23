import Foundation
internal import SwiftWhisper

@objc(SwiftWhisperConnector)
@objcMembers
public final class SwiftWhisperImpl: NSObject, IosNativeWhisperConnectorProtocol {

    private var whisper: Whisper?
    private var lastState: IosWhisperState?

    private let lock = NSLock()
    private var isBusy = false

    private static let errorDomain = "SwiftWhisperBridge"

    public override init() {
        super.init()
    }

    @objc(doInitWithModelPath:language:error:)
    public func doInit(modelPath: String, language: String, error: NSErrorPointer) -> Bool {
        lock.lock()
        defer {
            lock.unlock()
        }

        guard FileManager.default.fileExists(atPath: modelPath) else {
            Self.setError(
                error,
                code: 100,
                message: "Model file not found at path: \(modelPath)"
            )
            return false
        }


        let modelURL = URL(fileURLWithPath: modelPath)
        let whisperInstance = Whisper(fromFileURL: modelURL)

        whisperInstance.params.language = .init(rawValue: language) ?? .auto

        self.whisper = whisperInstance
        self.lastState = nil
        return true

    }

    @objc(processBytesWithAudioFrame:length:error:)
    public func processBytes(audioFrame: [Float], length: Int, error: NSErrorPointer) -> Bool {
        lock.lock()
        if isBusy {
            lock.unlock()
            Self.setError(
                error,
                code: 102,
                message: "Engine is currently busy processing another frame."
            )
            return false
        }
        isBusy = true
        lock.unlock()

        defer {
            lock.lock()
            isBusy = false
            lock.unlock()
        }

        guard let whisper = self.whisper else {
            Self.setError(
                error,
                code: 100,
                message: "Whisper engine is not initialized. Call doInit(...) first."
            )
            return false
        }

        guard length > 0, !audioFrame.isEmpty else {
            Self.setError(
                error,
                code: 101,
                message: "Audio frame buffer is empty or length is zero."
            )
            return false
        }

        let sliceCount = min(length, audioFrame.count)
        let samplesToProcess = Array(audioFrame[0..<sliceCount])

        // Unchecked wrapper to satisfy Swift 6 strict concurrency checks across isolation boundaries
        struct SendableWhisperWrapper: @unchecked Sendable {
            let whisper: Whisper
        }

        final class ResultBox: @unchecked Sendable {
            var segments: [Segment] = []
            var error: Error?
        }

        let wrapper = SendableWhisperWrapper(whisper: whisper)
        let box = ResultBox()
        let semaphore = DispatchSemaphore(value: 0)

        Task.detached {
            do {
                box.segments = try await wrapper.whisper.transcribe(audioFrames: samplesToProcess)
            } catch {
                box.error = error
            }
            semaphore.signal()
        }

        semaphore.wait()

        if let caughtError = box.error {
            Self.setError(
                error,
                code: 103,
                message: "Transcription failed: \(caughtError.localizedDescription)"
            )
            self.lastState = nil
            return false
        }

        let segments = box.segments.map { segment in
            IosWhisperSegment(
                text: segment.text,
                startTimeMs: Int64(segment.startTime),
                endTimeMs: Int64(segment.endTime)
            )
        }

        let fullText = segments.map(\.text).joined(separator: " ")
        self.lastState = IosWhisperState(
            fullText: fullText,
            segment: segments
        )

        return true
    }

    @objc(readState)
    public func readState() -> IosWhisperState? {
        lock.lock()
        defer {
            lock.unlock()
        }
        return lastState
    }

    @objc(close)
    public func close() {
        lock.lock()
        defer {
            lock.unlock()
        }

        whisper = nil
        lastState = nil
        isBusy = false
    }

    private static func setError(
        _ error: NSErrorPointer,
        code: Int,
        message: String
    ) {
        error?.pointee = NSError(
            domain: errorDomain,
            code: code,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}
