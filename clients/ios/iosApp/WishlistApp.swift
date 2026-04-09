import SwiftUI
import Shared

@main
struct WishlistApp: App {

    init() {
        KoinHelper.shared.start(
            baseUrl: Secrets.apiBaseUrl,
            supabaseUrl: Secrets.supabaseUrl,
            supabaseAnonKey: Secrets.supabaseAnonKey,
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
