# API and Network Contracts

## Socket Transfer Protocol
PeerLink eschews REST APIs for a raw TCP socket transfer pattern to eliminate overhead on the local network. 

### Packet 1 (ECDH Handshake)
- **Format**: Raw ByteArray
- **Payload**: User's ephemeral `.public` EC Key.

### Packet 2 (Metadata Transfer)
- **Format**: AES-GCM Encrypted JSON (`TransferMetadata` via Moshi).
- **Payload**:
```json
{
  "files": [
    { "fileName": "image.jpg", "fileSize": 1024000, "mimeType": "image/jpeg" }
  ],
  "totalSize": 1024000,
  "transferId": "UUID"
}
```

### Packet 3 (Chunked Bytes)
- **Format**: Continuous stream of encrypted fragments until `EOF` signal triggers termination.

## Service Ports
- Dynamic Ephemeral Port Allocation: Randomizer selects port `1024-65535`.
- Service Advertising: mDNS (Multicast DNS) advertising over UDP.
