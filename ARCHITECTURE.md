# PeerLink Architecture

## Core Principles
The app strictly follows **Clean Architecture** and **MVVM** principles adapted for Android. 
This structure ensures decoupling between the presentation framework (Jetpack Compose) and the low-level Networking/Crypto constraints.

### Modules:
1.  **`core`**: Contains base application classes, Hilt setup, Foreground Services, Background Process Managers.
2.  **`security`**: The complete vault. Handles ECDH key generation, cryptographic derivations (`SHA256(Shared Secret)`), and the AES-GCM ciphers. Completely agnostic to UI.
3.  **`network`**: Raw socket adapters, `NsdManager` wrappers for Multicast DNS. Defines the actual bytes-over-the-wire contracts.
4.  **`transfer`**: File reading/writing pipelines. Combines `security` stream ciphers and `network` TCP sockets into cohesive `Suspend` functions exposing progress via `StateFlow`s.
5.  **`domain`**: The business rules. Data models (`DevicePeer`, `TransferSession`), interfaces, UseCases (`StartTransferUseCase`).
6.  **`data`**: Repository concretions.
7.  **`presentation`**: Jetpack Compose screens, ViewModels injected via Hilt.
8.  **`util`**: Helpers (`InviteCode`, Context extensions, Formatters).

## Data Flow
*   Unidirectional Data Flow (UDF) via ViewModels.
*   Background transfers dispatch to ForegroundService, which holds singletons (via Hilt or manual instances binded to service). The UI connects to these active states via bounded Flows.

## Strengths
- **MVVM Integration**: Strong adoption of `ViewModel` with `StateFlow` ensures resilient UI states across configuration changes.
- **Dependency Injection**: Integrated DI via Hilt ensures decoupled object instantiation, easing testability.
- **Separation of Concerns**: Networking logic, cryptographic operations, and UI code are compartmentalized effectively.
- **Coroutines and Flow**: Utilizes lightweight concurrency mechanisms (Kotlin Coroutines).
- **UI Framework**: Modern declarative UI framework usage via Jetpack Compose.

## Weaknesses & Technical Debt
- **Error Handling**: Certain network operations emit generic errors or drop silent exceptions.
- **God Classes**: `TransferManager.kt` contains dense logic intersecting bounds of coordination and data formatting that could be separated into discrete components (e.g., TransferCoordinator vs SessionManager).
- **Socket Handling**: Blocking thread IO in some areas (e.g., chat socket loops) could be shifted to Dispatchers.IO channels to avoid thread starvation.

## Future Improvements
- Implement a robust Chunked File Transfer strategy allowing pause/resume operations.
- Adopt MVI (Model-View-Intent) in Compose architectures to yield explicitly tracked uni-directional state behaviors.
- Better encapsulation of TCP stream handling utilizing Kotlin Ktor or Netty to eliminate imperative socket boilerplate.
- Ensure strict lifecycle monitoring for the foreground service with generic fault tolerance parameters across foreground scopes.
