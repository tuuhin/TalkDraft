import Foundation

@objc(SherpaVADConfig)
@objcMembers
public final class SherpaVADConfig: NSObject {

    public var sampleRate: Int32 = 16000
    public var threshold: Float = 0.5
    public var minSilenceSeconds: Float = 0.5
    public var minSpeechSeconds: Float = 0.25
    public var maxSpeechSeconds: Float = 20.0
    public var numThreads: Int32 = 1
    public var bufferSeconds: Float = 30.0

    public init(
        sampleRate: Int32 = 16000,
        threshold: Float = 0.5,
        minSilenceSeconds: Float = 0.5,
        minSpeechSeconds: Float = 0.25,
        maxSpeechSeconds: Float = 20.0,
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
