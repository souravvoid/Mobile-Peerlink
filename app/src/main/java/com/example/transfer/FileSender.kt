package com.example.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.TransferStats
import com.example.security.CryptoUtils
import com.example.domain.model.FileItem
import com.example.domain.model.TransferMetadata
import com.example.util.PeerLinkException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.UUID

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

    suspend fun acceptAndSend(uris: List<Uri>, onApprovalRequested: suspend (String) -> Boolean) = withContext(Dispatchers.IO) {
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
            val aesKey = CryptoUtils.deriveAESKey(keyPair.private, peerPublicKey)
            
            val requestFiles = mutableListOf<FileItem>()
            var totalFilesSize = 0L

            for (uri in uris) {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var fileName = "unknown"
                var fileSize = 1L
                var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameCol = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        val sizeCol = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                        fileName = it.getString(displayNameCol)
                        fileSize = it.getLong(sizeCol)
                    }
                }
                requestFiles.add(FileItem(fileName, fileSize, mimeType))
                totalFilesSize += fileSize
            }
            
            val metadata = TransferMetadata(
                files = requestFiles,
                totalSize = totalFilesSize,
                transferId = UUID.randomUUID().toString()
            )

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(TransferMetadata::class.java)
            val metadataJson = adapter.toJson(metadata)
            
            _stats.value = _stats.value.copy(totalBytes = totalFilesSize, totalFiles = uris.size)

            val baseIv = ByteArray(CryptoUtils.GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
            outStream.write(baseIv)
            
            val metaBytes = metadataJson.toByteArray(Charsets.UTF_8)
            val encMeta = CryptoUtils.encrypt(metaBytes, aesKey, baseIv)
            outStream.writeInt(encMeta.size)
            outStream.write(encMeta)
            outStream.flush()
            
            _stats.value = _stats.value.copy(remoteFingerprint = fingerprint)

            val approved = onApprovalRequested(fingerprint)
            outStream.writeBoolean(approved)
            outStream.flush()
            
            val peerApproved = inStream.readBoolean()

            if (!approved || !peerApproved) {
                val rejectEx = PeerLinkException.PeerRejectedException(isSelfReject = !approved)
                _stats.value = _stats.value.copy(
                    error = rejectEx.description,
                    exception = rejectEx,
                    isComplete = true
                )
                return@withContext
            }

            _stats.value = _stats.value.copy(isWaitingForApproval = false)

            var globalTotalRead = 0L
            val startTime = System.currentTimeMillis()

            for ((index, uri) in uris.withIndex()) {
                val currentFile = requestFiles[index]
                _stats.value = _stats.value.copy(
                    currentFileName = currentFile.fileName,
                    currentFileIndex = index
                )
                
                context.contentResolver.openInputStream(uri)?.use { fileIn ->
                    val buffer = ByteArray(1024 * 1024 * 2)
                    var bytesRead: Int
                    var ivCounter = 1

                    while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                        val actualBytes = buffer.copyOf(bytesRead)
                        
                        val currentIv = baseIv.copyOf()
                        currentIv[11] = (currentIv[11] + ivCounter).toByte()
                        
                        val encryptedChunk = CryptoUtils.encrypt(actualBytes, aesKey, currentIv)
                        
                        outStream.writeInt(encryptedChunk.size)
                        outStream.write(encryptedChunk)
                        
                        globalTotalRead += bytesRead
                        ivCounter++

                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                        val speed = if (elapsedSec > 0) (globalTotalRead / 1024f / 1024f) / elapsedSec else 0f
                        
                        _stats.value = _stats.value.copy(
                            progress = globalTotalRead.toFloat() / totalFilesSize.toFloat(),
                            bytesTransferred = globalTotalRead,
                            speedMBps = speed
                        )
                    }
                }
                outStream.writeInt(-1) // End of this file
                outStream.flush()
            }
            outStream.writeInt(-2) // End of ALL files
            outStream.flush()
            _stats.value = _stats.value.copy(isComplete = true, progress = 1f)

        } catch (e: Exception) {
            e.printStackTrace()
            val mapped = PeerLinkException.fromThrowable(e)
            _stats.value = _stats.value.copy(
                error = mapped.description,
                exception = mapped,
                isComplete = true
            )
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

    fun reportException(e: Throwable) {
        val mapped = PeerLinkException.fromThrowable(e)
        _stats.value = _stats.value.copy(
            error = mapped.description,
            exception = mapped,
            isComplete = true
        )
    }
}
