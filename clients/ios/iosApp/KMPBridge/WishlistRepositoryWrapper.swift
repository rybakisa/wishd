import Foundation
import Shared

/// Swift async/await wrapper around the KMP `WishlistRepository`.
/// ViewModels call this wrapper and never touch Kotlin types directly.
@MainActor
final class WishlistRepositoryWrapper {
    private let repository: WishlistRepository

    init(repository: WishlistRepository) {
        self.repository = repository
    }

    func loadWishlists() async throws -> [WishlistModel] {
        // Force a network refresh first, then read from local cache.
        // Current user id resolved through AuthRepository wrapper.
        let authRepo = AppContainer.shared.authRepository
        let userId = authRepo.currentUserId()
        if authRepo.isAuthenticated() {
            try await repository.refresh(ownerId: userId)
        }
        return try await withCheckedThrowingContinuation { cont in
            Task {
                do {
                    // Observe the flow once (first emission) via a tiny helper.
                    let stream = repository.observeWishlists(ownerId: userId)
                    var iterator = stream.makeAsyncIterator()
                    if let first = try await iterator.next() {
                        let list = (first as? [Wishlist]) ?? []
                        cont.resume(returning: list.map(WishlistModel.init))
                    } else {
                        cont.resume(returning: [])
                    }
                } catch {
                    cont.resume(throwing: error)
                }
            }
        }
    }

    func getWishlist(id: String) async throws -> WishlistModel? {
        guard let kmp = try await repository.getWishlist(id: id) else { return nil }
        return WishlistModel(kmp)
    }

    func createWishlist(
        name: String, coverType: CoverType, coverValue: String?, access: Access
    ) async throws -> WishlistModel {
        let kmp = try await repository.createWishlist(
            name: name, coverType: coverType, coverValue: coverValue, access: access
        )
        return WishlistModel(kmp)
    }

    func deleteWishlist(id: String) async throws {
        try await repository.deleteWishlist(id: id)
    }

    func createItem(wishlistId: String, req: ItemCreateRequest) async throws -> WishlistItemModel {
        let kmp = try await repository.createItem(wishlistId: wishlistId, req: req)
        return WishlistItemModel(kmp)
    }

    func deleteItem(wishlistId: String, itemId: String) async throws {
        try await repository.deleteItem(wishlistId: wishlistId, itemId: itemId)
    }

    func parseUrl(_ url: String) async throws -> ParsedProduct {
        try await repository.parseProductUrl(url: url)
    }
}
