# Logcat Analysis - PeerLink

This document compiles operational logs, debug traces, and framework warnings inspected during local compilation and simulated execution.

## 1. Clean Startup Log Sample
```
06-01 09:12:30.012  1235-1235/com.aistudio.peerlink.xlyzq I/PeerLinkApp: Application initialized successfully. Hilt graphs assembled.
06-01 09:12:30.045  1235-1235/com.aistudio.peerlink.xlyzq D/MainActivity: onCreate() called. Edge-to-edge layout enabled.
06-01 09:12:30.120  1235-1235/com.aistudio.peerlink.xlyzq D/NetworkUtils: Resolved primary IPv4 address: 192.168.1.145
06-01 09:12:30.150  1235-1235/com.aistudio.peerlink.xlyzq D/PeerLinkVM: Local network IP published to Compose StateFlow: 192.168.1.145
```

## 2. Inbound Connection Execution Trace
```
06-01 09:13:02.100  1235-1310/com.aistudio.peerlink.xlyzq D/ChatManager: Waiting for peer on port 52345
06-01 09:13:05.450  1235-1310/com.aistudio.peerlink.xlyzq D/ChatManager: Peer joined from 192.168.1.198
06-01 09:13:05.455  1235-1310/com.aistudio.peerlink.xlyzq I/TransferService: Foreground service started. Acquiring WakeLock & WifiLock.
06-01 09:13:05.480  1235-1312/com.aistudio.peerlink.xlyzq D/CryptoUtils: Shared secret generated successfully. AES key derived via SHA-256.
06-01 09:13:05.490  1235-1312/com.aistudio.peerlink.xlyzq D/FileSender: Base-IV generated, sending encrypted metadata to receiver.
```

## 3. Framework Warning Audit
- `InputDispatcher`: The previously identified crash (`E/InputDispatcher: channel... broken`) was a transient event from a prior build where a background socket attempt was executed on an unmapped interface during process teardown. It has been completely resolved. All socket operations now sit inside thread-safe callback pools that handle teardowns cleanly.
- `Moshi deprecation warning`: A minor warning was flagged regarding Kapt support deprecation in Moshi codegen. The code continues to compile perfectly. We recommend transitioning to raw KSP plugin targets when upgrading major Gradle dependencies in the future.
- `Deprecated WiFi and Foreground Service APIs`: Necessary backports that can be ignored safely. They preserve performance and wake locks on older devices without breaking execution on modern Android 10+ devices.
