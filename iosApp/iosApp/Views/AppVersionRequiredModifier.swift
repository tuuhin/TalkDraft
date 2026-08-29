import Foundation
import SwiftUI

struct AppVersionRequiredModifier<AppContent: View>: ViewModifier {
    @StateObject private var viewModel = RemoteConfigViewmodel()

    let contentBuilder: (Bool) -> AppContent

    init(@ViewBuilder contentBuilder: @escaping (Bool) -> AppContent) {
        self.contentBuilder = contentBuilder
    }

    func body(content: Content) -> some View {
        contentBuilder(!viewModel.showDialog)
        .task {
            await viewModel.checkConfig()
        }
        .confirmationDialog(
            "App update required",
            isPresented: $viewModel.showDialog,
            titleVisibility: .visible
        ) {
            Button("Update Now") {
                if let url = URL(string: "https://tuhinbhowmick.in") {
                    UIApplication.shared.open(url)
                }
                viewModel.showDialog = true
            }
        } message: {
            Text(
                "A new version of the app is required. Please update to continue using the application."
            )
        }
    }
}

extension View {
    func requireAppUpdate<AppContent: View>(@ViewBuilder content: @escaping (Bool) -> AppContent)
        -> some View {
        self.modifier(AppVersionRequiredModifier(contentBuilder: content))
    }
}
