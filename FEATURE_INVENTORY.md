# Feature Inventory - PeerLink

This document contains a comprehensive record of the capabilities implemented inside the PeerLink codebase, including their active functional state, validation source code classes, and evidence of implementation robustness.

| Feature Area | Sub-Feature | Status | Verification Classes | Evidence of implementation |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication & Cryptography** | ECDH Ephemeral Key Exchange | **Complete** | `CryptoUtils.kt`, `CryptoUtilsTest.kt` | KeyPair generator using standardized prime256v1 curve; produces temporary symmetric secrets off-device. |
| | AES-256-GCM Transport Encryption | **Complete** | `CryptoUtils.kt` | Symmetric transport with Galois Counter Mode (GCM) ensuring metadata and payload tag verification (integrity). |
| | SHA-256 Fingerprint Generation | **Complete** | `CryptoUtils.kt`, `CryptoUtilsTest.kt` | Hash-based unique peer fingerprints preventing Man-in-the-Middle (MitM) interceptions. |
| | Base64 Secure IV Rotation | **Complete** | `FileSender.kt`, `FileReceiver.kt` | Rotates individual 12-byte initialization vectors using block-based counter additions to prevent GCM nonce reuse failures. |
| **P2P Coordinate Mapping** | Base58 Invite Codes | **Complete** | `InviteCode.kt` | Compresses IP, file listener port, and chat sockets into an alphanumeric coordinate string readable/writable across devices. |
| | Local IP Resolution | **Complete** | `NetworkUtils.kt` | Isolates interface configurations to dynamically extract primary IPv4 adapters, skipping inert loopback/tun configurations. |
| **Socket Transfer Engine** | Dynamic Port Mapping | **Complete** | `FileSender.kt`, `ChatManager.kt` | Initializes listening servers using automatic port assignment (`0`) to avoid local socket reservation conflicts. |
| | Buffered Block Streaming | **Complete** | `FileSender.kt`, `FileReceiver.kt` | Sends documents in robust 2MB byte chunks (`1024 * 1024 * 2`) ensuring multi-gigabyte transfers execute without OOM bounds. |
| | Multi-File Queues | **Complete** | `FileSender.kt`, `FileReceiver.kt`, `Navigation.kt` | Compiles composite local URI queues into serial streams accompanied by robust dynamic JSON metadata packets. |
| **State & Background Handling** | Data-Sync Foreground Service | **Complete** | `TransferService.kt` | Enforces background execution persistence using system wake locks, high-performance Wi-Fi locks, and native status bar notifications. |
| | Unidirectional View States | **Complete** | `PeerLinkViewModel.kt`, `Navigation.kt` | Exposes standard architectural `StateFlow` metrics containing reactive speeds, percentages, and peer confirmations. |
| **Interactive Collaboration** | Live Encrypted Socket Chat | **Complete** | `ChatManager.kt`, `Navigation.kt` | Operates an asynchronous duplex text channel over a sibling TCP port allowing participants to message during active transfers. |
| | Chat File Insertion | **Complete** | `ChatManager.kt`, `Navigation.kt` | Supports inline immediate files additions by sending custom command commands directly over the duplex chat loop. |
