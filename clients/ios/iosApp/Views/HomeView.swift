import SwiftUI
import Shared

struct HomeView: View {
    @StateObject private var vm = HomeViewModel()
    @State private var showCreate = false
    @State private var showAuth = false

    var body: some View {
        Group {
            if vm.wishlists.isEmpty {
                emptyState
            } else {
                list
            }
        }
        .navigationTitle("Wishlists")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            if vm.currentUser == nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Sign in") { showAuth = true }
                }
            } else if !vm.wishlists.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showCreate = true }) { Image(systemName: "plus") }
                }
            }
        }
        .sheet(isPresented: $showCreate, onDismiss: { Task { await vm.reload() } }) {
            CreateWishlistView()
        }
        .sheet(isPresented: $showAuth, onDismiss: { Task { await vm.onAppear() } }) {
            AuthView()
        }
        .task { await vm.onAppear() }
        .refreshable { await vm.reload() }
        .alert("Error", isPresented: .constant(vm.errorMessage != nil), actions: {
            Button("OK") { vm.errorMessage = nil }
        }, message: { Text(vm.errorMessage ?? "") })
    }

    private var emptyState: some View {
        VStack(spacing: 20) {
            Button(action: { showCreate = true }) {
                ZStack {
                    Circle().fill(Color.accentColor).frame(width: 140, height: 140)
                    Image(systemName: "plus")
                        .font(.system(size: 64, weight: .thin))
                        .foregroundStyle(.white)
                }
            }
            .buttonStyle(.plain)
            Text("Create your first wishlist").font(.title3).fontWeight(.semibold)
            Text("Tap to start").foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var list: some View {
        List {
            ForEach(vm.wishlists) { w in
                NavigationLink(destination: WishlistDetailView(wishlistId: w.id)) {
                    row(for: w)
                }
            }
            .onDelete { indexSet in
                for i in indexSet {
                    let id = vm.wishlists[i].id
                    Task { await vm.delete(id) }
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private func row(for w: WishlistModel) -> some View {
        HStack(spacing: 12) {
            CoverBadge(type: w.coverType, value: w.coverValue)
            VStack(alignment: .leading, spacing: 2) {
                Text(w.name).font(.headline)
                Text("\(w.items.count) items · \(w.access.displayLabel.lowercased())")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

struct CoverBadge: View {
    let type: CoverType
    let value: String?
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12).fill(Color(UIColor.secondarySystemBackground))
            switch type {
            case .emoji: Text(value ?? "🎁").font(.title)
            default: Text("🎁").font(.title)
            }
        }
        .frame(width: 52, height: 52)
    }
}
