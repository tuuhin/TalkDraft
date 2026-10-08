import Foundation

@objc
protocol IosPunctuationProtocol: NSObjectProtocol {

    @objc(doInitWithModelPath:isOnline:vocabPath:error:)
    func doInit(modelPath: String, isOnline: Bool, vocabPath: String, error: NSErrorPointer) -> Bool

    @objc(processText:error:)
    func processText(textFrame: String, error: NSErrorPointer) -> String?

    @objc(cleanUp)
    func cleanUp()
}
