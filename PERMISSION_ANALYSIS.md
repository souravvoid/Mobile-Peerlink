# Android Permission Model & Security Analysis — PeerLink

This permission model analysis reviews declared permissions, APIs, runtime consent flows, and modern OS optimizations (Android 13, 14, 15) to guarantee compliance and absolute startup safety.

---

## 1. Declared Permissions Audit

Below is the verification matrix for permissions declared in PeerLink's `AndroidManifest.xml`:

| Declared Permission | Classification | Usage in Application | Startup Impact |
| :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal (Auto-granted) | Spawning TCP/UDP sockets for data stream exchange and local discovery messaging. | **None** (Granted on installation). |
| `android.permission.ACCESS_NETWORK_STATE` | Normal (Auto-granted) | Querying interface configurations (e.g. Wi-Fi connection vs Mobile connection). | **None** (Granted on installation). |
| `android.permission.ACCESS_WIFI_STATE` | Normal (Auto-granted) | Checking Wi-Fi link status to alert user on inactive interfaces. | **None** (Granted on installation). |
| `android.permission.CHANGE_WIFI_MULTICAST_STATE` | Normal (Auto-granted) | Acquiring the `MulticastLock` enabling correct reception of NSD/DNS-SD discovery packets. | **None** (Granted on installation). |
| `android.permission.WAKE_LOCK` | Normal (Auto-granted) | Holding wake locks during foreground data transport to prevent CPU sleep. | **None** (Granted on installation). |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (Runtime) | Raising progress notifications and completion indicators from the foreground service (Android 13+). | **Gracefully Handled** (Prompted on MainActivity launch; non-blocking on denial). |
| `android.permission.NEARBY_WIFI_DEVICES` | Dangerous (Runtime) | Declared with `neverForLocation` flags to support advanced localized peer sockets. | **Non-blocking** (Not requested on startup, used strictly for local-scopings). |
| `android.permission.ACCESS_FINE_LOCATION` | Dangerous (Runtime) | Restricted to `maxSdkVersion="32"` for backward compatibility on legacy networks. | **None** on newer Android 13+ devices. |
| `android.permission.ACCESS_COARSE_LOCATION` | Dangerous (Runtime) | Restricted to `maxSdkVersion="32"` for backward compatibility on legacy networks. | **None** on newer Android 13+ devices. |
| `android.permission.FOREGROUND_SERVICE` | Normal (Auto-granted) | Declaring the right to run background transfer helper tasks (Android 9+). | **None** (Granted on installation). |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | Normal (Auto-granted) | Specifying API 34+ Foreground Service type (`dataSync`) as required by compiler targets. | **None** (Granted on installation). |

---

## 2. API Level Compliance (Android 13, 14, 15)

### Android 13 (API 33) — Notification Permissions
- **Handling**: `MainActivity` detects if the API is Android 13+ and checks for `POST_NOTIFICATIONS`. If not granted, it utilizes an asynchronous contract launcher `requestPermissionLauncher` without locking the UI thread.
- **Fail-safe**: Denial only prevents system tray update alerts; backports keep the transfer core fully active.

### Android 13 (API 33) — Location vs. Nearby Wi-Fi
- **Handling**: PeerLink targets `NEARBY_WIFI_DEVICES` using `android:usesPermissionFlags="neverForLocation"`. On API 33+, location access is avoided for local Wi-Fi pairing.
- **Fail-safe**: By using standard system `NsdManager` APIs for service registration and discovery, standard location or Wi-Fi scans are managed entirely by the secure OS daemon. Thus, location permission is not forced at launch.

### Android 14 / 15 (API 34+) — Foreground Service Speeds & Types
- **Handling**: Android 14 introduces stringent checks preventing general-purpose foreground services. PeerLink conforms perfectly by adding `android:foregroundServiceType="dataSync"` inside the `<service>` declaration of `TransferService` and declaring the matching `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />`.
- **Fail-safe**: Prevents the security sandbox from aborting the app during background streaming operations when the user switches tasks.

---

## 3. Startup Crash Verification
- **Are permission requests blocking?**: No. Permissions are queried dynamically, and their corresponding callbacks are fully asynchronous. 
- **Can denial cause startup crash?**: Absolutely not. Denial of notification permissions limits foreground status indicators but never affects main app loading or viewModel initialization loops.
