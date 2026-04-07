import Foundation
import SwiftUI
import Shared

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var wishlists: [WishlistModel] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var currentUser: AuthUser?

    private let repo: WishlistRepositoryWrapper
    private let auth: AuthRepositoryWrapper

    init(repo: WishlistRepositoryWrapper = AppContainer.shared.wishlistRepository,
         auth: AuthRepositoryWrapper = AppContainer.shared.authRepository) {
        self.repo = repo
        self.auth = auth
    }

    func onAppear() async {
        currentUser = auth.currentUser()
        await reload()
    }

    func reload() async {
        isLoading = true
        defer { isLoading = false }
        do {
            wishlists = try await repo.loadWishlists()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func delete(_ id: String) async {
        do {
            try await repo.deleteWishlist(id: id)
            await reload()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var busy: Bool = false
    @Published var errorMessage: String?

    private let auth: AuthRepositoryWrapper

    init(auth: AuthRepositoryWrapper = AppContainer.shared.authRepository) {
        self.auth = auth
    }

    func signInWithGoogle() async -> Bool {
        busy = true
        defer { busy = false }
        do {
            try await auth.signInWithGoogle()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func signInWithApple() async -> Bool {
        busy = true
        defer { busy = false }
        do {
            try await auth.signInWithApple()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}

@MainActor
final class WishlistDetailViewModel: ObservableObject {
    @Published var wishlist: WishlistModel?
    @Published var errorMessage: String?

    let wishlistId: String
    private let repo: WishlistRepositoryWrapper

    init(wishlistId: String, repo: WishlistRepositoryWrapper = AppContainer.shared.wishlistRepository) {
        self.wishlistId = wishlistId
        self.repo = repo
    }

    func load() async {
        do { wishlist = try await repo.getWishlist(id: wishlistId) }
        catch { errorMessage = error.localizedDescription }
    }

    func deleteItem(_ itemId: String) async {
        do {
            try await repo.deleteItem(wishlistId: wishlistId, itemId: itemId)
            await load()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

@MainActor
final class AddItemViewModel: ObservableObject {
    @Published var busy: Bool = false
    @Published var errorMessage: String?
    @Published var parsed: ParsedProduct?

    let wishlistId: String
    private let repo: WishlistRepositoryWrapper

    init(wishlistId: String, repo: WishlistRepositoryWrapper = AppContainer.shared.wishlistRepository) {
        self.wishlistId = wishlistId
        self.repo = repo
    }

    func parseUrl(_ url: String) async {
        busy = true
        defer { busy = false }
        do { parsed = try await repo.parseUrl(url) }
        catch { errorMessage = "Could not parse URL" }
    }

    func save(_ req: ItemCreateRequest) async -> Bool {
        busy = true
        defer { busy = false }
        do {
            _ = try await repo.createItem(wishlistId: wishlistId, req: req)
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}

@MainActor
final class CreateWishlistViewModel: ObservableObject {
    @Published var busy: Bool = false
    @Published var errorMessage: String?

    private let repo: WishlistRepositoryWrapper
    private let auth: AuthRepositoryWrapper

    init(repo: WishlistRepositoryWrapper = AppContainer.shared.wishlistRepository,
         auth: AuthRepositoryWrapper = AppContainer.shared.authRepository) {
        self.repo = repo
        self.auth = auth
    }

    var requiresLogin: Bool { !auth.isAuthenticated() }

    func create(name: String, coverType: CoverType, coverValue: String?, access: Access) async -> WishlistModel? {
        busy = true
        defer { busy = false }
        do {
            return try await repo.createWishlist(
                name: name, coverType: coverType, coverValue: coverValue, access: access
            )
        } catch {
            errorMessage = error.localizedDescription
            return nil
        }
    }
}
