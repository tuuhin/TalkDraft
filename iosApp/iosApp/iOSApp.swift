import SwiftUI
import TalkDraftApp

class AppDelegate: NSObject, UIApplicationDelegate {

    private let whisperImpl = SwiftWhisperBridge()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // set the whisper provider
        IosWhisperBridge.shared.setProtocol(protocol: whisperImpl)
        // prepares the koin
        KoinInitializer.shared.doInitKoin()
        return true
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}
