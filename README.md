# PeerLink Android Edition

PeerLink Android is a secure, decentralized local network (P2P) file sharing application. It enables high-speed, direct device-to-device transfers without cloud intermediates.

## Features
- **Zero-Trust Encryption:** Automatic ephemeral ECDH key exchanges using secp256r1.
- **Wire-Speed AES-256-GCM:** Protects streaming file transfers.
- **Background Transfers:** Resilient socket streams handled by Android Foreground Services and WifiLocks.
- **Aurora Borealis UI:** Jetpack Compose Material 3 implementation featuring glassmorphism and animated components.
- **mDNS Auto-Discovery:** Find nearby devices without entering manual IP addresses.

## Architecture
Built with Clean Architecture, MVVM, and Dependency Injection via Hilt.
See `ARCHITECTURE.md` for full breakdown.
