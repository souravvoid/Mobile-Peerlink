# Project Analysis
- Architecture: MVVM with Clean Architecture.
- Security: Custom ECDH and AES-GCM implemented.
- Network Layer: Uses ServerSocket and Socket for direct TCP transfer.
- Dependency Injection: Reverted from Hilt to Manual DI due to KSP cross-compatibility bugs with Gradle 8.x
- Build Scripts: Gradle Kotlin DSL used.

## Missing components
- No unit tests for UI.
- No end to end tests for File Sender/Receiver.
