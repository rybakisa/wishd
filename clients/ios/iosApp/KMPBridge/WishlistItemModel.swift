import Foundation
import Shared

/// Swift value type mirroring `WishlistItem` from the KMP shared module.
struct WishlistItemModel: Identifiable, Equatable {
    let id: String
    let wishlistId: String
    var name: String
    var description: String
    var url: String?
    var imageUrl: String?
    var price: Double?
    var currency: String
    var isPurchased: Bool

    init(_ kmp: WishlistItem) {
        id = kmp.id
        wishlistId = kmp.wishlistId
        name = kmp.name
        description = kmp.description
        url = kmp.url
        imageUrl = kmp.imageUrl
        price = kmp.price?.doubleValue
        currency = kmp.currency
        isPurchased = kmp.isPurchased
    }

    func toKMP() -> WishlistItem {
        WishlistItem(
            id: id,
            wishlistId: wishlistId,
            name: name,
            description: description,
            url: url,
            imageUrl: imageUrl,
            price: price.map { KotlinDouble(value: $0) },
            currency: currency,
            isPurchased: isPurchased
        )
    }
}

/// Swift value type mirroring `Wishlist` from the KMP shared module.
struct WishlistModel: Identifiable, Equatable {
    let id: String
    var name: String
    var description: String
    var items: [WishlistItemModel]
    let ownerId: String
    var isPublic: Bool

    init(_ kmp: Wishlist) {
        id = kmp.id
        name = kmp.name
        description = kmp.description
        items = kmp.items.map { WishlistItemModel($0) }
        ownerId = kmp.ownerId
        isPublic = kmp.isPublic
    }

    func toKMP() -> Wishlist {
        Wishlist(
            id: id,
            name: name,
            description: description,
            items: items.map { $0.toKMP() },
            ownerId: ownerId,
            isPublic: isPublic
        )
    }
}
