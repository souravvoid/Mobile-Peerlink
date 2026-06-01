package com.example.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.domain.TransferManager
import com.example.network.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

@HiltViewModel
class PeerLinkViewModel @Inject constructor(
    private val transferManager: TransferManager
) : ViewModel() {

    val stats = transferManager.stats

    private val _ipAddress = MutableStateFlow(NetworkUtils.getLocalIpv4Address() ?: "Unknown")
    val ipAddress = _ipAddress.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode = _inviteCode.asStateFlow()

    private var approvalDeferred: CompletableDeferred<Boolean>? = null

    private val _fingerprintToApprove = MutableStateFlow<String?>(null)
    val fingerprintToApprove = _fingerprintToApprove.asStateFlow()

    fun startSending(uri: Uri) {
        transferManager.reset()
        transferManager.startSending(uri, onApproval = { fingerprint ->
            _fingerprintToApprove.value = fingerprint
            val deferred = CompletableDeferred<Boolean>()
            approvalDeferred = deferred
            deferred.await()
        }, onPortReady = { port ->
            val ip = _ipAddress.value
            if (ip != "Unknown") {
                _inviteCode.value = com.example.util.InviteCode.encode(ip, port)
            }
        })
    }

    fun startReceiving(code: String) {
        val details = com.example.util.InviteCode.decode(code)
        if (details != null) {
            transferManager.reset()
            transferManager.startReceiving(details.first, details.second, onApproval = { fingerprint ->
                _fingerprintToApprove.value = fingerprint
                val deferred = CompletableDeferred<Boolean>()
                approvalDeferred = deferred
                deferred.await()
            })
        }
    }

    fun answerApproval(approved: Boolean) {
        _fingerprintToApprove.value = null
        approvalDeferred?.complete(approved)
        approvalDeferred = null
        if (!approved) {
           transferManager.reset()
        }
    }

    fun cancelTransfer() {
        transferManager.reset()
        _inviteCode.value = null
        _fingerprintToApprove.value = null
        approvalDeferred?.complete(false)
        approvalDeferred = null
    }
}
