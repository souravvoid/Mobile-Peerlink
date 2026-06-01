# Security Validation - PeerLink

A cryptographical and architectural review of PeerLink's secure communication layer was conducted.

## 1. Zero-Knowledge Handshake Configuration
PeerLink establishes secure channels between two unknown physical nodes without requiring a centralized certificate authority or external cloud discovery servers. It solves this using an **Ephemeral Elliptic Curve Diffie-Hellman (ECDH)** key agreement protocol.

- **Curve selection**: prime256v1 / secp256r1. This is a highly robust, NSA-approved industrial curve that resistant to standard cryptographical attack vectors.
- **Key Generation**: 
  ```kotlin
  val kpg = KeyPairGenerator.getInstance("EC")
  kpg.initialize(256)
  return kpg.generateKeyPair()
  ```
- **Symmetric Secret Derivation**: Shared secrets are derived on-device, and then transformed into a flat AES-256 transmission key using SHA-256 hashing.
  ```kotlin
  val digest = MessageDigest.getInstance("SHA-256")
  val hash = digest.digest(sharedSecret)
  return SecretKeySpec(hash, "AES")
  ```

## 2. AES-256-GCM Secure Transport
The decrypted stream or individual metadata is never processed in cleartext. Sockets run authenticated encryption through raw AES in Galois Counter Mode (GCM).

- **Authentication assurance (AEAD)**: Ensures that any bit modifications or data injection attempts by hostile third-party nodes will corrupt the integrity tag, immediately halting socket operations before writing payload blocks.
- **Tag Validation depth**: 128-bit authentication tags.
- **Unique IV rotation**: PeerLink avoids GCM's critical "nonce reuse" vulnerability by generating a random 12-byte base-IV at startup, and then modifying the 11th byte index iteratively with a monotonic counter for each transmitted block.
  ```kotlin
  val currentIv = baseIv.copyOf()
  currentIv[11] = (currentIv[11] + ivCounter).toByte()
  ```

## 3. Human-In-The-Loop MitM Prevention
Even if symmetric channels are secure, an attacker on the same local area network could intercept the coordination handshake. PeerLink mitigates this through **Fingerprint-based Human-In-The-Loop Validation**.

1. The sender and receiver exchange raw public keys over the socket.
2. PeerLink hashes the peer's public key using SHA-256 and displays the first 8 characters as a hex fingerprint:
   ```kotlin
   val fingerprint = CryptoUtils.generateFingerprint(peerKeyBytes)
   ```
3. Before raw payloads can be sent, both devices render high-contrast approval dialogs displaying this fingerprint value. Users visually verify that the fingerprint matches across boards, making Man-in-the-Middle interceptions impossible.
