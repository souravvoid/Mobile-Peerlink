package com.example.data.repository

import android.net.Uri
import com.example.domain.TransferManager
import com.example.domain.TransferStats
import com.example.domain.repository.PeerLinkRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerLinkRepositoryImpl @Inject constructor(
    private val transferManager: TransferManager
) : PeerLinkRepository {

    override val transferStats: StateFlow<TransferStats> = transferManager.stats

    override fun startSending(uris: List<Uri>, onApproval: suspend (String) -> Boolean, configurePort: (Int, Int) -> Unit) {
        transferManager.startSending(uris, onApproval, configurePort)
    }

    override fun startReceiving(ip: String, port: Int, onApproval: suspend (String) -> Boolean) {
        transferManager.startReceiving(ip, port, onApproval)
    }

    override fun reset() {
        transferManager.reset()
    }
}
