import SwiftUI
import Shared

private let CURRENCIES = ["USD","EUR","GBP","JPY","RUB","CAD","AUD","CHF","INR","CNY"]

struct AddWishlistItemView: View {
    @StateObject private var vm: AddItemViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var urlText: String = ""
    @State private var name: String = ""
    @State private var imageUrl: String = ""
    @State private var description: String = ""
    @State private var price: String = ""
    @State private var currency: String = "USD"
    @State private var size: String = ""
    @State private var comment: String = ""

    init(wishlistId: String) {
        _vm = StateObject(wrappedValue: AddItemViewModel(wishlistId: wishlistId))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Paste link") {
                    HStack {
                        TextField("URL", text: $urlText)
                            .keyboardType(.URL)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        Button("Parse") { Task { await vm.parseUrl(urlText) } }
                            .disabled(urlText.isEmpty || vm.busy)
                    }
                }
                Section("Details") {
                    TextField("Name *", text: $name)
                    TextField("Image URL", text: $imageUrl).keyboardType(.URL).textInputAutocapitalization(.never)
                    TextField("Description", text: $description, axis: .vertical)
                    HStack {
                        TextField("Price", text: $price).keyboardType(.decimalPad)
                        Picker("", selection: $currency) {
                            ForEach(CURRENCIES, id: \.self) { Text($0).tag($0) }
                        }
                        .pickerStyle(.menu)
                    }
                    TextField("Size", text: $size)
                    TextField("Comment", text: $comment, axis: .vertical)
                }
                if let err = vm.errorMessage {
                    Section { Text(err).foregroundStyle(.red).font(.caption) }
                }
            }
            .navigationTitle("Add item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") { Task { await save() } }
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || vm.busy)
                }
            }
            .onChange(of: vm.parsed) { _, parsed in
                guard let p = parsed else { return }
                name = p.name
                imageUrl = p.imageUrl ?? imageUrl
                description = p.description_ ?? description
                if let pv = p.price { price = String(pv.doubleValue) }
                if let c = p.currency { currency = c }
            }
        }
    }

    private func save() async {
        let req = ItemCreateRequest(
            name: name.trimmingCharacters(in: .whitespaces),
            url: urlText.isEmpty ? nil : urlText,
            imageUrl: imageUrl.isEmpty ? nil : imageUrl,
            description: description.isEmpty ? nil : description,
            price: Double(price).map { KotlinDouble(value: $0) },
            currency: currency.isEmpty ? "USD" : currency,
            size: size.isEmpty ? nil : size,
            comment: comment.isEmpty ? nil : comment,
            sortOrder: 0
        )
        if await vm.save(req) { dismiss() }
    }
}
