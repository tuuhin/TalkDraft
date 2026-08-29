import SwiftUI
import TalkDraftApp

struct ComposeView: UIViewControllerRepresentable {
    let showContentForIos: Bool

    func makeUIViewController(context: Context) -> some UIViewController {
        MainViewController.shared.viewController(showContentForIos: showContentForIos)
    }

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
    }
}
