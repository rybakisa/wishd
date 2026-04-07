import SwiftUI
import Shared

@main
struct WishlistApp: App {

    // Supabase config — replace with your project values
    private static let supabaseUrl = "https://your-project.supabase.co"
    private static let supabaseAnonKey = "your-anon-key"

    init() {
        KoinHelper.shared.start(
            baseUrl: "http://localhost:4000",
            supabaseUrl: Self.supabaseUrl,
            supabaseAnonKey: Self.supabaseAnonKey,
            callbackScheme: "com.wishlist.ios",
            callbackHost: "auth"
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle OAuth redirect from Supabase (com.wishlist.ios://auth#access_token=...)
                    handleOAuthCallback(url: url)
                }
        }
    }

    private func handleOAuthCallback(url: URL) {
        guard url.scheme == "com.wishlist.ios" else { return }
        guard let mgr = KoinComponentHelperKt.getSupabaseAuthManager() else { return }
        Task {
            try? await mgr.client.auth.handle(url)
        }
    }
}
