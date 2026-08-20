import Foundation
import SwiftWhisper
import TalkDraftApp

final class SwiftWhisperBridge: IosWhisperProtocol {

    private var whisper: Whisper?
    private var lastState: IosBridgeWhisperState?
    private var lastError: IosBridgeWhisperCodeError?

    private let lock = NSLock()
    private var isBusy = false

    func doInit(modelPath: String, language: String) -> Bool {
        guard FileManager.default.fileExists(atPath: modelPath) else {
            lastError = .ModelLoadFailed()
            return false
        }
        whisper = Whisper(fromFileURL: URL(fileURLWithPath: modelPath))
        return true
    }

    func processBytes(samples: KotlinShortArray, length: Int32) -> Bool {
        lock.lock()
        if isBusy {
            lock.unlock()
            lastError = .AudioProcessing()
            return false
        }
        isBusy = true
        lock.unlock()
        defer {
            lock.lock()
            isBusy = false
            lock.unlock()
        }

        guard let whisper else {
            lastError = .ModelLoadFailed()
            return false
        }

        let count = Int(length)
        guard count > 0 else {
            lastError = .AudioEmpty()
            return false
        }

        var floatFrames = [Float](repeating: 0, count: count)
        for i in 0..<count {
            floatFrames[i] = Float(samples.get(index: Int32(i))) / 32768.0
        }

        let semaphore = DispatchSemaphore(value: 0)
        var resultSegments: [Segment] = []
        var caughtError: Error?

        Task {
            do {
                resultSegments = try await whisper.transcribe(audioFrames: floatFrames)
            } catch {
                caughtError = error
            }
            semaphore.signal()
        }
        semaphore.wait()

        if caughtError != nil {
            lastError = .TranscriptionFailed()
            lastState = nil
            return false
        }


        let segments = resultSegments.map {
            IosBridgeWhisperSegment(
                text: $0.text,
                startTimeMs: Int64($0.startTime) * 10,
                endTimeMs: Int64($0.endTime) * 10
            )
        }

        lastState = IosBridgeWhisperState(
            fullText: segments.map(\.text).joined(separator: " "),
            segment: segments
        )
        lastError = nil
        return true
    }

    func readState() -> IosBridgeWhisperState? {
        lastState
    }

    func readError() -> IosBridgeWhisperCodeError? {
        lastError
    }

    func close() {
        whisper = nil
        lastState = nil
        lastError = nil
    }
}
