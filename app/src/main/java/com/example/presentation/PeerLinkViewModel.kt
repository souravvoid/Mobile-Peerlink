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
import com.example.domain.usecase.GetTransferStatsUseCase
import com.example.domain.usecase.StartSendingUseCase
import com.example.domain.usecase.StartReceivingUseCase
import com.example.domain.usecase.ResetTransferUseCase

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.Context
import com.example.transfer.FileSender
import com.example.transfer.FileReceiver

@HiltViewModel
class PeerLinkViewModel @Inject constructor(
    private val transferManager: TransferManager, // Keep for chat features
    private val startSendingUseCase: StartSendingUseCase,
    private val startReceivingUseCase: StartReceivingUseCase,
    private val getTransferStatsUseCase: GetTransferStatsUseCase,
    private val resetTransferUseCase: ResetTransferUseCase
) : ViewModel() {

    init {
        transferManager.chatManager.onFileOfferReceived = { fileName, size, port ->
            val chatIp = transferManager.chatManager.peerIp
            if (chatIp.isNotEmpty()) {
                startReceivingUseCase(chatIp, port, onApproval = { true })
            }
        }
    }

    fun sendFileViaChat(context: Context, uris: List<Uri>) {
        val sender = FileSender(context)
        viewModelScope.launch(Dispatchers.IO) {
            val port = sender.startListening()
            
            var fileName = "unknown"
            var fileSize = 1L
            // Use the first file for chat display fallback
            if (uris.isNotEmpty()) {
                val cursor = context.contentResolver.query(uris.first(), null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameIdx != -1) fileName = it.getString(nameIdx)
                        if (sizeIdx != -1) fileSize = it.getLong(sizeIdx)
                    }
                }
                if (uris.size > 1) {
                    fileName = "$fileName and ${uris.size - 1} more"
                }
            }
            
            transferManager.chatManager.sendFileOffer(fileName, fileSize, port)
            transferManager.monitorSender(sender)
            sender.acceptAndSend(uris, onApprovalRequested = { true })
        }
    }

    private val _selectedFiles = MutableStateFlow<List<Uri>>(emptyList())
    val selectedFiles = _selectedFiles.asStateFlow()

    val stats = getTransferStatsUseCase()
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

    fun addFiles(uris: List<Uri>) {
        _selectedFiles.value = (_selectedFiles.value + uris).distinct()
    }

    fun removeFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value - uri
    }

    fun clearFiles() {
        _selectedFiles.value = emptyList()
    }

    fun startSending() {
        if (_selectedFiles.value.isEmpty()) return
        resetTransferUseCase()
        startSendingUseCase(_selectedFiles.value, onApproval = { fingerprint ->
            _fingerprintToApprove.value = fingerprint
            val deferred = CompletableDeferred<Boolean>()
            approvalDeferred = deferred
            deferred.await()
        }, configurePort = { filePort, chatPort ->
            val ip = _ipAddress.value
            if (ip != "Unknown") {
                _inviteCode.value = com.example.util.InviteCode.encode(ip, filePort, chatPort)
            }
        })
    }

    fun startReceiving(code: String) {
        val details = com.example.util.InviteCode.decode(code)
        if (details != null) {
            resetTransferUseCase()
            if (details.third != -1) {
                transferManager.chatManager.connectAsClient(details.first, details.third)
            }
            startReceivingUseCase(details.first, details.second, onApproval = { fingerprint ->
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
           resetTransferUseCase()
        }
    }

    fun cancelTransfer() {
        resetTransferUseCase()
        _inviteCode.value = null
        _fingerprintToApprove.value = null
        approvalDeferred?.complete(false)
        approvalDeferred = null
    }
}
