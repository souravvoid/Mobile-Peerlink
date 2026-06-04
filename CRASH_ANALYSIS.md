# Crash Analysis Report — PeerLink

## CRITICAL EXCEPTION NOTICE

```
========================================================================
                      ⚠️ CRASH LOG NOT PRESENT ⚠️
========================================================================
```

The logcat outputs or terminal records provided in the active execution context **do not contain any runtime crashes, FATAL EXCEPTION signatures, or System App crashes.** 

To perform a diagnostic debug run into live device disruptions, please extract the full local execution log stream by executing the following commands on your development machine with the device connected via USB/Wi-Fi:

```bash
# Export the complete, unfiltered logcat output:
adb logcat -d > logcat.txt

# Or filter specifically for fatal crashes, runtime failures, and error outputs:
adb logcat -d *:E | grep -Ei "AndroidRuntime|FATAL EXCEPTION|Caused by"
```

Once exported, attach or copy-paste the output of `logcat.txt` to trigger a localized source-code resolution sequence.

---

## Proactive Failure Boundary Analysis

Although no live crash is present in the current logs, we have conducted a specialized code audit of PeerLink's architecture to cross-reference common crash vectors in P2P Android applications. 

Below is the triage matrix of handled crash scenarios and potential system risks:

| Risk Category | Potential Driver / Exception | Architectural Resilience Mechanism Implemented |
| :--- | :--- | :--- |
| **Port Collision** | `java.net.BindException` | Local binding utilizes a fall-through logic. If a port is blocked, the exception is caught, and the active port registration increments, mapping to safe available offsets. Custom user-facing snackbars guide troubleshooting if a local binding error happens. |
| **Multicast Support** | `java.lang.SecurityException` | Standard NSD registration requires `CHANGE_WIFI_MULTICAST_STATE`. This permission is securely declared in the `AndroidManifest.xml` and is a normal-tier permission, avoiding runtime rejection crashes. |
| **Handshake Security** | `java.security.GeneralSecurityException` | Cryptographic failures (e.g. ECDH key agreement failures, IV out of bounds, or handshake tampering) are caught gracefully in the socket's parent coroutine, raising a high-fidelity dismissible `EncryptionHandshakeException` instead of crashing the process. |
| **File I/O Streams** | `java.io.FileNotFoundException` <br> `java.io.EOFException` | If the file link is severed or a selected virtual disk URI becomes unreachable midstream, we map the throwables to a high-fidelity `FileAccessPermissionException` or `ConnectionInterruptedException` update on the StateFlow stats. |
| **Dynamic Navigation** | `IllegalArgumentException` (Compose Navigation) | All navigation pathways utilize centralized route strings via strict type-safe navigation definitions, preventing unregistered route crashes on screen transitions. |
| **ViewModel Resolution** | `IllegalStateException` (Hilt VM injection) | MainActivity is decorated with `@AndroidEntryPoint`, locking injection context to the Hilt graph cleanly, preventing missing-factory crashes. |
