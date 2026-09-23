import Foundation

@objc(IosWhisperSegment)
@objcMembers
public final class IosWhisperSegment: NSObject {

    public let text: String
    public let startTimeMs: Int64
    public let endTimeMs: Int64

    @objc(initWithText:startTimeMs:endTimeMs:)
    public init(text: String, startTimeMs: Int64, endTimeMs: Int64) {
        self.text = text
        self.startTimeMs = startTimeMs
        self.endTimeMs = endTimeMs
        super.init()
    }
}

@objc(IosWhisperState)
@objcMembers
public final class IosWhisperState: NSObject {

    public let fullText: String
    public let segment: [IosWhisperSegment]

    @objc(initWithFullText:segment:)
    public init(fullText: String = "", segment: [IosWhisperSegment] = []) {
        self.fullText = fullText
        self.segment = segment
        super.init()
    }

    /// Default convenience initializer (empty state)
    public override convenience init() {
        self.init(fullText: "", segment: [])
    }
}
