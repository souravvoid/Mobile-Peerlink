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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.Context
import com.example.transfer.FileSender
import com.example.transfer.FileReceiver

@HiltViewModel
class PeerLinkViewModel @Inject constructor(
    private val transferManager: TransferManager
) : ViewModel() {

    init {
        transferManager.chatManager.onFileOfferReceived = { fileName, size, port ->
            val chatIp = transferManager.chatManager.peerIp
            if (chatIp.isNotEmpty()) {
                transferManager.startReceiving(chatIp, port, onApproval = { true })
            }
        }
    }

    fun sendFileViaChat(context: Context, uri: Uri) {
        val sender = FileSender(context)
        viewModelScope.launch(Dispatchers.IO) {
            val port = sender.startListening()
            
            var fileName = "unknown"
            var fileSize = 1L
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx != -1) fileName = it.getString(nameIdx)
                    if (sizeIdx != -1) fileSize = it.getLong(sizeIdx)
                }
            }
            
            transferManager.chatManager.sendFileOffer(fileName, fileSize, port)
            transferManager.monitorSender(sender)
            sender.acceptAndSend(uri, onApprovalRequested = { true })
        }
    }

    val stats = transferManager.stats
    val chatMessages = transferManager.chatManager.messages
    val isChatConnected = transferManager.chatManager.isConnected

    fun sendChatMessage(text: String) {
        transferManager.chatManager.sendTextMessage(text)
    }

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
        }, onPortReady = { filePort, chatPort ->
            val ip = _ipAddress.value
            if (ip != "Unknown") {
                _inviteCode.value = com.example.util.InviteCode.encode(ip, filePort, chatPort)
            }
        })
    }

    fun startReceiving(code: String) {
        val details = com.example.util.InviteCode.decode(code)
        if (details != null) {
            transferManager.reset()
            if (details.third != -1) {
                transferManager.chatManager.connectAsClient(details.first, details.third)
            }
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
