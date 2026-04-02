import Foundation
import Shared

/// Central dependency container for the iOS app.
///
/// Resolves KMP Koin-managed dependencies and wraps them in Swift-friendly types.
/// Must be accessed only after `KoinHelper.shared.start()` in `WishlistApp.init`.
@MainActor
final class AppContainer {

    static let shared = AppContainer()
    private init() {}

    lazy var wishlistRepository: WishlistRepositoryWrapper = {
        let kmpRepo: WishlistRepository = KoinComponentKt.getWishlistRepository()
        return WishlistRepositoryWrapper(repository: kmpRepo)
    }()
}
