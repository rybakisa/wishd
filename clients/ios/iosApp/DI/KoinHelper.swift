import Foundation
import Shared

/// Initialises Koin for iOS. Call `KoinHelper.shared.start(...)` once at app launch.
final class KoinHelper {
    static let shared = KoinHelper()
    private init() {}

    func start(
        baseUrl: String = "http://localhost:4000",
        supabaseUrl: String = "",
        supabasePublishableKey: String = "",
        callbackScheme: String = "com.wishlist.ios",
        callbackHost: String = "auth"
    ) {
        SharedModuleKt.doInitKoin(
            baseUrl: baseUrl,
            supabaseUrl: supabaseUrl,
            supabasePublishableKey: supabasePublishableKey,
            callbackScheme: callbackScheme,
            callbackHost: callbackHost
        )
    }
}
