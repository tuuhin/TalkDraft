import SwiftUI
import TalkDraftApp

class AppDelegate: NSObject, UIApplicationDelegate {

    private let whisper = SwiftWhisperBridge()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // set the whisper provider
        IosWhisperBridge.shared.setProtocol(protocol: whisper)
        // prepares the koin
        KoinInitializer.shared.doInitKoin()
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
