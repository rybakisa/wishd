import SwiftUI
import Shared

struct AuthView: View {
    @StateObject private var vm = AuthViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Spacer()
                Text("Wishlist").font(.largeTitle).bold()
                Text("Sign in to share and sync").foregroundStyle(.secondary)
                Spacer().frame(height: 32)

                Button {
                    Task {
                        if await vm.signInWithApple() { dismiss() }
                    }
                } label: {
                    Text("Continue with Apple").fontWeight(.semibold).frame(maxWidth: .infinity)
                }
                .controlSize(.large)
                .buttonStyle(.borderedProminent)
                .tint(.primary)

                Button {
                    Task {
                        if await vm.signInWithGoogle() { dismiss() }
                    }
                } label: {
                    Text("Continue with Google").fontWeight(.semibold).frame(maxWidth: .infinity)
                }
                .controlSize(.large)
                .buttonStyle(.bordered)

                if let err = vm.errorMessage {
                    Text(err).foregroundStyle(.red).font(.caption)
                }
                Spacer()
            }
            .padding(.horizontal, 24)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) { Button("Cancel") { dismiss() } }
            }
            .disabled(vm.busy)
        }
    }
}
