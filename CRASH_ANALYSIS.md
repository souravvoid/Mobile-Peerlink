# Crash and Error Resilience Report - PeerLink

A crash resilience audit was performed to guarantee that the application handles external environment errors gracefully without unexpected crashes or Application Not Responding (ANR) events.

## 1. Handled Exception Scenarios

### Scenario A: Attempting to connect with an invalid Invite Code
- **Impact Risk**: NullPointerException or BadCoordinates exception during decoding.
- **Handling**: `InviteCode.decode()` handles format violations by returning `null` instead of raising exceptions.
  ```kotlin
  fun decode(code: String): Triple<String, Int, Int>? { ... }
  ```
- **ViewModel Result**: When decoding returns `null`, the view model skips socket connection triggers, keeping the interface stable and prompting the user to re-enter coordinates.

### Scenario B: Targets are completely offline (IP/Port Unreachable)
- **Impact Risk**: Direct thread block or instant socket crash during initialization.
- **Handling**: Sockets are wrapped in try-catch-finally blocks inside clean `Dispatchers.IO` background threads.
- **ViewModel Result**: Connection errors are caught cleanly, updating the state flow's `.error` parameter and displaying a user-friendly error card rather than crashing the thread.

### Scenario C: Handshake Rejection or Fingerprint Mismatch
- **Impact Risk**: Blocked socket resources or memory leaks.
- **Handling**: Rejections write a boolean status to the socket, prompting the remote node to tear down resources.
  ```kotlin
  if (!approved || !peerApproved) {
      _stats.value = _stats.value.copy(error = "Transfer rejected", isComplete = true)
      return@withContext
  }
  ```
- **ViewModel Result**: Sockets close gracefully, resetting the system status to wait for new user connections.

### Scenario D: Storage Permissions or Directory Missing
- **Impact Risk**: `FileNotFoundException` or `PermissionDenied` when creating save paths.
- **Handling**: PeerLink attempts to auto-create missing directories before streaming blocks.
  ```kotlin
  val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
  val peerLinkDir = File(downloadsDir, "PeerLink").apply { mkdirs() }
  ```
- **ViewModel Result**: Write failures are caught by the socket's try-catch block, safely displaying a "Transfer failed: local disk error" message.
