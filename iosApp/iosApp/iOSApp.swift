import SwiftUI
import TalkDraftApp

class AppDelegate: NSObject, UIApplicationDelegate {

    private let whisper = SwiftWhisperBridge()
    private let voiceActivity = SwiftVoiceActivityDetector()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // prepares the koin
        KoinInitializer.shared.doInitKoin()
        // sync db refresh task
        RemoteDbSyncRegistrarBridge.shared.invoke()
        // set the whisper provider
        IosWhisperBridge.shared.setProtocol(protocol: whisper)
        // set up vad provider
        IosVoiceActivityDetectorBridge.shared.setProtocol(protocol: voiceActivity)
        return true
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate


    var body: some Scene {
        WindowGroup {
            Color.clear.requireAppUpdate { showContent in
                ComposeView(showContentForIos: showContent)
                    .ignoresSafeArea()
            }
            .task {
                try? await IosAppInitializer.shared.setup()
            }
    }
    }
}
