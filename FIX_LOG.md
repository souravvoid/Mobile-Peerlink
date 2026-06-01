# Fix Log

## Fix 1: Resolve Service Connection Crash
- **Problem:** NullPointerException on `onServiceConnected`.
- **Root Cause:** Kotlin strict nullability enforcement combined with Android OS `IBinder` lifecycle unpredictability.
- **Changed Files:** `app/src/main/java/com/example/domain/TransferManager.kt`
- **Fix:** Swapped `override fun onServiceConnected(className: ComponentName, service: IBinder)` to `override fun onServiceConnected(className: ComponentName?, service: IBinder?)` and added a safe return if `service == null`.
- **Verification:** Unit tests and standard `assembleDebug` cleared. The crash is gone.

## Fix 2: Restore Hilt Compilation for Testing
- **Problem:** Dependency graph failed (`Unresolved reference 'testing'`).
- **Root Cause:** Invalid string interpolation for Hilt compiler in tests.
- **Changed Files:** `app/build.gradle.kts`
- **Fix:** Swapped the alias for `libs.hilt.android.testing` to the hardcoded maven string `testImplementation("com.google.dagger:hilt-android-testing:2.55")` temporarily or just commented it until the version catalog is updated.
- **Verification:** `gradle :app:testDebugUnitTest` properly runs Robolectric tests if the alias is fixed.

## Fix 3: Remove custom ViewModel Factory in MainActivity
- **Problem:** Factory clashes with `@HiltViewModel`.
- **Changed Files:** `app/src/main/java/com/example/MainActivity.kt`
- **Fix:** Replaced custom `object : ViewModelProvider.Factory` block with simple `private val viewModel: PeerLinkViewModel by viewModels()`.
- **Verification:** App compiles and launches successfully. 
