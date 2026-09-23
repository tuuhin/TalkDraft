import SwiftUI
import TalkDraftApp

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // setup logging
        IosAppInitializer.shared.setupLogging()
        // prepares the koin
        KoinInitializer.shared.doInitKoin()
        // sync db refresh task
        RemoteDbSyncRegistrarBridge.shared.invoke()
        Task {
            // sets up analytics and crashlytics
            try? await IosAppInitializer.shared.setup()
        }
        return true
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    private var appDelegate: AppDelegate

    private let linkRouter: IosDeepLinkRouter = IosDeepLinkRouter()

    var body: some Scene {
        WindowGroup {
            Color.clear.requireAppUpdate { showContent in
                ComposeView(showContentForIos: showContent, linkRouter: linkRouter)
                    .ignoresSafeArea()
            }
            .onOpenURL { url in
                linkRouter.handleLink(url: url.absoluteString)
            }
        }
    }
}
