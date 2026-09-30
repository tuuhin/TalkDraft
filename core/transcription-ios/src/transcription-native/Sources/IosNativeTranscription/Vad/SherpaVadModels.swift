import Foundation

@objc(SherpaVADConfig)
@objcMembers
public final class SherpaVADConfig: NSObject {

    public var sampleRate: Int32 = 16000
    public var threshold: Float = 0.4
    public var minSilenceSeconds: Float = 0.3
    public var minSpeechSeconds: Float = 0.1
    public var maxSpeechSeconds: Float = 6.0
    public var numThreads: Int32 = 1
    public var bufferSeconds: Float = 30.0

    public init(
        sampleRate: Int32 = 16000,
        threshold: Float = 0.4,
        minSilenceSeconds: Float = 0.3,
        minSpeechSeconds: Float = 0.1,
        maxSpeechSeconds: Float = 6.0,
        numThreads: Int32 = 1,
        bufferSeconds: Float = 30.0
    ) {
        self.sampleRate = sampleRate
        self.threshold = threshold
        self.minSilenceSeconds = minSilenceSeconds
        self.minSpeechSeconds = minSpeechSeconds
        self.maxSpeechSeconds = maxSpeechSeconds
        self.numThreads = numThreads
        self.bufferSeconds = bufferSeconds
        super.init()
    }
}

@objc(SherpaVadSegmentResult)
@objcMembers
public final class SherpaVadSegmentResult: NSObject {
    public let samples: [Float]
    public let startSample: Int32
    public let endSample: Int32

    public init(samples: [Float], startSample: Int32) {
        self.samples = samples
        self.startSample = startSample
        self.endSample = Int32(samples.count) + startSample
        super.init()
    }
}
