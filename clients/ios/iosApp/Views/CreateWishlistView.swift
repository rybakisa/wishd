import SwiftUI

struct CreateWishlistView: View {

    @ObservedObject var viewModel: WishlistViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var description = ""
    @State private var isPublic = false
    @State private var isSaving = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Wishlist name", text: $name)
                    TextField("Description (optional)", text: $description, axis: .vertical)
                        .lineLimit(2...)
                }

                Section {
                    Toggle("Make public", isOn: $isPublic)
                } footer: {
                    Text("Public wishlists can be shared with a link.")
                }
            }
            .navigationTitle("New Wishlist")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") { Task { await create() } }
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || isSaving)
                }
            }
        }
    }

    private func create() async {
        isSaving = true
        defer { isSaving = false }
        await viewModel.createWishlist(
            name: name.trimmingCharacters(in: .whitespaces),
            description: description,
            isPublic: isPublic
        )
        dismiss()
    }
}
