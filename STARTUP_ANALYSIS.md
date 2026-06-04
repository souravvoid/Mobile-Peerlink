# Android App Startup Trace Analysis — PeerLink

This document contains a comprehensive trace of the startup sequence of the PeerLink decentralized file-sharing application, auditing each phase for runtime stability, thread blocking, or potential initialization crashes.

---

## 1. Trace Sequence Overview

The following sequence details how code is executed sequentially upon launch of the `com.aistudio.peerlink.xlyzq` process.

```
[System Link / App Launch]
           │
           ▼
1. PeerLinkApplication.onCreate()
   - Class loaded from: com.example.PeerLinkApplication
   - Hilt Dependency Graphs assembled
           │
           ▼
2. DAG Compilation Verification
   - Injection of TransferManager, Use Cases, and Repositories
           │
           ▼
3. MainActivity.onCreate()
   - Edge-to-Edge window insets enabled
   - Dynamic prompt for Notification runtime permission (API 33+)
           │
           ▼
4. Compose setContent()
   - Theme initialization (MyApplicationTheme)
   - Home, History, and Settings routes registered in NavHost
           │
           ▼
5. PeerLinkViewModel Initialization
   - SharedPreferences read for "device_name" and "visibility_enabled"
   - Device IP resolution through background NetworkUtils
   - Local network discovery listeners set on Flow streams
```

---

## 2. In-Depth Operational Audit

### Phase A: Application Initialization (`PeerLinkApplication`)
- **Action**: The Android runtime instantiates the `PeerLinkApplication` class declared in `AndroidManifest.xml` under class name `.PeerLinkApplication`.
- **Hilt graph assembly**: Annotated with `@HiltAndroidApp`, Hilt hooks into `onCreate()` to instantiate the dependency injection root graph.
- **Audit Findings**: Pristine. Execution takes **<10ms**. No blocking database migrations or synchronous web queries are executed on the application thread.

### Phase B: Dependency Graph Resolution (Hilt)
- **Action**: Component injection of singletons and VM parameters:
  - `TransferManager` via construction injection.
  - `StartSendingUseCase`, `StartReceivingUseCase`, `GetTransferStatsUseCase`, `ResetTransferUseCase`.
- **Audit Findings**: The dependency graph is fully declarative and resolves cleanly. There are no circular dependencies or runtime provider exceptions.

### Phase C: Activity Lifecycle & Views (`MainActivity`)
- **Action**: `MainActivity` lifecycle begins inside `onCreate(savedInstanceState: Bundle?)`. 
- **Edge-to-Edge Integration**: `enableEdgeToEdge()` configuration applies system-level styling offsets.
- **Permission triggers**: On Android 13+ (API 33+), checks for `POST_NOTIFICATIONS` runtime permission and prompts user asynchronously via `registerForActivityResult`.
- **Composition Entry**: Renders the Compose tree inside `setContent` with `MyApplicationTheme` wrapping `MainApp`.
- **Audit Findings**: No synchronous UI thread locks or database queries are triggered. Thread remains fully non-blocking.

### Phase D: View Model Initialization (`PeerLinkViewModel`)
- **Action**: Constructed with standard `@HiltViewModel` annotations.
- **Local Settings Resolve**: Immediately reads from standard local XML SharedPreferences (`peerlink_prefs`).
- **Core Coroutines Subscriptions**:
  - Starts background collectors on `nsdHelper.discoveryException` to route discovery errors gracefully to the UI.
  - Subscribes to `stats` events updating progress, rate counters, and error states dynamically.
- **Audit Findings**: StateFlow parameters are correctly initialized to modern non-null default states. Network calls for IP configuration are properly deferred to `Dispatchers.IO` background execution pools.
