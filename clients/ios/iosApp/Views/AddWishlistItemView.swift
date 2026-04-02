import SwiftUI

struct AddWishlistItemView: View {

    let wishlistId: String
    @ObservedObject var viewModel: WishlistViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var description = ""
    @State private var url = ""
    @State private var price = ""
    @State private var currency = "USD"
    @State private var isSaving = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Required") {
                    TextField("Item name", text: $name)
                }

                Section("Optional") {
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(2...)
                    TextField("URL", text: $url)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                    HStack {
                        TextField("Price", text: $price)
                            .keyboardType(.decimalPad)
                        Picker("", selection: $currency) {
                            ForEach(["USD", "EUR", "GBP", "JPY", "CAD", "AUD"], id: \.self) {
                                Text($0)
                            }
                        }
                        .pickerStyle(.menu)
                        .labelsHidden()
                    }
                }
            }
            .navigationTitle("New Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { Task { await add() } }
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || isSaving)
                }
            }
        }
    }

    private func add() async {
        isSaving = true
        defer { isSaving = false }
        await viewModel.addItem(
            to: wishlistId,
            name: name.trimmingCharacters(in: .whitespaces),
            description: description,
            url: url.isEmpty ? nil : url,
            price: Double(price),
            currency: currency
        )
        dismiss()
    }
}
