import Foundation

@objc(IosNativeWhisperConnectorProtocol)
public protocol IosNativeWhisperConnectorProtocol: NSObjectProtocol {
    @objc(doInitWithModelPath:language:error:)
    func doInit(modelPath: String, language: String, error: NSErrorPointer) -> Bool

    @objc(processBytesWithAudioFrame:length:error:)
    func processBytes(audioFrame: [Float], length: Int, error: NSErrorPointer) -> Bool

    func readState() -> IosWhisperState?
    func close()
}
