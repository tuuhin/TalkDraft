import Foundation

/// Defines the interface for performing speech-to-text inference using ONNX Runtime.
@objc
protocol IosZipFormerProtocol: NSObjectProtocol {

    /// Initializes the ONNX model sessions and token mappings.
    ///
    /// - Parameters:
    ///   - encoderPath: File path to the ONNX encoder model.
    ///   - decoderPath: File path to the ONNX decoder model.
    ///   - joinerPath: File path to the ONNX joiner model.
    ///   - tokensPath: File path to the token vocabulary file.
    ///   - error: Pointer to an `NSError` object that captures runtime errors.
    /// - Returns: `true` if initialization succeeded; otherwise, `false`.
    @objc(doInitWithEncoderPath:decoderPath:joinerPath:tokensPath:error:)
    func doInit(
        encoderPath: String,
        decoderPath: String,
        joinerPath: String,
        tokensPath: String,
        error: NSErrorPointer
    ) -> Bool

    /// Transcribes log-mel filter bank features into text.
    ///
    /// - Parameters:
    ///   - melFeatures: Flattened log-mel feature buffer.
    ///   - numFrames: Total number of acoustic frames in the input.
    ///   - error: Pointer to an `NSError` object that captures transcription errors.
    /// - Returns: Decoded text string if successful; otherwise, `nil`.
    @objc(transcribe:numFrames:error:)
    func transcribe(melFeatures: [Float], numFrames: Int, error: NSErrorPointer) -> String?

    // Cleans up the setup logic
    @objc(cleanUp)
    func cleanUp()
}
