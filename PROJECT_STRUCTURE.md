# Project Structure - PeerLink

PeerLink is a secure, decentralized peer-to-peer (P2P) file-sharing and messaging application designed for high-performance localized transfers. It operates entirely on-device with zero reliance on cloud coordination pools, utilizing localized Wi-Fi/mDNS networking, Ephemeral Elliptic Curve Diffie-Hellman (ECDH) key exchanges, and AES-256-GCM symmetric transport-layer encryption.

## Architecture

PeerLink is designed following the **Model-View-ViewModel (MVVM)** pattern combined with **Clean Architecture** principles in a single-module configuration. State Management relies on Kotlin's unidirectional flows (`StateFlow` and `SharedFlow`), rendering UIs purely in **Jetpack Compose (Material Design 3)**.

```
com.example
├── di                     # Dependency Injection Providers (Dagger Hilt Modules)
│   └── AppModule.kt       # App-scoped dependency binding & providers
├── data                   # Data Layer
│   └── repository         # Local repository implementations
│       └── PeerLinkRepositoryImpl.kt
├── domain                 # Domain Layer
│   ├── model              # Immutable data models
│   │   ├── FileMetadata.kt
│   │   └── LocalFile.kt
│   ├── repository         # abstract repositories contracts
│   │   └── PeerLinkRepository.kt
│   └── usecase            # Business actions & orchestrations
│       ├── GetTransferStatsUseCase.kt
│       ├── ResetTransferUseCase.kt
│       ├── StartReceivingUseCase.kt
│       └── StartSendingUseCase.kt
├── network                # Network Utility classes
│   └── NetworkUtils.kt    # Socket & IPv4 Resolution utilities
├── security               # Crypto & Cryptography Package
│   └── CryptoUtils.kt     # Elliptic Curve (ECDH), SHA-256, AES-256-GCM logic
├── chat                   # Thread-safe Realtime Chat Socket Pool
│   └── ChatManager.kt     # Chat messages, commands, and socket actions
├── transfer               # Streaming Data Layer
│   ├── FileReceiver.kt    # Parallel/Serial file block receiver + Decryption
│   ├── FileSender.kt      # Stream chunking, base-IV rotation + Encryption
│   └── TransferService.kt # Foreground Data-Sync Android Service
├── ui                     # Design System Theme Definitions
│   └── theme              # M3 styles, Color Palettes, Typography
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── util                   # Helper & Formatting classes
│   └── InviteCode.kt      # Base58-like secure coordinate packs
├── MainActivity.kt        # Entry-point ComponentActivity (Hilt Bound)
└── PeerLinkApplication.kt # Global Application context base with @HiltAndroidApp
```

## Modules & Dependencies

```kotlin
dependencies {
    // Android Jetpack & Compose M3 Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // Dagger Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Moshi JSON Serialization
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.kotlin.codegen)
}
```

## Core Feature Execution Flows

### 1. File Sending Connection Flow
```
[File Selection] -> [Start Listeners]  -> [Encode Invite Code] -> [Accept Socket] -> [ECDH Key Exchange] -> [Approve Handshake] -> [AES-GCM Chunk Stream]
```

### 2. File Receiving Connection Flow
```
[Input Invite Code] -> [Resolve Host Coordinates] -> [Connect Socket] -> [ECDH Key Exchange] -> [Render Handshake Dialog] -> [Approve/Reject] -> [Decrypt Blocks] -> [Store in Downloads]
```
