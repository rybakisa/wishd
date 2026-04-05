import Foundation
import Shared

/// Initialises Koin for iOS. Call `KoinHelper.shared.start(baseUrl:)` once at app launch.
final class KoinHelper {
    static let shared = KoinHelper()
    private init() {}

    func start(baseUrl: String = "http://localhost:4000") {
        SharedModuleKt.doInitKoin(baseUrl: baseUrl)
    }
}
