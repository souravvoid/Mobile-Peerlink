# PeerLink Deployment

## Keystore & Signatures
PeerLink employs Android standard V2 + V3 APK signing through standard build processes.

## Permissions & Manifest
Required local network scanning permissions fall under Android Sandbox best practices. See AndroidManifest for all declarations.

## App Store Submission
Review Android 14 Policy updates regarding Foreground Services:
- Explicit use of `FOREGROUND_SERVICE_TYPE_DATA_SYNC` must be declared.
- Nearby Wifi permissions require the never for location flag.
