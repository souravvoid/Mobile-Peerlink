package com.example.domain.repository

import android.net.Uri
import com.example.domain.TransferStats
import kotlinx.coroutines.flow.StateFlow

interface TransferRepository {
    val statsFlow: StateFlow<TransferStats>
    fun startListening(port: Int, onApproved: suspend (remoteFingerprint: String) -> Boolean)
    fun startConnecting(ip: String, port: Int, onApproved: suspend (remoteFingerprint: String) -> Boolean)
    suspend fun sendFile(uri: Uri, fileName: String, fileSize: Long)
    suspend fun receiveFile(saveDirectoryUri: Uri)
    fun cancelTransfer()
}
