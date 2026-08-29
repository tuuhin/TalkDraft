import SwiftUI
import TalkDraftApp

struct ContentView: View {

    var body: some View {
        NavigationStack {
            VStack {
                Spacer()
                Text("App Ios version is not planned")
                    .padding()
                    .font(.body)
                Spacer()
            }
            .navigationTitle("Talk Draft")
        }
    }
}

#Preview {
    ContentView()
}
