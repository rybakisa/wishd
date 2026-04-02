import SwiftUI

struct WishlistDetailView: View {

    let wishlist: WishlistModel
    @ObservedObject var viewModel: WishlistViewModel
    @State private var showingAddItem = false

    var body: some View {
        List {
            ForEach(wishlist.items) { item in
                NavigationLink(destination: WishlistItemDetailView(item: item, viewModel: viewModel)) {
                    ItemRow(item: item, onToggle: {
                        Task { await viewModel.togglePurchased(item) }
                    })
                }
            }
            .onDelete { offsets in
                let ids = offsets.map { wishlist.items[$0].id }
                Task {
                    for id in ids { await viewModel.deleteItem(id: id) }
                }
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if wishlist.items.isEmpty {
                ContentUnavailableView(
                    "No Items",
                    systemImage: "cart",
                    description: Text("Tap + to add items to \(wishlist.name).")
                )
            }
        }
        .navigationTitle(wishlist.name)
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showingAddItem = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAddItem) {
            AddWishlistItemView(wishlistId: wishlist.id, viewModel: viewModel)
        }
    }
}

private struct ItemRow: View {
    let item: WishlistItemModel
    let onToggle: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onToggle) {
                Image(systemName: item.isPurchased ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(item.isPurchased ? .green : .secondary)
                    .font(.title2)
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .strikethrough(item.isPurchased)
                    .foregroundStyle(item.isPurchased ? .secondary : .primary)

                if let price = item.price {
                    Text(price, format: .currency(code: item.currency))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
