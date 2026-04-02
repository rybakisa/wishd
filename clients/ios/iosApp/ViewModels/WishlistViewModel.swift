import Foundation
import SwiftUI

@MainActor
final class WishlistViewModel: ObservableObject {

    @Published private(set) var wishlists: [WishlistModel] = []
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?

    private let repository: WishlistRepositoryWrapper
    private let userId: String
    private var streamTask: Task<Void, Never>?

    init(repository: WishlistRepositoryWrapper, userId: String = "current-user") {
        self.repository = repository
        self.userId = userId
    }

    // MARK: - Lifecycle

    func onAppear() {
        guard streamTask == nil else { return }
        isLoading = true
        streamTask = Task { [weak self] in
            guard let self else { return }
            for await batch in self.repository.wishlists(userId: self.userId) {
                self.wishlists = batch
                self.isLoading = false
            }
        }
    }

    func onDisappear() {
        streamTask?.cancel()
        streamTask = nil
    }

    // MARK: - Wishlist Actions

    func createWishlist(name: String, description: String, isPublic: Bool) async {
        do {
            _ = try await repository.createWishlist(
                name: name,
                description: description,
                isPublic: isPublic,
                ownerId: userId
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deleteWishlist(id: String) async {
        do {
            try await repository.deleteWishlist(id: id)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deleteWishlists(at offsets: IndexSet) async {
        let ids = offsets.map { wishlists[$0].id }
        for id in ids { await deleteWishlist(id: id) }
    }

    // MARK: - Item Actions

    func addItem(to wishlistId: String, name: String, description: String, url: String?, price: Double?, currency: String = "USD") async {
        do {
            _ = try await repository.addItem(
                wishlistId: wishlistId,
                name: name,
                description: description,
                url: url,
                price: price,
                currency: currency
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func togglePurchased(_ item: WishlistItemModel) async {
        do {
            try await repository.markItemPurchased(itemId: item.id, purchased: !item.isPurchased)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deleteItem(id: String) async {
        do {
            try await repository.deleteItem(id: id)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func updateItem(_ item: WishlistItemModel) async {
        do {
            _ = try await repository.updateItem(item)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
