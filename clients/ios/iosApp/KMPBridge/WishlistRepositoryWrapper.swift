import Foundation
import Shared

/// Swift async/await wrapper around the KMP `WishlistRepository`.
///
/// Bridges Kotlin `Flow` / `suspend` functions so SwiftUI ViewModels
/// never import Kotlin types directly.
@MainActor
final class WishlistRepositoryWrapper {

    private let repository: WishlistRepository

    init(repository: WishlistRepository) {
        self.repository = repository
    }

    // MARK: - Wishlists

    /// Live stream of all wishlists owned by the user.
    /// Uses SKIE-generated async sequence (or fallback shim) over the KMP Flow.
    func wishlists(userId: String) -> AsyncStream<[WishlistModel]> {
        AsyncStream { continuation in
            Task {
                do {
                    for try await batch in repository.getWishlists(userId: userId) {
                        let models = (batch as! [Wishlist]).map(WishlistModel.init)
                        continuation.yield(models)
                    }
                } catch {
                    continuation.finish()
                }
            }
        }
    }

    func getWishlist(id: String) async throws -> WishlistModel? {
        guard let kmp = try await repository.getWishlist(id: id) else { return nil }
        return WishlistModel(kmp)
    }

    func createWishlist(name: String, description: String, isPublic: Bool, ownerId: String) async throws -> WishlistModel {
        let kmp = try await repository.createWishlist(wishlist: Wishlist(
            id: UUID().uuidString,
            name: name,
            description: description,
            items: [],
            ownerId: ownerId,
            isPublic: isPublic
        ))
        return WishlistModel(kmp)
    }

    func updateWishlist(_ model: WishlistModel) async throws -> WishlistModel {
        let kmp = try await repository.updateWishlist(wishlist: model.toKMP())
        return WishlistModel(kmp)
    }

    func deleteWishlist(id: String) async throws {
        try await repository.deleteWishlist(id: id)
    }

    // MARK: - Items

    func addItem(wishlistId: String, name: String, description: String, url: String?, price: Double?, currency: String) async throws -> WishlistItemModel {
        let kmp = try await repository.addItem(
            wishlistId: wishlistId,
            item: WishlistItem(
                id: UUID().uuidString,
                wishlistId: wishlistId,
                name: name,
                description: description,
                url: url,
                imageUrl: nil,
                price: price.map { KotlinDouble(value: $0) },
                currency: currency,
                isPurchased: false
            )
        )
        return WishlistItemModel(kmp)
    }

    func updateItem(_ model: WishlistItemModel) async throws -> WishlistItemModel {
        let kmp = try await repository.updateItem(item: model.toKMP())
        return WishlistItemModel(kmp)
    }

    func deleteItem(id: String) async throws {
        try await repository.deleteItem(id: id)
    }

    func markItemPurchased(itemId: String, purchased: Bool) async throws {
        try await repository.markItemPurchased(itemId: itemId, purchased: purchased)
    }
}
