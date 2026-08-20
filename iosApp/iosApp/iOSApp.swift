import SwiftUI
import TalkDraftApp

@main
struct iOSApp: App {
    let whisperImpl = SwiftWhisperBridge()

    init() {
        // set the whisper provider
        IosWhisperBridge.shared.setProtocol(protocol: whisperImpl)
        // prepares the koin
        KoinInitializer.shared.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
