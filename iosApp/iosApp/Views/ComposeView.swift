import SwiftUI
import TalkDraftApp

struct ComposeView: UIViewControllerRepresentable {

    let showContentForIos: Bool
    let linkRouter: IosDeepLinkRouter

    func makeUIViewController(context: Context) -> some UIViewController {
        MainViewController.shared.viewController(
            showContentForIos: showContentForIos, router: linkRouter)
    }

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
    }
}
