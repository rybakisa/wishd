import SwiftUI
import Shared

@main
struct WishlistApp: App {

    init() {
        KoinHelper.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
