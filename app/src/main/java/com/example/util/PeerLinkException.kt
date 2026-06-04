package com.example.util

import java.io.FileNotFoundException
import java.io.IOException
import java.net.BindException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.io.EOFException
import java.security.GeneralSecurityException

sealed class PeerLinkException(
    val title: String,
    val description: String,
    val recoveryHint: String,
    cause: Throwable? = null
) : IOException("$title: $description", cause) {

    class PortBindingException(val port: Int, cause: Throwable? = null) : PeerLinkException(
        title = "Port Binding Failed",
        description = "Could not bind to local port $port.",
        recoveryHint = "Try disabling other host/transfer apps, toggle Airplane mode on and off, or restart your device.",
        cause = cause
    )

    class HandshakeTimeoutException(cause: Throwable? = null) : PeerLinkException(
        title = "Handshake Timeout",
        description = "Network linkage timed out during initial security verification.",
        recoveryHint = "Keep both devices near each other and verify they remain on the exact same Wi-Fi subnet.",
        cause = cause
    )

    class EncryptionHandshakeException(val detail: String, cause: Throwable? = null) : PeerLinkException(
        title = "Secure Handshake Failed",
        description = "Security key negotiation with the peer failed: $detail",
        recoveryHint = "Verification failed. Disconnect and recreate the transfer connection to renegotiate keys.",
        cause = cause
    )

    class PeerRejectedException(val isSelfReject: Boolean) : PeerLinkException(
        title = "Transfer Rejected",
        description = if (isSelfReject) "You declined the connection request." else "The remote peer declined the file transfer request.",
        recoveryHint = if (isSelfReject) "Restart the transfer and tap Authoize if this was an accidental rejection." else "Ensure the remote user verifies your security fingerprint and selects Authorize."
    )

    class ConnectionInterruptedException(cause: Throwable? = null) : PeerLinkException(
        title = "Connection Interrupted",
        description = "The file stream link was abruptly closed or timed out mid-transfer.",
        recoveryHint = "Keep the application in the foreground. Ensure no power-saving limiters disable your Wi-Fi.",
        cause = cause
    )

    class DiscoveryException(val action: String, cause: Throwable? = null) : PeerLinkException(
        title = "P2P Discovery Failure",
        description = "Automatic service discovery ($action) failed to initialize.",
        recoveryHint = "Toggle Wi-Fi. If using public Wi-Fi (which often blocks multicast), use a local mobile hotspot instead.",
        cause = cause
    )

    class FileAccessPermissionException(val fileName: String, cause: Throwable? = null) : PeerLinkException(
        title = "File Access Blocked",
        description = "Could not open, read, or write file '$fileName'.",
        recoveryHint = "Enable external storage access or select a file type that isn't restricted by your system settings.",
        cause = cause
    )

    class SocketConnectionException(val ip: String, val port: Int, cause: Throwable? = null) : PeerLinkException(
        title = "Failed to Connect",
        description = "Unable to establish network handshake with remote endpoint at $ip:$port.",
        recoveryHint = "Ensure the sender has started sending and the generated Invite Code matches correctly.",
        cause = cause
    )

    class UnknownTransferException(cause: Throwable? = null) : PeerLinkException(
        title = "Unexpected Transfer Error",
        description = cause?.localizedMessage ?: "An unknown system disruption took place during file streaming.",
        recoveryHint = "Please check peer logs and relaunch the transfer session.",
        cause = cause
    )

    companion object {
        fun fromThrowable(throwable: Throwable): PeerLinkException {
            if (throwable is PeerLinkException) return throwable

            return when (throwable) {
                is BindException -> PortBindingException(0, throwable)
                is ConnectException -> SocketConnectionException("Remote Peer", 0, throwable)
                is SocketTimeoutException -> HandshakeTimeoutException(throwable)
                is EOFException -> ConnectionInterruptedException(throwable)
                is SocketException -> {
                    val message = throwable.message?.lowercase() ?: ""
                    if (message.contains("reset") || message.contains("closed") || message.contains("broken pipe")) {
                        ConnectionInterruptedException(throwable)
                    } else {
                        UnknownTransferException(throwable)
                    }
                }
                is GeneralSecurityException -> EncryptionHandshakeException(throwable.localizedMessage ?: "Cryptographic error", throwable)
                is FileNotFoundException -> FileAccessPermissionException("Selected file", throwable)
                is SecurityException -> FileAccessPermissionException("Restricted file / storage", throwable)
                else -> UnknownTransferException(throwable)
            }
        }
    }
}
