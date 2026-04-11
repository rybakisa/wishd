# ADR-001: KMP Architecture Evaluation & Improvements

**Status:** Proposed  
**Date:** 2026-04-09  
**Deciders:** Andrey Ezhov

## Context

The wishlist-app KMP client (`clients/shared/`) targets Android and iOS using Compose Multiplatform, Ktor, SQLDelight, Koin, and Supabase Auth. The codebase is well-structured with clean separation of concerns (auth, data, domain, network, storage, UI) and good test coverage. This ADR evaluates the current architecture against KMP best practices and proposes targeted improvements.

## Current Architecture Assessment

### What Works Well

| Area | Assessment |
|------|-----------|
| **Module structure** | Single `shared` module with clean `commonMain/androidMain/iosMain` split |
| **Repository pattern** | Interface in `domain/`, impl in `data/` with local-first caching |
| **SQLDelight usage** | Reactive flows via `.asFlow()`, proper transactions, type-safe queries |
| **Ktor setup** | Bearer auth with automatic token refresh, content negotiation |
| **Testing** | MockEngine for HTTP, in-memory SQLite, `runTest` for coroutines |
| **Auth state machine** | Well-defined sealed class states, mutex for sync, retry-loop prevention |
| **Compose Multiplatform** | 100% shared UI with platform-specific actions via CompositionLocal |

### Issues Identified

---

## Decision: Proposed Improvements

### 1. Type-Safe Navigation (High Priority)

**Problem:** Routes are string-based (`"detail/{id}"`), which is fragile and loses type safety at compile time. Argument extraction via `backStackEntry.arguments?.getString("id")` can silently return null.

**Current** (`AppNavigation.kt:14-21`):
```kotlin
object Routes {
    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/$id"
}
```

**Recommended:** Use the Kotlin Serialization-based type-safe navigation available since Navigation Compose 2.8+. Define routes as `@Serializable` data objects/classes:

```kotlin
@Serializable data object Home
@Serializable data object Auth
@Serializable data object Create
@Serializable data class Detail(val id: String)
@Serializable data class AddItem(val wishlistId: String)
```

| Dimension | String Routes (Current) | Type-Safe Routes |
|-----------|------------------------|------------------|
| Compile-time safety | None | Full |
| Refactoring risk | High (silent breaks) | Low (compiler catches) |
| Effort | -- | Low (already on Nav 2.8) |

---

### 2. Hardcoded `Dispatchers` (High Priority)

**Problem:** `Dispatchers.Default` is hardcoded throughout `WishlistRepositoryImpl` and `AuthRepository`. This makes unit testing harder and violates KMP best practice of dispatcher injection.

**Current** (`WishlistRepositoryImpl.kt:9,11`):
```kotlin
import kotlinx.coroutines.Dispatchers
// ...
.mapToList(Dispatchers.Default)
suspend fun getWishlist(id: String) = withContext(Dispatchers.Default) { ... }
```

**Recommended:** Inject a `CoroutineDispatcher` or use a `DispatcherProvider` interface:

```kotlin
class WishlistRepositoryImpl(
    private val api: WishlistApiClient,
    private val db: WishlistDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WishlistRepository {
    override fun observeWishlists(ownerId: String) =
        q.selectWishlistsByOwner(ownerId).asFlow().mapToList(dispatcher)
}
```

Same applies to `AuthRepository.kt:31`:
```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

---

### 3. Unscoped `CoroutineScope` in `AuthRepository` (High Priority)

**Problem:** `AuthRepository` creates its own `CoroutineScope` (`AuthRepository.kt:31`) that is never cancelled. This leaks coroutines and is a lifecycle hazard, especially on iOS where there's no automatic scope management.

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

**Recommended options:**
- **Option A:** Accept a `CoroutineScope` as a constructor parameter (injected from the platform lifecycle).
- **Option B:** Implement `Closeable`/`AutoCloseable` and cancel the scope in `close()`, then manage lifecycle in Koin via `onClose { it.close() }`.
- **Option C:** If `AuthRepository` is truly app-scoped singleton, document this explicitly and accept the trade-off.

Option B is the cleanest for DI:
```kotlin
class AuthRepository(...) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override fun close() { scope.cancel() }
}

// In Koin:
single { AuthRepository(get(), get(), get(), get()) } onClose { it?.close() }
```

---

### 4. Missing UI State Abstraction (Medium Priority)

**Problem:** Every ViewModel independently manages loading/error/data states with separate `MutableStateFlow` fields. This leads to duplicated patterns and makes it easy to forget resetting error state.

**Current pattern repeated in 5 ViewModels:**
```kotlin
private val _error = MutableStateFlow<String?>(null)
private val _isLoading = MutableStateFlow(false)
```

**Recommended:** Introduce a `UiState<T>` sealed class:
```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

This reduces boilerplate and ensures consistent error/loading handling across screens.

---

### 5. N+1 Query in `observeWishlists` (Medium Priority)

**Problem:** `WishlistRepositoryImpl.kt:26` executes a separate `selectItemsByWishlist` query for each wishlist row inside a `map` operator on the Flow. This is an N+1 query pattern that will degrade as the wishlist count grows.

```kotlin
.map { rows ->
    rows.map { row ->
        val items = q.selectItemsByWishlist(row.id).executeAsList().map { it.toDomain() }
        row.toDomain(items)
    }
}
```

**Recommended:** Add a SQLDelight query that JOINs wishlists with items in a single query, or prefetch all items for the owner and group in Kotlin:

```kotlin
.map { rows ->
    val allItems = q.selectItemsByOwner(ownerId).executeAsList()
    val itemsByWishlist = allItems.groupBy { it.wishlist_id }
    rows.map { row -> row.toDomain(itemsByWishlist[row.id]?.map { it.toDomain() } ?: emptyList()) }
}
```

---

### 6. Domain Models Coupled to Serialization (Medium Priority)

**Problem:** Domain models in `WishlistModels.kt` are annotated with `@Serializable` and `@SerialName`. These are network/data concerns leaking into what should be pure domain types. The same classes serve as API DTOs, database mappers, and domain entities.

**Recommended:** For a project of this size, this is acceptable pragmatism. However, if the API and local schema start diverging (e.g., backend adds fields the client doesn't need, or vice versa), consider splitting into:
- `network/dto/` - API response/request types with `@Serializable`
- `data/` - Domain models (plain data classes)
- Mappers between them

**Verdict:** Low urgency. Flag for when API/domain divergence becomes painful.

---

### 7. Error Handling: Raw Exception Messages in UI (Medium Priority)

**Problem:** ViewModels expose `throwable.message` directly to the UI (`HomeViewModel.kt:39,50`). This can leak technical details (HTTP status codes, stack traces) to users.

```kotlin
.catch { _error.value = it.message }
runCatching { repo.refresh(user.id) }.onFailure { _error.value = it.message }
```

**Recommended:** Map exceptions to user-friendly messages:
```kotlin
private fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> when (status) {
        401 -> "Session expired. Please sign in again."
        in 500..599 -> "Server error. Please try again later."
        else -> "Something went wrong."
    }
    is java.net.UnknownHostException -> "No internet connection."
    else -> "An unexpected error occurred."
}
```

Note: On KMP, use `expect/actual` or Ktor's exception types for platform-independent network error detection.

---

### 8. iOS Framework Configuration (Low Priority)

**Problem:** The iOS framework is configured as `isStatic = true` (`build.gradle.kts:28`). This is fine, but you're missing `export()` declarations for any shared types that Swift code needs to reference directly. The iOS layer uses `KoinComponentHelper` bridge classes, which works but adds boilerplate.

**Recommendation:** Consider using [SKIE](https://skie.touchlab.co/) for better Swift interop. It generates:
- Sealed class exhaustive `switch` in Swift
- Flow-to-AsyncSequence bridging (eliminating manual `KMPBridge/` wrappers)
- Suspend function to async/await mapping

---

### 9. Missing Gradle Convention Plugin (Low Priority)

**Problem:** Build configuration is spread across `shared/build.gradle.kts` and `android/build.gradle.kts` with duplicated settings (JVM target, Android SDK versions, etc.).

**Recommended:** Extract shared build config into a Gradle convention plugin under `build-logic/` or use `buildSrc`. This is standard KMP practice for multi-module projects and becomes essential as modules grow.

---

### 10. Database Migrations (Low Priority)

**Problem:** No SQLDelight migration files exist. The current setup works only for fresh installs or `destructiveMigrate`. When the schema evolves, this will cause data loss on app updates.

**Recommended:** Configure SQLDelight migrations:
```kotlin
sqldelight {
    databases {
        create("WishlistDatabase") {
            packageName.set("com.wishlist.shared.storage")
            deriveSchemaFromMigrations.set(true)
            // or: verifyMigrations.set(true)
        }
    }
}
```

Create numbered migration files: `1.sqm`, `2.sqm`, etc.

---

## Summary: Priority Matrix

| # | Improvement | Priority | Effort | Impact |
|---|------------|----------|--------|--------|
| 1 | Type-safe navigation | High | Low | Eliminates runtime route errors |
| 2 | Inject dispatchers | High | Low | Testability + KMP correctness |
| 3 | Scope lifecycle for AuthRepository | High | Low | Prevents coroutine leaks |
| 4 | `UiState<T>` sealed class | Medium | Low | Reduces ViewModel boilerplate |
| 5 | Fix N+1 query | Medium | Low | Performance at scale |
| 6 | Separate DTOs from domain | Medium | Medium | Cleaner boundaries (defer) |
| 7 | User-friendly error messages | Medium | Low | Better UX |
| 8 | SKIE for iOS interop | Low | Medium | Eliminates Swift bridge boilerplate |
| 9 | Gradle convention plugins | Low | Medium | Build maintainability |
| 10 | SQLDelight migrations | Low | Low | Data safety on updates |

## Consequences

**What becomes easier:**
- Testing (dispatcher injection, scoped coroutines)
- Refactoring navigation (compiler catches broken routes)
- Scaling the UI layer (consistent state pattern)
- iOS development (better Swift interop with SKIE)

**What becomes harder:**
- Nothing significant; all changes are incremental and non-breaking

**What to revisit:**
- DTO separation (#6) when API versioning or schema divergence occurs
- Multi-module split if the `shared` module grows beyond ~50 source files

## Action Items

1. [ ] Migrate to type-safe navigation routes (#1)
2. [ ] Inject `CoroutineDispatcher` into repositories (#2)
3. [ ] Add `Closeable` to `AuthRepository` with Koin `onClose` (#3)
4. [ ] Introduce `UiState<T>` sealed class (#4)
5. [ ] Optimize `observeWishlists` query (#5)
6. [ ] Map exceptions to user-friendly messages (#7)
7. [ ] Evaluate SKIE for iOS interop (#8)
8. [ ] Set up SQLDelight migration strategy (#10)
