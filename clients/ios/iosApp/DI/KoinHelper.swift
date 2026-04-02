import Foundation
import Shared

/// Initialises Koin for iOS.
///
/// Call `KoinHelper.shared.start(baseUrl:)` once at app launch.
/// All KMP dependencies are then resolvable through the Koin container.
final class KoinHelper {

    static let shared = KoinHelper()
    private init() {}

    func start(baseUrl: String = "https://your-supabase-url.supabase.co/functions/v1") {
        SharedModuleKt.doInitKoin(baseUrl: baseUrl)
    }
}
