import SwiftUI
import TalkDraftApp

struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> some UIViewController {
        MainViewController.shared.viewController()
    }

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
    }
}
