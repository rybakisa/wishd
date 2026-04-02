import SwiftUI

struct WishlistItemDetailView: View {

    let item: WishlistItemModel
    @ObservedObject var viewModel: WishlistViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var editedName: String
    @State private var editedDescription: String
    @State private var editedUrl: String
    @State private var editedPrice: String
    @State private var editedCurrency: String
    @State private var isSaving = false

    init(item: WishlistItemModel, viewModel: WishlistViewModel) {
        self.item = item
        self.viewModel = viewModel
        _editedName = State(initialValue: item.name)
        _editedDescription = State(initialValue: item.description)
        _editedUrl = State(initialValue: item.url ?? "")
        _editedPrice = State(initialValue: item.price.map { String($0) } ?? "")
        _editedCurrency = State(initialValue: item.currency)
    }

    var body: some View {
        Form {
            Section("Details") {
                TextField("Name", text: $editedName)
                TextField("Description", text: $editedDescription, axis: .vertical)
                    .lineLimit(2...)
                TextField("URL", text: $editedUrl)
                    .keyboardType(.URL)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
            }

            Section("Price") {
                HStack {
                    TextField("0.00", text: $editedPrice)
                        .keyboardType(.decimalPad)
                    Picker("Currency", selection: $editedCurrency) {
                        ForEach(["USD", "EUR", "GBP", "JPY", "CAD", "AUD"], id: \.self) {
                            Text($0)
                        }
                    }
                    .pickerStyle(.menu)
                    .labelsHidden()
                }
            }

            if let url = item.url, let parsed = URL(string: url) {
                Section {
                    Link("Open Link", destination: parsed)
                }
            }
        }
        .navigationTitle(item.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { Task { await save() } }
                    .disabled(editedName.trimmingCharacters(in: .whitespaces).isEmpty || isSaving)
            }
        }
    }

    private func save() async {
        isSaving = true
        defer { isSaving = false }
        var updated = item
        updated.name = editedName.trimmingCharacters(in: .whitespaces)
        updated.description = editedDescription
        updated.url = editedUrl.isEmpty ? nil : editedUrl
        updated.price = Double(editedPrice)
        updated.currency = editedCurrency
        await viewModel.updateItem(updated)
        dismiss()
    }
}
