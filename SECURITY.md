# PeerLink Security Protocol

## 1. Local Network Vault (Zero Trust)
PeerLink assumes the Local Area Network (WiFi) is hostile and subject to packet sniffing.

## 2. Key Exchange
*   Algorithm: `secp256r1` (ECDH) via Android Conscrypt Service.
*   Ephemeral keys are generated for **every single session**; they are not cached.
*   Vulnerability handling: MITM. Both clients will display a visual fingerprint of the AES shared key derived from the ECDH handshake. Users must visually verify to confirm uncompromised connections.

## 3. Storage
*   Keys: Kept statically in process memory during transfer. Promptly dropped on completion.
*   Disk Input/Output: Saved files are sanitized specifically for Directory Traversal vectors (`..`, `/`).
*   Output Directory: Android Scoped Storage (`MediaStore`/`Downloads` directory) strictly enforced instead of demanding `MANAGE_EXTERNAL_STORAGE` permissions.
*   No legacy `java.io.File` absolute paths are parsed during staging. We read via `ContentResolver.openInputStream(uri)` complying strictly with Android 11+ App Storage isolation mandates.

## 4. Transfer Cipher
*   **AES-256-GCM**.
*   A 12-byte random base IV is generated via `SecureRandom`. Every subsequent 4MB payload chunk increments this IV sequentially. 
*   Metadata is transmitted heavily chunked with embedded encrypted length headers preventing Out-of-Memory (OOM) stream bomb attacks.
*   Unlike earlier implementations where primitive data arrays passed implicitly over standard byte boundaries, the `Moshi` JSON object representing names and sizes is entirely passed *after* ECDH cryptographic enforcement, maintaining metadata security across promiscuous local networks.

## 5. Input Validation
*   Port parsing guarantees 1024-65535 boundaries.
*   IPs are validated correctly.

## 6. Secrets / Environment Variables
*   No hardcoded access keys. Application does not talk to third party telemetry or APIs.
