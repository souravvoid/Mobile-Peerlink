package com.example.transfer

import android.content.Context
import android.os.Environment
import com.example.domain.TransferStats
import com.example.security.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket

class FileReceiver(private val context: Context) {
    
    private val _stats = MutableStateFlow(TransferStats())
    val stats = _stats.asStateFlow()

    private var socket: Socket? = null

    suspend fun connectAndReceive(ip: String, port: Int, onApprovalRequested: suspend (String) -> Boolean) = withContext(Dispatchers.IO) {
        try {
            _stats.value = TransferStats(isConnecting = true)
            socket = Socket(ip, port)
            _stats.value = _stats.value.copy(isConnecting = false, isWaitingForApproval = true)

            val outStream = DataOutputStream(socket!!.getOutputStream())
            val inStream = DataInputStream(socket!!.getInputStream())

            val keyPair = CryptoUtils.generateKeyPair()
            
            val peerKeyLen = inStream.readInt()
            val peerKeyBytes = ByteArray(peerKeyLen)
            inStream.readFully(peerKeyBytes)
            val peerPublicKey = CryptoUtils.decodePublicKey(peerKeyBytes)
            
            val pubEncoded = keyPair.public.encoded
            outStream.writeInt(pubEncoded.size)
            outStream.write(pubEncoded)
            outStream.flush()

            val fingerprint = CryptoUtils.generateFingerprint(peerKeyBytes)
            _stats.value = _stats.value.copy(remoteFingerprint = fingerprint)

            val peerApproved = inStream.readBoolean()
            val approved = onApprovalRequested(fingerprint)
            outStream.writeBoolean(approved)
            outStream.flush()

            if (!approved || !peerApproved) {
                _stats.value = _stats.value.copy(error = "Transfer rejected", isComplete = true)
                return@withContext
            }
            
            _stats.value = _stats.value.copy(isWaitingForApproval = false)

            val aesKey = CryptoUtils.deriveAESKey(keyPair.private, peerPublicKey)

            val baseIv = ByteArray(CryptoUtils.GCM_IV_LENGTH)
            inStream.readFully(baseIv)
            
            val fileSize = inStream.readLong()
            val encMetaLen = inStream.readInt()
            val encMeta = ByteArray(encMetaLen)
            inStream.readFully(encMeta)
            
            val metaBytes = CryptoUtils.decrypt(encMeta, aesKey, baseIv)
            val rawFileName = String(metaBytes, Charsets.UTF_8)
            
            val safeFileName = File(rawFileName).name
            _stats.value = _stats.value.copy(currentFileName = safeFileName, totalBytes = fileSize)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val peerLinkDir = File(downloadsDir, "PeerLink").apply { mkdirs() }
            val outputFile = File(peerLinkDir, safeFileName)

            FileOutputStream(outputFile).use { fileOut ->
                var totalWritten = 0L
                var ivCounter = 1
                val startTime = System.currentTimeMillis()

                while (true) {
                    val chunkLen = inStream.readInt()
                    if (chunkLen == -1) break

                    val encChunk = ByteArray(chunkLen)
                    inStream.readFully(encChunk)

                    val currentIv = baseIv.copyOf()
                    currentIv[11] = (currentIv[11] + ivCounter).toByte()

                    val plainChunk = CryptoUtils.decrypt(encChunk, aesKey, currentIv)
                    fileOut.write(plainChunk)
                    
                    totalWritten += plainChunk.size
                    ivCounter++
                    
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                    val speed = if (elapsedSec > 0) (totalWritten / 1024f / 1024f) / elapsedSec else 0f
                    
                    _stats.value = _stats.value.copy(
                        progress = totalWritten.toFloat() / fileSize.toFloat(),
                        bytesTransferred = totalWritten,
                        speedMBps = speed
                    )
                }
            }
            
            _stats.value = _stats.value.copy(isComplete = true, progress = 1f)

        } catch (e: Exception) {
            e.printStackTrace()
            _stats.value = _stats.value.copy(error = e.message, isComplete = true)
        } finally {
            socket?.close()
        }
    }
    
    fun cancel() {
        try {
            socket?.close()
        } catch (e: Exception) {}
    }
}
