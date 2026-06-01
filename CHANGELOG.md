# Changelog

## [1.1.0] - Native Multi-File Transfer
- Added support for selecting and securely sending multiple files sequentially. 
- Integrated `Moshi` for sending typed JSON `TransferMetadata` packets securely over AES-GCM.
- Rebuilt `SendScreen` and `ReceiveScreen` UI to display file queues dynamically.

## [1.0.0] - Initial Release
- Implemented core PeerLink engine over secp256r1 and AES-256-GCM.
- Created base UI structures and transfer protocol logic. 
- Initial Foreground service configuration.
