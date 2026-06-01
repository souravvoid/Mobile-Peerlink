# PeerLink Android Edition

PeerLink Android is a secure, decentralized local network (P2P) file sharing application. It enables high-speed, direct device-to-device transfers without cloud intermediates.

## Features
- **Zero-Trust Encryption:** Automatic ephemeral ECDH key exchanges using secp256r1.
- **Wire-Speed AES-256-GCM:** Protects streaming file transfers natively across the LAN.
- **Background Transfers:** Resilient socket streams handled by Android Foreground Services and WifiLocks.
- **Aurora Borealis UI:** Jetpack Compose Material 3 implementation featuring glassmorphism and animated components.
- **mDNS Auto-Discovery:** Find nearby devices without entering manual IP addresses.
- **Multiple File Transfers**: Select and share large batches of files automatically handled via dynamically parsed Moshi JSON boundaries.

## Architecture Overview
Built with Clean Architecture, MVVM, and Dependency Injection via Hilt.
For detailed information, please read the [Architecture Documentation](ARCHITECTURE.md).

## Installation & Build Instructions
Please read [INSTALLATION.md](INSTALLATION.md) for IDE and runtime setup steps, and compile tasks.

## Usage
1. Open PeerLink on both Android devices.
2. Ensure both devices are on the same Wi-Fi network (or connected to one another's hotspot).
3. The sender selects files and generates an invite boundary.
4. The receiver either automatically discovers the sender via mDNS or enters the Invite Code.
5. The receiver explicitly validates the shared cryptographic fingerprint displayed on both devices before approving the connection.

## Testing
Detailed methodologies and test reports can be found in [TESTING.md](TESTING.md).

## Security
See [SECURITY.md](SECURITY.md) for details on zero-trust constraints, memory scopes, and algorithms.

## Contributing
Please see our [CONTRIBUTING.md](CONTRIBUTING.md) rules before making pull requests.

## Developer Documentation
- [API and Protocol Details](API.md)
- [Deployment Guidelines](DEPLOYMENT.md)
- [Development Workflow](DEVELOPMENT.md)
- [Roadmap](ROADMAP.md)
- [Changelog](CHANGELOG.md)
