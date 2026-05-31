# PeerLink P2P File Transfer - Implementation Plan

## Phase 1: Project Setup & Core Configuration
- [ ] Configure `applicationId` to `com.aistudio.peerlink.xxxxx`
- [ ] Update `metadata.json` and `strings.xml` with "PeerLink"
- [ ] Add Hilt for UI and Dependency Injection.
- [ ] Add Navigation Compose, Coroutines, Lifecycle dependencies.
- [ ] Configure `AndroidManifest.xml` with required Local Network, Wi-Fi, and Foreground permissions.

## Phase 2: Security & Cryptography Layer (`security/`)
- [ ] Implement `CryptoUtils`: ECDH (secp256r1) key generation, Shared Secret Derivation via SHA-256.
- [ ] Implement AES-GCM-256 cipher streams with moving sequential IVs.
- [ ] Implement Fingerprint Generator for Key Verification (SHA-256 of public key).

## Phase 3: Networking & Device Discovery (`network/`)
- [ ] Implement `NsdManager` for mDNS background discovery (Peer Publisher and Listener).
- [ ] Create `PortUtils` for dynamic ephemeral port allocation.
- [ ] Build raw TCP `ServerSocket` and `Socket` clients using Kotlin Coroutines + `Dispatchers.IO`.

## Phase 4: Transfer Logic & Domain Layer (`transfer/`, `domain/`)
- [ ] Build `FileSender` mapped to asynchronous coroutine channels.
- [ ] Build `FileReceiver` to sequential download and decrypt to Scoped Storage (Downloads/PeerLink).
- [ ] Implement `TransferStats` Flow state.
- [ ] Build Model and UseCases for presentation integration.

## Phase 5: Background Execution (`core/`, `util/`)
- [ ] Implement `TransferForegroundService` supporting `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
- [ ] Implement ongoing system Notifications with progress bars. 
- [ ] Manage `WifiLock` (HIGH_PERF) and `WakeLock` lifecycles.

## Phase 6: Presentation UI (`presentation/`)
- [ ] Setup "Aurora Borealis" Material 3 Theme (Glassmorphism, glowing buttons, teal/violet accents).
- [ ] Build `NavGraph` (Home, Send, Receive, Settings).
- [ ] Build Home Screen (Identity Card, Receive/Send fast actions).
- [ ] Build Send Screen (File Picker, Start Sending, Progress state).
- [ ] Build Receive Screen (Input code or network peer list, Verification Dialog, Progress state).
- [ ] Generate App Icons.

## Phase 7: Validation
- [ ] Verify Gradle compile.
- [ ] Verify Permissions flow.
- [ ] End-to-end Socket logic review.
