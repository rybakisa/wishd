import SwiftUI
import Shared

private let EMOJIS = ["🎁","🎂","🎄","🎉","💝","💍","💐","📱","💻","🎧","📚","🎮","⚽","🏀","🎸","🎨","🧸","👟","👗","👜","⌚","💎","🧁","🍰","🏠","🚗","✈️","🏝","☕","🍷","🌸","🌟"]

struct CreateWishlistView: View {
    @StateObject private var vm = CreateWishlistViewModel()
    @Environment(\.dismiss) private var dismiss

    @State private var name: String = ""
    @State private var coverTypeIdx: Int = 0  // 0: emoji, 1: image, 2: none
    @State private var selectedEmoji: String = "🎁"
    @State private var imageUrl: String = ""
    @State private var accessIdx: Int = 0  // 0: link, 1: public, 2: private
    @State private var showLoginPrompt = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Name") {
                    TextField("Gift list name", text: $name)
                }
                Section("Cover") {
                    Picker("Cover", selection: $coverTypeIdx) {
                        Text("Emoji").tag(0)
                        Text("Image").tag(1)
                        Text("None").tag(2)
                    }
                    .pickerStyle(.segmented)

                    if coverTypeIdx == 0 { emojiGrid }
                    else if coverTypeIdx == 1 {
                        TextField("Image URL", text: $imageUrl).keyboardType(.URL).textInputAutocapitalization(.never)
                    }
                }
                Section("Access") {
                    Picker("Access", selection: $accessIdx) {
                        Text("Link").tag(0)
                        Text("Public").tag(1)
                        Text("Private").tag(2)
                    }
                    .pickerStyle(.segmented)
                }
                if let err = vm.errorMessage {
                    Section { Text(err).foregroundStyle(.red).font(.caption) }
                }
            }
            .navigationTitle("New wishlist")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Create") {
                        Task { await submit() }
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || vm.busy)
                }
            }
            .task {
                if vm.requiresLogin { showLoginPrompt = true }
            }
            .sheet(isPresented: $showLoginPrompt, onDismiss: {
                if vm.requiresLogin { dismiss() }
            }) { AuthView() }
        }
    }

    private var emojiGrid: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 8), spacing: 8) {
            ForEach(EMOJIS, id: \.self) { e in
                Text(e).font(.title2)
                    .frame(width: 34, height: 34)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(e == selectedEmoji ? Color.accentColor.opacity(0.2) : Color(UIColor.secondarySystemBackground))
                    )
                    .onTapGesture { selectedEmoji = e }
            }
        }
        .padding(.vertical, 4)
    }

    private func submit() async {
        let coverType: CoverType
        let coverValue: String?
        switch coverTypeIdx {
        case 0: coverType = CoverType.emoji; coverValue = selectedEmoji
        case 1: coverType = CoverType.image; coverValue = imageUrl.isEmpty ? nil : imageUrl
        default: coverType = CoverType.none; coverValue = nil
        }
        let access: Access = [Access.link, Access.public_, Access.private_][accessIdx]
        if let _ = await vm.create(name: name.trimmingCharacters(in: .whitespaces),
                                   coverType: coverType, coverValue: coverValue, access: access) {
            dismiss()
        }
    }
}
