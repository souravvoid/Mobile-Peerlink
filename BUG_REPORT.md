# Bug Report

## Bug 1: NullPointerException on Service Binding
- **File:** `app/src/main/java/com/example/domain/TransferManager.kt`
- **Line:** ~29
- **Severity:** High (Crash on launch/test under specific OS environments where binder takes time to yield valid services).
- **Root Cause:** Android OS can sometimes invoke `onServiceConnected` with a null `IBinder` depending on process death/memory contexts; the previous typed parameters (`className: ComponentName, service: IBinder`) explicitly crashed in Kotlin since it forces Non-Null checking.
- **Impact:** Crashes the application unpredictably in integration scenarios or Robolectric environments.

## Bug 2: Compile Errors from Hilt / KSP
- **File:** `app/build.gradle.kts`
- **Severity:** Critical (Blocks build).
- **Root Cause:** Missing explicit string configurations for `hilt-android-testing` and compiler KSP directives. A custom test implementation of Hilt was declared as a map, resolving effectively to a non-existent class target.
- **Impact:** Entire continuous integration workflow broken.

## Bug 3: Viewmodel Factory / Missing Context 
- **File:** `MainActivity.kt`
- **Severity:** Medium
- **Root Cause:** Reverted back to Hilt, but an explicit custom `ViewModelProvider.Factory` was still hanging around in `MainActivity.kt` causing Dagger to throw an implicit cast mismatch between internal dependencies.
- **Impact:** Prevents clean DI.
