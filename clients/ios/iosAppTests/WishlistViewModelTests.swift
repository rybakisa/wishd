import XCTest
@testable import iosApp
import Shared

@MainActor
final class WishlistViewModelTests: XCTestCase {

    // MARK: - Helpers

    private func makeViewModel(wishlists: [WishlistModel] = []) -> (WishlistViewModel, FakeRepository) {
        let fake = FakeRepository(initialWishlists: wishlists)
        let wrapper = WishlistRepositoryWrapper(repository: fake)
        return (WishlistViewModel(repository: wrapper), fake)
    }

    // MARK: - Tests

    func testInitialStateIsEmpty() {
        let (vm, _) = makeViewModel()
        XCTAssertTrue(vm.wishlists.isEmpty)
        XCTAssertFalse(vm.isLoading)
        XCTAssertNil(vm.errorMessage)
    }

    func testOnAppearDeliversWishlists() async {
        let wishlist = makeWishlist(name: "Birthday")
        let (vm, _) = makeViewModel(wishlists: [wishlist])
        vm.onAppear()
        try? await Task.sleep(nanoseconds: 10_000_000)
        XCTAssertEqual(vm.wishlists.count, 1)
        XCTAssertEqual(vm.wishlists.first?.name, "Birthday")
    }

    func testTogglePurchasedCallsRepository() async {
        let item = makeItem(name: "Book", wishlistId: "wl-1", isPurchased: false)
        let wishlist = WishlistModel(Wishlist(
            id: "wl-1", name: "Test", description: "",
            items: [item.toKMP()], ownerId: "u", isPublic: false
        ))
        let (vm, fake) = makeViewModel(wishlists: [wishlist])
        vm.onAppear()
        try? await Task.sleep(nanoseconds: 10_000_000)

        await vm.togglePurchased(item)

        XCTAssertEqual(fake.lastMarkPurchasedId, item.id)
        XCTAssertEqual(fake.lastMarkPurchasedValue, true)
    }

    func testDeleteWishlistCallsRepository() async {
        let wishlist = makeWishlist(name: "Old List")
        let (vm, fake) = makeViewModel(wishlists: [wishlist])
        vm.onAppear()
        try? await Task.sleep(nanoseconds: 10_000_000)

        await vm.deleteWishlist(id: wishlist.id)

        XCTAssertTrue(fake.deletedWishlistIds.contains(wishlist.id))
    }

    // MARK: - Factory

    private func makeWishlist(name: String) -> WishlistModel {
        WishlistModel(Wishlist(
            id: UUID().uuidString, name: name, description: "",
            items: [], ownerId: "test-user", isPublic: false
        ))
    }

    private func makeItem(name: String, wishlistId: String, isPurchased: Bool = false) -> WishlistItemModel {
        WishlistItemModel(WishlistItem(
            id: UUID().uuidString, wishlistId: wishlistId, name: name,
            description: "", url: nil, imageUrl: nil,
            price: nil, currency: "USD", isPurchased: isPurchased
        ))
    }
}

// MARK: - Fake repository (pure Swift — no KMP runtime)

private final class FakeRepository: WishlistRepository {

    private let initialWishlists: [WishlistModel]
    private(set) var lastMarkPurchasedId: String?
    private(set) var lastMarkPurchasedValue: Bool?
    private(set) var deletedWishlistIds: [String] = []
    private(set) var deletedItemIds: [String] = []

    init(initialWishlists: [WishlistModel]) {
        self.initialWishlists = initialWishlists
    }

    func getWishlists(userId: String) -> any WishlistsFlow {
        return FakeWishlistFlow(wishlists: initialWishlists.map { $0.toKMP() })
    }

    func getWishlist(id: String) async throws -> Wishlist? {
        initialWishlists.first(where: { $0.id == id })?.toKMP()
    }

    func createWishlist(wishlist: Wishlist) async throws -> Wishlist { wishlist }
    func updateWishlist(wishlist: Wishlist) async throws -> Wishlist { wishlist }

    func deleteWishlist(id: String) async throws {
        deletedWishlistIds.append(id)
    }

    func addItem(wishlistId: String, item: WishlistItem) async throws -> WishlistItem { item }
    func updateItem(item: WishlistItem) async throws -> WishlistItem { item }

    func deleteItem(id: String) async throws {
        deletedItemIds.append(id)
    }

    func markItemPurchased(itemId: String, purchased: Bool) async throws {
        lastMarkPurchasedId = itemId
        lastMarkPurchasedValue = purchased
    }
}
