# Project Understanding

## Project Purpose
PeerLink is a fast, offline file-sharing Android application that utilizes a local network or hotspot configuration. It's designed to seamlessly transfer data securely via Socket transmission with built-in End-to-End Encryption, without requiring an intermediate backend or cloud server.

## Architecture
- **Architecture Pattern:** MVVM (Model-View-ViewModel) + Clean Architecture concepts.
- **Layers:** 
  - **Domain:** Standalone business rules (`TransferManager`, use cases, crypto).
  - **Data:** Implements pure logic (TCP Sockets, Wi-Fi Direct).
  - **Presentation:** UI in Jetpack Compose tightly bound to ViewModel states.
- **Dependency Injection:** Dagger Hilt (recently re-introduced and configured properly for tests and runtime compilation with KSP).

## Features
- Client-Server peer discovery
- Secure ECDH key exchange
- High-performance AES-GCM encrypted socket file transfer
- Clean Jetpack Compose modern Material 3 UI

## Dependencies
- Jetpack Compose (UI)
- Dagger Hilt (Dependency Injection)
- Kotlin Coroutines & Flow (Concurrency and states)

## Build Process
- Follows standard Android Gradle layout (`build.gradle.kts` files).
- JVM target 17 / 11 mixed (being migrated)
- Strict dependency on KSP for Dagger Hilt integration.

## Deployment Process
- Deployed via direct APK distribution locally, or uploaded to Google Play. 

## Known Limitations
- Background services occasionally crash on some edge devices without aggressive keep-alive constraints.
- Emulators without simulated multi-device Wi-Fi features can't easily reproduce E2E file transfers.
