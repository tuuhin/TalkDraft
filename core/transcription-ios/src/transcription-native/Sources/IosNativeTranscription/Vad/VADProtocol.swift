import Foundation

@objc(IosVadProtocol)
public protocol VADProtocol: NSObjectProtocol {

    func initialize(config: SherpaVADConfig, error: NSErrorPointer) -> Bool

    func accept(samples: [Float]) -> Bool
    // func popSegment(out: inout [Float], startSample: inout Int32) -> Bool
    func flush()
    func reset()
    func close()
}
