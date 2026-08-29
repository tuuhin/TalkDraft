import Foundation
import TalkDraftApp

@MainActor
class RemoteConfigViewmodel: ObservableObject {
    private let factory = IosAppViewmodelProvider.shared
    @Published var showDialog: Bool = false

    func checkConfig() async {
        do {
            try await factory.loadRemoteConfig()
            self.showDialog = factory.showNewVersionRequiredDialog
        } catch {
            print("Failed to load remote config: \(error.localizedDescription)")
        }
    }
}
