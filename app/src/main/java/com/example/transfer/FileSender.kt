package com.example.transfer

import android.content.Context
import android.net.Uri
import com.example.domain.TransferStats
import com.example.security.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom

class FileSender(private val context: Context) {
    
    private val _stats = MutableStateFlow(TransferStats())
    val stats = _stats.asStateFlow()

    private var serverSocket: ServerSocket? = null
    var listeningPort: Int = -1

    fun startListening(): Int {
        serverSocket = ServerSocket(0)
        listeningPort = serverSocket?.localPort ?: -1
        return listeningPort
    }

    suspend fun acceptAndSend(uri: Uri, onApprovalRequested: suspend (String) -> Boolean) = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            _stats.value = TransferStats(isConnecting = true)
            socket = serverSocket?.accept()
            _stats.value = _stats.value.copy(isConnecting = false, isWaitingForApproval = true)

            // ECDH Handshake
            val keyPair = CryptoUtils.generateKeyPair()
            val outStream = DataOutputStream(socket!!.getOutputStream())
            val inStream = DataInputStream(socket.getInputStream())

            val pubEncoded = keyPair.public.encoded
            outStream.writeInt(pubEncoded.size)
            outStream.write(pubEncoded)
            outStream.flush()

            val peerKeyLen = inStream.readInt()
            val peerKeyBytes = ByteArray(peerKeyLen)
            inStream.readFully(peerKeyBytes)

            val peerPublicKey = CryptoUtils.decodePublicKey(peerKeyBytes)
            val fingerprint = CryptoUtils.generateFingerprint(peerKeyBytes)
            
            _stats.value = _stats.value.copy(remoteFingerprint = fingerprint)

            val approved = onApprovalRequested(fingerprint)
            outStream.writeBoolean(approved)
            outStream.flush()
            
            val peerApproved = inStream.readBoolean()

            if (!approved || !peerApproved) {
                _stats.value = _stats.value.copy(error = "Transfer rejected", isComplete = true)
                return@withContext
            }

            _stats.value = _stats.value.copy(isWaitingForApproval = false)

            val aesKey = CryptoUtils.deriveAESKey(keyPair.private, peerPublicKey)
            
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var fileName = "unknown"
            var fileSize = 1L
            cursor?.use {
                if (it.moveToFirst()) {
                    fileName = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                    fileSize = it.getLong(it.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
                }
            }
            
            _stats.value = _stats.value.copy(currentFileName = fileName, totalBytes = fileSize)

            val baseIv = ByteArray(CryptoUtils.GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
            outStream.write(baseIv)
            
            val metaBytes = fileName.toByteArray(Charsets.UTF_8)
            val encMeta = CryptoUtils.encrypt(metaBytes, aesKey, baseIv)
            outStream.writeLong(fileSize)
            outStream.writeInt(encMeta.size)
            outStream.write(encMeta)
            outStream.flush()

            context.contentResolver.openInputStream(uri)?.use { fileIn ->
                val buffer = ByteArray(1024 * 1024 * 2)
                var bytesRead: Int
                var totalRead = 0L
                var ivCounter = 1

                val startTime = System.currentTimeMillis()

                while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                    val actualBytes = buffer.copyOf(bytesRead)
                    
                    val currentIv = baseIv.copyOf()
                    currentIv[11] = (currentIv[11] + ivCounter).toByte()
                    
                    val encryptedChunk = CryptoUtils.encrypt(actualBytes, aesKey, currentIv)
                    
                    outStream.writeInt(encryptedChunk.size)
                    outStream.write(encryptedChunk)
                    
                    totalRead += bytesRead
                    ivCounter++

                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                    val speed = if (elapsedSec > 0) (totalRead / 1024f / 1024f) / elapsedSec else 0f
                    
                    _stats.value = _stats.value.copy(
                        progress = totalRead.toFloat() / fileSize.toFloat(),
                        bytesTransferred = totalRead,
                        speedMBps = speed
                    )
                }
                outStream.writeInt(-1)
                outStream.flush()
                _stats.value = _stats.value.copy(isComplete = true, progress = 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _stats.value = _stats.value.copy(error = e.message, isComplete = true)
        } finally {
            socket?.close()
            serverSocket?.close()
        }
    }
    
    fun cancel() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
    }
}
