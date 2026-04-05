import Foundation
import Shared

/// Central access to KMP-backed repositories, wrapped in Swift-friendly async APIs.
/// Must be accessed only after `KoinHelper.shared.start()` in `WishlistApp.init`.
@MainActor
final class AppContainer {
    static let shared = AppContainer()
    private init() {}

    lazy var wishlistRepository: WishlistRepositoryWrapper = {
        WishlistRepositoryWrapper(repository: KoinComponentHelperKt.getWishlistRepository())
    }()

    lazy var authRepository: AuthRepositoryWrapper = {
        AuthRepositoryWrapper(repo: KoinComponentHelperKt.getAuthRepository())
    }()
}
