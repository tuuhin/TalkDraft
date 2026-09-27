import Foundation
import TalkDraftApp

@MainActor
class RemoteConfigViewmodel: ObservableObject {
    private let factory = IosAppViewmodelProvider.shared
    @Published var showDialog: Bool = false

    var isRunningInPreview: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
    }

    func checkConfig() async {
        if isRunningInPreview {
            // skip if we are running in preview mode
            return
        }
        do {
            try await factory.loadRemoteConfig()
            self.showDialog = factory.showNewVersionRequiredDialog
        } catch {
            LoggerKt.w(string: "Failed to load remote config: \(error.localizedDescription)")
        }
    }
}
