import SwiftUI
import Shared

struct WishlistDetailView: View {
    @StateObject private var vm: WishlistDetailViewModel
    @State private var showAdd = false
    @State private var showShareSheet = false

    init(wishlistId: String) {
        _vm = StateObject(wrappedValue: WishlistDetailViewModel(wishlistId: wishlistId))
    }

    var body: some View {
        Group {
            if let w = vm.wishlist {
                content(for: w)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(vm.wishlist?.name ?? "Wishlist")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let w = vm.wishlist {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showShareSheet = true } label: { Image(systemName: "square.and.arrow.up") }
                        .disabled(w.shareToken.isEmpty)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showAdd = true } label: { Image(systemName: "plus") }
                }
            }
        }
        .task { await vm.load() }
        .sheet(isPresented: $showAdd, onDismiss: { Task { await vm.load() } }) {
            if let w = vm.wishlist { AddWishlistItemView(wishlistId: w.id) }
        }
        .sheet(isPresented: $showShareSheet) {
            if let w = vm.wishlist {
                ShareSheet(items: ["https://wishlist.app/s/\(w.shareToken)"])
            }
        }
    }

    @ViewBuilder
    private func content(for w: WishlistModel) -> some View {
        if w.items.isEmpty {
            ContentUnavailableView("No items yet", systemImage: "gift", description: Text("Tap + to add one"))
        } else {
            List {
                ForEach(w.items) { item in
                    ItemRow(item: item)
                }
                .onDelete { indexSet in
                    for i in indexSet {
                        let id = w.items[i].id
                        Task { await vm.deleteItem(id) }
                    }
                }
            }
            .listStyle(.insetGrouped)
        }
    }
}

private struct ItemRow: View {
    let item: WishlistItemModel
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(item.name).font(.body).fontWeight(.medium)
            if let d = item.description, !d.isEmpty {
                Text(d).font(.caption).foregroundStyle(.secondary).lineLimit(2)
            }
            HStack(spacing: 12) {
                if let p = item.price {
                    Text("\(formatPrice(p)) \(item.currency)").font(.caption).foregroundStyle(.primary)
                }
                if let s = item.size, !s.isEmpty {
                    Text("size \(s)").font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private func formatPrice(_ p: Double) -> String {
    let f = NumberFormatter()
    f.numberStyle = .decimal
    f.maximumFractionDigits = 2
    return f.string(from: NSNumber(value: p)) ?? "\(p)"
}
