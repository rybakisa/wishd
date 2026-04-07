import Foundation
import Shared

@MainActor
final class AuthRepositoryWrapper {
    private let repo: AuthRepository

    init(repo: AuthRepository) {
        self.repo = repo
        // Restore persisted session on construction (fire-and-forget).
        Task { try? await repo.restore() }
    }

    func signInWithGoogle() async throws {
        try await repo.signInWithGoogle()
    }

    func signInWithApple() async throws {
        try await repo.signInWithApple()
    }

    func logout() async throws {
        try await repo.logout()
    }

    func currentUser() -> AuthUser? {
        repo.currentUser.value as? AuthUser
    }

    func currentUserId() -> String {
        repo.userIdOrAnon()
    }

    func isAuthenticated() -> Bool {
        repo.isAuthenticated()
    }
}
