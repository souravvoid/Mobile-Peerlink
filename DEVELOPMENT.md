# Development Guide

## Local Environment
- PeerLink relies on Java 17 and AGP 8.x.
- Dagger Hilt is used for Dependency Injection (Ensure KSP is configured securely if upgrading Gradle versions).

## Core Technologies
- **Kotlin Coroutines**: All asynchronous logic (network sockets, file chunking) is built on `Dispatchers.IO`.
- **Jetpack Compose**: All screens are fully built using Compose. Unidirectional Data Flow is strictly mandated.

## Network Mocking
- Avoid instantiating raw standard `java.net.Socket` internally inside Views. Pass them down via Hilt from the Data Layer to keep the application modular.

## Handling File Streams
- When editing stream lengths, keep chunks exactly sized around 2MB for memory safety (`1024 * 1024 * 2`). Do not load entire files into memory. 
