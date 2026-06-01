# File Security Audit

## ECDH and Forward Secrecy
The protocol maintains its use of `java.security.KeyPairGenerator` strictly generating `EC` (Elliptic Curve) profiles per session. The handshake correctly mitigates Man-In-The-Middle attacks due to physical visual verification of the derived fingerprint string on both screens.

## Metadata Transports
Unlike earlier implementations where primitive data arrays passed implicitly over standard byte boundaries, the `Moshi` JSON object representing names and sizes is entirely passed *after* ECDH cryptographic enforcement, maintaining metadata security across promiscuous local networks.

## Data Layer (AES-GCM)
The system leverages AES-GCM (Galois/Counter Mode). A base Initialization Vector (IV) is shared over the secured channel, and monotonically incremented `ivCounter` states isolate every individual file chunk during loop iterations. Nonce reuse vulnerabilities are fully averted by mutating index buffers predictably.

## Scoped Storage
No legacy `java.io.File` absolute paths are parsed during staging. We read via `ContentResolver.openInputStream(uri)` complying strictly with Android 11+ App Storage isolation mandates. Saves happen cleanly into standard `Environment.DIRECTORY_DOWNLOADS`.
