import Foundation

@objc(ZipFormerResultSegment)
@objcMembers
public final class ZipFormerResultSegment: NSObject, Identifiable {

    public let segmentId: Int64
    public let segment: String?

    public init(segmentId: Int64, segment: String?) {
        self.segmentId = segmentId
        self.segment = segment
        super.init()
    }
}
