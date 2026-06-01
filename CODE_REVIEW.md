# Code Review: PeerLink Android

## 1. Domain Layer (`TransferManager.kt`, `Models.kt`)
- **Code Smells**: Dense service binding and domain orchestration merged inside `TransferManager`.
- **Misuse of Coroutines**: Coroutine dispatchers should be injected rather than statically declaring `CoroutineScope(Dispatchers.IO).launch`. This hinders unit testing where `TestCoroutineDispatcher` is preferred.
- **Exception Handling**: Global exception catches inside coroutines without localized granularity.

## 2. Cryptographic Security Layer (`CryptoUtils.kt`)
- **Code Cleanliness**: Functions cleanly mapped. AES-GCM tags and parameters are hardcoded properly.
- **Improvements**: It relies directly on `java.security.SecureRandom()`. The `Cipher` blocks should be wrapped in `try-finally` or structured concurrency error handling.

## 3. Presentation Layer (`Navigation.kt`, `PeerLinkViewModel.kt`)
- **Compose Recomposition**: Some dynamic lambdas mapped directly to `onClick` blocks could lead to unnecessary recompositions if view hierarchies scale up.
- **State Spillage**: Several `MutableStateFlow` are well protected behind `asStateFlow()`, conforming to strict read-only parameters from the UI tier.
- **Smell**: Heavy conditional business logic in Jetpack Compose screen components. Ideally, UI screens restrict themselves purely to emitting intents and observing state.

## 4. Hardware/Network Interaction (`FileSender.kt`, `ChatManager.kt`)
- **Memory Leaks**: `Socket` objects correctly shut down inside `finally` blocks, but thread creation (`Thread { }`) is deployed organically without Executor lifecycle control.
- **Lifecycle Hazards**: Explicit thread launching without coupling to a `ViewModel` or `Service` scope allows thread-leakage upon abrupt activity destructions.

## 5. Maintenance Action Items
1. Refactor `Thread { }` in `ChatManager.kt` into `viewModelScope.launch(Dispatchers.IO)`.
2. Convert static dispatcher references (`Dispatchers.IO`) in `TransferManager.kt` to generic variables supplied via Hilt or custom constructor.
3. Migrate pure socket interactions to explicit flow streams.
