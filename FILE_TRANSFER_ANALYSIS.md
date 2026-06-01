# File Transfer Analysis

## Current file transfer flow
The previous file transfer flow allowed sending a single file using `ActivityResultContracts.GetContent()`. It relied on a custom TCP socket protocol managed by `TransferManager` via `TransferService`. 
1. Sender picks a single file URI.
2. Sender sets up ServerSocket and negotiates ECDH.
3. Once symmetric keys are derived (AES-GCM), sender would push File Name and File Size before streaming encrypted bytes.
4. Receiver confirms ECDH public keys via a fingerprint validation prompt.

## Sender Flow (Old)
- `SendScreen` invoked `GetContent`.
- `PeerLinkViewModel` started `FileSender` and broadcast an invite code representing its IP.

## Receiver Flow (Old)
- Receiver connected to Sender IP/Port.
- Received File metadata followed by exact stream chunks until `-1` chunk termination character was encountered.

## Limitations
- Only one file could be transported at a time.
- The approval dialog (`AlertDialog` in `MainApp`) only demonstrated cryptographic integrity, not what files were actually pending.
- Image previews or batch sharing were unsupported.

## Proposed Strategy
Migrated exactly to an `OpenMultipleDocuments` contract, yielding a `List<Uri>`. 
Introduced `TransferMetadata` JSON via Moshi to declare all pending files dynamically across the socket, allowing the receiving peer to inspect incoming files natively.
