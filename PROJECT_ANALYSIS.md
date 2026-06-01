# Project Analysis: PeerLink Android

## 1. Architecture Overview
PeerLink Android adopts a modern Android structure spanning MVVM and Clean Architecture paradigms:
- **Presentation Layer**: Built with Jetpack Compose, managing dynamic layouts securely and cleanly mapping state flows.
- **Domain Layer**: Houses transfer orchestration (`TransferManager`) and Core Networking (`Models`). Coroutines serve as the async synchronization engine.
- **Data/Network Layer**: Uses native Java `Socket` mapping inside standard Coroutine channels. Overcomes the constraints of direct physical networking endpoints.
- **Security Layer**: Self-contained `CryptoUtils` applying ECDH + AES-GCM without exposing key derivations globally.

## 2. Dependency Graph
- **Jetpack Compose**: Handles the View framework natively.
- **Hilt / Dagger**: Scopes the Dependency space mapping ViewModels.
- **Kotlin SDK & Coroutines**: Handles standard multithreading via Flows.
- **No Complex Third-Party Networking Libraries**: Ensures that local offline sockets remain completely independent of external dependencies like Retrofit (which is unnecessary for this feature set).
- **JSON handling API**: Integrated for Chat capabilities sharing structured messages synchronously alongside heavy payload transfers.

## 3. Module Graph
- Entirely mapped inside the `:app` monolith structure. Given the singular domain context (offline local P2P transfers), modularizing network vs domain vs presentation layers into separate Gradle modules would be overkill, but the internal package scoping (`com.example.chat`, `com.example.transfer`, `com.example.security`) achieves identical encapsulation.

## 4. Potential Risks
- Direct raw socket bindings might fail due to strict firewall rules on disparate Android OS manufacturer forks.
- Thread control loops interacting directly with UI states via Dispatchers could result in back-pressure if a massive chunked payload drops mid-stream.

## 5. Missing Components
- Persistent History tracking via standard structured database formats (e.g. Room).
- Active scanning mapping across MDNS / NSD (Network Service Discovery) rather than manual IP entry + port codes.
