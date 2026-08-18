import SwiftUI
import TalkDraftApp

@main
struct iOSApp: App {

    init() {
        KoinInitializer.shared.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}