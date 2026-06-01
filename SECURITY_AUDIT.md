# Security Audit: PeerLink Android

## 1. Cryptographic Suite Analysis
The application ensures zero-trust local networking via an appropriately modern cryptographic stack:
- **Key Exchange**: ECDH (Elliptic Curve Diffie-Hellman) parameterizing secure ad-hoc exchanges.
- **Symmetric Encryption**: AES/GCM/NoPadding (Advanced Encryption Standard - Galois/Counter Mode).
- **Integrity Checking**: GCM inherently provides authenticated encryption, guaranteeing that ciphertext manipulations are trapped.
- **Key Derivation**: ECDH output is appropriately hashed using SHA-256 before serving as an AES symmetric key.

## 2. Parameter Evaluation
- **Initialization Vectors (IV)**: `CryptoUtils.GCM_IV_LENGTH` is set explicitly to 12 bytes. This aligns perfectly with NIST standards for peak AES-GCM performance securely minimizing collision boundaries.
- **Tag Validation**: 128-bit authentication tagging guarantees tamper impedance.
- **Nonce/IV Generation**: `SecureRandom` guarantees highly entropic generation for the primary root IV. The stream utilizes IV counter rotations.

## 3. Threat Model Simulation & Findings
- **Replay Attacks**: Ad-hoc derivation of asymmetric keypairs on every initialized session explicitly blocks token capture-and-replay attacks.
- **Modified Packets / Altered Ciphertext**: AES-GCM will immediately fail to decrypt via `Cipher.doFinal`, terminating streams securely.
- **Silent Corruption**: Since GCM authentication tags validate ciphertext integrity, injected malware segments logically fail validation, halting propagation.

## 4. Android System Security Permissions
- `android.permission.INTERNET`, `ACCESS_WIFI_STATE`, `NEARBY_WIFI_DEVICES`: Restricted appropriately to ad-hoc local topologies.
- **Data Persistence**: Uses Scoped Storage and transient local caches. No private keys are permanently persisted in shared `/sdcard` environments, mitigating broad disk-scraping vectors.

## 5. Potential Security Improvements
- Implement Strict TLS streams wrapping the sockets on top of AES-GCM, utilizing explicit mutual-certificate trusting if persistent device histories are mapped.
- Ensure RAM cleanup methodologies zeroize `byte[]` arrays post-encryption to mitigate RAM heap scraping tools (though realistically unfeasible for generalized non-rooted threat vectors).
