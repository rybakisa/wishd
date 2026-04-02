import SwiftUI

struct WishlistListView: View {

    @StateObject private var viewModel = WishlistViewModel(
        repository: AppContainer.shared.wishlistRepository
    )
    @State private var showingCreate = false

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading…")
            } else {
                list
            }
        }
        .navigationTitle("Wishlists")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showingCreate = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingCreate) {
            CreateWishlistView(viewModel: viewModel)
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") {}
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .onAppear { viewModel.onAppear() }
        .onDisappear { viewModel.onDisappear() }
    }

    private var list: some View {
        List {
            ForEach(viewModel.wishlists) { wishlist in
                NavigationLink(destination: WishlistDetailView(wishlist: wishlist, viewModel: viewModel)) {
                    WishlistRow(wishlist: wishlist)
                }
            }
            .onDelete { offsets in
                Task { await viewModel.deleteWishlists(at: offsets) }
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if viewModel.wishlists.isEmpty {
                ContentUnavailableView(
                    "No Wishlists",
                    systemImage: "gift",
                    description: Text("Tap + to create your first wishlist.")
                )
            }
        }
    }
}

private struct WishlistRow: View {
    let wishlist: WishlistModel

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(wishlist.name)
                .font(.headline)
            HStack {
                Text("\(wishlist.items.count) item\(wishlist.items.count == 1 ? "" : "s")")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if wishlist.isPublic {
                    Label("Public", systemImage: "globe")
                        .font(.caption2)
                        .foregroundStyle(.blue)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    NavigationStack { WishlistListView() }
}
