package com.example.domain.repository

import android.net.Uri
import com.example.domain.TransferStats
import kotlinx.coroutines.flow.StateFlow

interface PeerLinkRepository {
    val transferStats: StateFlow<TransferStats>
    fun startSending(uri: Uri, onApproval: suspend (String) -> Boolean, configurePort: (Int, Int) -> Unit)
    fun startReceiving(ip: String, port: Int, onApproval: suspend (String) -> Boolean)
    fun reset()
}
