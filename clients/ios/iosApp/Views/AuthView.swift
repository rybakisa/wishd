import SwiftUI
import Shared

struct AuthView: View {
    @StateObject private var vm = AuthViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showEmailSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Spacer()
                Text("Wishlist").font(.largeTitle).bold()
                Text("Sign in to share and sync").foregroundStyle(.secondary)
                Spacer().frame(height: 32)

                Button {
                    Task {
                        if await vm.login(provider: .apple,
                                          email: "apple-user@privaterelay.appleid.com",
                                          displayName: "Apple User") { dismiss() }
                    }
                } label: {
                    Text("Continue with Apple").fontWeight(.semibold).frame(maxWidth: .infinity)
                }
                .controlSize(.large)
                .buttonStyle(.borderedProminent)
                .tint(.primary)

                Button {
                    Task {
                        if await vm.login(provider: .google,
                                          email: "google-user@gmail.com",
                                          displayName: "Google User") { dismiss() }
                    }
                } label: {
                    Text("Continue with Google").fontWeight(.semibold).frame(maxWidth: .infinity)
                }
                .controlSize(.large)
                .buttonStyle(.bordered)

                Button {
                    showEmailSheet = true
                } label: {
                    Text("Continue with Email").fontWeight(.semibold).frame(maxWidth: .infinity)
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
            .sheet(isPresented: $showEmailSheet) {
                EmailLoginSheet(vm: vm, onDone: { dismiss() })
            }
            .disabled(vm.busy)
        }
    }
}

private struct EmailLoginSheet: View {
    @ObservedObject var vm: AuthViewModel
    let onDone: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var email: String = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
            }
            .navigationTitle("Sign in")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Continue") {
                        Task {
                            if await vm.login(provider: .email, email: email, displayName: nil) {
                                dismiss(); onDone()
                            }
                        }
                    }
                    .disabled(!email.contains("@") || vm.busy)
                }
            }
        }
    }
}
