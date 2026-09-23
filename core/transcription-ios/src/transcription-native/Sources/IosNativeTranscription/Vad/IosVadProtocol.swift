import Foundation

@objc(IosVadProtocol)
public protocol IosVadProtocol: NSObjectProtocol {

    @objc(initializeWithModelPath:sampleRate:threshold:error:)
    func initialize(modelPath: String, sampleRate: Int32, threshold: Float, error: NSErrorPointer)
        -> Bool

    @objc(processFrameWithAudioFrame:error:)
    func processFrame(audioFrame: [Float], error: NSErrorPointer) -> Float

    func resetState()
    func close()
}
