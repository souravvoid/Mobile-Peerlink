package com.example.presentation

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.TransferManager
import com.example.network.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import com.example.domain.usecase.GetTransferStatsUseCase
import com.example.domain.usecase.StartSendingUseCase
import com.example.domain.usecase.StartReceivingUseCase
import com.example.domain.usecase.ResetTransferUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.transfer.FileSender
import com.example.transfer.FileReceiver

data class HistoryItem(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val direction: String, // "SEND" or "RECEIVE"
    val timestamp: Long,
    val isSuccess: Boolean,
    val error: String? = null,
    val peerName: String
)

@HiltViewModel
class PeerLinkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferManager: TransferManager, // Keep for chat features
    private val startSendingUseCase: StartSendingUseCase,
    private val startReceivingUseCase: StartReceivingUseCase,
    private val getTransferStatsUseCase: GetTransferStatsUseCase,
    private val resetTransferUseCase: ResetTransferUseCase
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("peerlink_prefs", Context.MODE_PRIVATE)

    private val _deviceName = MutableStateFlow(sharedPrefs.getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL)
    val deviceName = _deviceName.asStateFlow()

    private val _visibilityEnabled = MutableStateFlow(sharedPrefs.getBoolean("visibility_enabled", true))
    val visibilityEnabled = _visibilityEnabled.asStateFlow()

    private val _autoAcceptEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_accept_enabled", false))
    val autoAcceptEnabled = _autoAcceptEnabled.asStateFlow()

    private val _saveLocation = MutableStateFlow(sharedPrefs.getString("save_location", "Downloads/PeerLink") ?: "Downloads/PeerLink")
    val saveLocation = _saveLocation.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "Dark Mode") ?: "Dark Mode")
    val themeMode = _themeMode.asStateFlow()

    private val _transferHistory = MutableStateFlow<List<HistoryItem>>(emptyList())
    val transferHistory = _transferHistory.asStateFlow()

    private var lastLoggedTransferId: String? = null

    // Relocated properties first to ensure initialization safety
    private val _selectedFiles = MutableStateFlow<List<com.example.domain.model.LocalFile>>(emptyList())
    val selectedFiles = _selectedFiles.asStateFlow()

    val stats = getTransferStatsUseCase()
    val chatMessages = transferManager.chatManager.messages
    val isChatConnected = transferManager.chatManager.isConnected

    val discoveredPeers = transferManager.nsdHelper.discoveredPeers

    private val _ipAddress = MutableStateFlow(NetworkUtils.getLocalIpv4Address() ?: "Unknown")
    val ipAddress = _ipAddress.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode = _inviteCode.asStateFlow()

    private var approvalDeferred: CompletableDeferred<Boolean>? = null

    private val _fingerprintToApprove = MutableStateFlow<String?>(null)
    val fingerprintToApprove = _fingerprintToApprove.asStateFlow()

    private val _activeException = MutableStateFlow<com.example.util.PeerLinkException?>(null)
    val activeException = _activeException.asStateFlow()

    fun clearActiveException() {
        _activeException.value = null
        transferManager.nsdHelper.clearDiscoveryException()
    }

    init {
        _transferHistory.value = loadHistory()

        transferManager.chatManager.onFileOfferReceived = { fileName, size, port ->
            val chatIp = transferManager.chatManager.peerIp
            val isAutoAccept = _autoAcceptEnabled.value
            if (chatIp.isNotEmpty()) {
                startReceivingUseCase(chatIp, port, onApproval = { isAutoAccept })
            }
        }

        viewModelScope.launch {
            transferManager.nsdHelper.discoveryException.collect { nsdEx ->
                if (nsdEx != null) {
                    _activeException.value = nsdEx
                }
            }
        }

        viewModelScope.launch {
            stats.collect { currentStats ->
                val transferId = currentStats.metadata?.transferId
                val fileName = currentStats.currentFileName ?: currentStats.metadata?.files?.firstOrNull()?.fileName ?: "File Transfer"
                val totalSize = currentStats.metadata?.totalSize ?: currentStats.totalBytes
                
                if (currentStats.exception != null) {
                    _activeException.value = currentStats.exception
                }

                if (transferId != null && transferId != lastLoggedTransferId) {
                    if (currentStats.isComplete) {
                        lastLoggedTransferId = transferId
                        recordTransferHistory(
                            fileName = fileName,
                            fileSize = totalSize,
                            direction = if (_selectedFiles.value.isNotEmpty()) "SEND" else "RECEIVE",
                            isSuccess = true,
                            error = null,
                            peerName = "Peer (${currentStats.remoteFingerprint?.take(6) ?: "Unknown"})"
                        )
                    } else if (currentStats.error != null) {
                        lastLoggedTransferId = transferId
                        recordTransferHistory(
                            fileName = fileName,
                            fileSize = totalSize,
                            direction = if (_selectedFiles.value.isNotEmpty()) "SEND" else "RECEIVE",
                            isSuccess = false,
                            error = currentStats.error,
                            peerName = "Peer"
                        )
                    }
                }
            }
        }
    }

    private fun saveHistory(list: List<HistoryItem>) {
        val serialized = list.joinToString("\n") { item ->
            val cleanError = item.error?.replace("|", " ") ?: ""
            "${item.id}|${item.fileName}|${item.fileSize}|${item.direction}|${item.timestamp}|${item.isSuccess}|${cleanError}|${item.peerName}"
        }
        sharedPrefs.edit().putString("transfer_history_v2", serialized).apply()
    }

    private fun loadHistory(): List<HistoryItem> {
        val serialized = sharedPrefs.getString("transfer_history_v2", null) ?: return emptyList()
        return serialized.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 8) {
                HistoryItem(
                    id = parts[0],
                    fileName = parts[1],
                    fileSize = parts[2].toLongOrNull() ?: 0L,
                    direction = parts[3],
                    timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
                    isSuccess = parts[5].toBoolean(),
                    error = parts[6].ifEmpty { null },
                    peerName = parts[7]
                )
            } else null
        }
    }

    private fun recordTransferHistory(
        fileName: String,
        fileSize: Long,
        direction: String,
        isSuccess: Boolean,
        error: String?,
        peerName: String
    ) {
        val newItem = HistoryItem(
            id = java.util.UUID.randomUUID().toString(),
            fileName = fileName,
            fileSize = fileSize,
            direction = direction,
            timestamp = System.currentTimeMillis(),
            isSuccess = isSuccess,
            error = error,
            peerName = peerName
        )
        val updated = listOf(newItem) + _transferHistory.value
        _transferHistory.value = updated
        saveHistory(updated)
    }

    fun clearHistory() {
        _transferHistory.value = emptyList()
        sharedPrefs.edit().remove("transfer_history_v2").apply()
    }

    fun updateDeviceName(name: String) {
        _deviceName.value = name
        sharedPrefs.edit().putString("device_name", name).apply()
    }

    fun updateVisibility(enabled: Boolean) {
        _visibilityEnabled.value = enabled
        sharedPrefs.edit().putBoolean("visibility_enabled", enabled).apply()
    }

    fun updateAutoAccept(enabled: Boolean) {
        _autoAcceptEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_accept_enabled", enabled).apply()
    }

    fun updateSaveLocation(path: String) {
        _saveLocation.value = path
        sharedPrefs.edit().putString("save_location", path).apply()
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
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


    fun startDiscovery() {
        transferManager.nsdHelper.startDiscovery()
    }

    fun stopDiscovery() {
        transferManager.nsdHelper.stopDiscovery()
    }

    fun connectToDiscoveredPeer(peer: com.example.network.DiscoveredPeer) {
        resetTransferUseCase()
        if (peer.chatPort != -1) {
            transferManager.chatManager.connectAsClient(peer.ip, peer.chatPort)
        }
        startReceivingUseCase(peer.ip, peer.filePort, onApproval = { fingerprint ->
            _fingerprintToApprove.value = fingerprint
            val deferred = CompletableDeferred<Boolean>()
            approvalDeferred = deferred
            deferred.await()
        })
    }

    fun sendChatMessage(text: String) {
        transferManager.chatManager.sendTextMessage(text)
    }


    fun addFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val validFiles = uris.mapNotNull { uri ->
                var name = "Unknown file"
                var size = 0L
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (nameIdx != -1) name = cursor.getString(nameIdx)
                            if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                        }
                    }
                    com.example.domain.model.LocalFile(uri, name, size)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            _selectedFiles.value = (_selectedFiles.value + validFiles).distinctBy { it.uri }
        }
    }

    fun removeFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value.filter { it.uri != uri }
    }

    fun clearFiles() {
        _selectedFiles.value = emptyList()
    }

    fun startSending() {
        if (_selectedFiles.value.isEmpty()) return
        resetTransferUseCase()
        val uris = _selectedFiles.value.map { it.uri }
        startSendingUseCase(uris, onApproval = { fingerprint ->
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
