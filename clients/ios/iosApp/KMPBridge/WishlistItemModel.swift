import Foundation
import Shared

/// Swift value type mirroring KMP `WishlistItem`.
struct WishlistItemModel: Identifiable, Equatable {
    let id: String
    let wishlistId: String
    var name: String
    var url: String?
    var imageUrl: String?
    var description: String?
    var price: Double?
    var currency: String
    var size: String?
    var comment: String?
    var sortOrder: Int

    init(_ kmp: WishlistItem) {
        id = kmp.id
        wishlistId = kmp.wishlistId
        name = kmp.name
        url = kmp.url
        imageUrl = kmp.imageUrl
        description = kmp.description_
        price = kmp.price?.doubleValue
        currency = kmp.currency
        size = kmp.size
        comment = kmp.comment
        sortOrder = Int(kmp.sortOrder)
    }
}

/// Swift value type mirroring KMP `Wishlist`.
struct WishlistModel: Identifiable, Equatable {
    let id: String
    let ownerId: String
    var name: String
    var coverType: CoverType
    var coverValue: String?
    var access: Access
    let shareToken: String
    var items: [WishlistItemModel]

    init(_ kmp: Wishlist) {
        id = kmp.id
        ownerId = kmp.ownerId
        name = kmp.name
        coverType = kmp.coverType
        coverValue = kmp.coverValue
        access = kmp.access
        shareToken = kmp.shareToken
        items = kmp.items.map(WishlistItemModel.init)
    }
}

extension CoverType {
    /// Wire string: "none" | "emoji" | "image"
    var wireValue: String { wire() }
}

extension Access {
    /// Wire string: "link" | "public" | "private"
    var wireValue: String { wire() }
    var displayLabel: String { wireValue.capitalized }
}
