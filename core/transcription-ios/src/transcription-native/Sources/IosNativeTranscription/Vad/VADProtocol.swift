import Foundation

@objc(IosVadProtocol)
public protocol VADProtocol: NSObjectProtocol {

    @objc(initializeWithConfig:error:)
    func initialize(config: SherpaVADConfig, error: NSErrorPointer) -> Bool

    @objc(acceptWithSamples:)
    func accept(samples: [Float]) -> Bool

    @objc(popSegment)
    func popSegment() -> SherpaVadSegmentResult?

    @objc(flush)
    func flush()

    @objc(reset)
    func reset()

    @objc(close)
    func close()
}
