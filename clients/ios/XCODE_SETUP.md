# iOS Xcode Project Setup

Follow these steps to wire the KMP `Shared` XCFramework into the `iosApp` Xcode project.

## 1. Build the KMP XCFramework

```bash
cd wishlist-kmp
./gradlew :shared:assembleReleaseXCFramework
# Output: shared/build/XCFrameworks/release/Shared.xcframework
```

## 2. Create the Xcode Project

1. Open Xcode → **File › New › Project**
2. Choose **iOS › App**
   - Product Name: `iosApp`
   - Bundle ID: `com.wishlist.iosApp`
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Save into `wishlist-kmp/iosApp/`
3. Delete the generated `ContentView.swift` and `iosApp.swift` — the files in this folder replace them.

## 3. Link the XCFramework

1. Select the `iosApp` target → **General › Frameworks, Libraries, and Embedded Content**
2. Click **+** → **Add Files** → navigate to `shared/build/XCFrameworks/release/Shared.xcframework`
3. Set **Embed** to **Do Not Embed** (static framework).

## 4. Add SKIE (Swift/Kotlin Interop Enhancer)

Add the SKIE Gradle plugin to `shared/build.gradle.kts` so Kotlin coroutine `Flow`s
become native Swift `AsyncSequence`s:

```kotlin
plugins {
    // …existing plugins…
    id("co.touchlab.skie") version "0.9.0"
}
```

Then rebuild the XCFramework (step 1). The `WishlistRepository.getItems` flow becomes
directly iterable with `for await batch in repository.getItems(userId:)`.

## 5. Add Source Files

Add all `.swift` files from `iosApp/iosApp/` to the Xcode target:

```
iosApp/
  WishlistApp.swift
  ContentView.swift
  DI/
    KoinHelper.swift
    AppContainer.swift
  KMPBridge/
    WishlistRepositoryWrapper.swift
    WishlistItemModel.swift
    WishlistItemFactory.swift
  ViewModels/
    WishlistViewModel.swift
  Views/
    WishlistListView.swift
    WishlistItemDetailView.swift
    AddWishlistItemView.swift
```

## 6. Minimum Deployment Target

Set **iOS Deployment Target** to **16.0** or later (required for `ContentUnavailableView`
and certain Swift concurrency features).

## 7. Run Tests

Add `iosAppTests/WishlistViewModelTests.swift` to the test target and run:

```
Cmd+U
```

All tests should pass without a running simulator (they use an in-memory fake repository).

## 8. Phase 2 Hook-Up Checklist

Once `WIS-20` (Phase 2: Shared Data & Network Layer) is done:

- [ ] Uncomment `single<WishlistRepository> { WishlistRepositoryImpl(get(), get()) }` in
      `shared/src/iosMain/.../PlatformModule.ios.kt`
- [ ] Confirm `KoinComponentKt.getWishlistRepository()` resolves the real implementation
- [ ] Run the full test suite (`Cmd+U`) and UI smoke test on simulator
- [ ] Verify no URLSession or CoreData imports remain in `iosApp/`
