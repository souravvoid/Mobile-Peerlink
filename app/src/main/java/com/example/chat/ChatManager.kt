package com.example.chat

import android.util.Log
import com.example.domain.TransferStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isMe: Boolean,
    val text: String = "",
    val isFileCommand: Boolean = false,
    val fileName: String = "",
    val fileSize: Long = 0,
    val filePort: Int = 0,
    val time: Long = System.currentTimeMillis()
)

class ChatManager {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var outStream: PrintWriter? = null
    private var running = false

    var peerIp: String = ""

    // Callbacks for file operations
    var onFileOfferReceived: ((fileName: String, size: Long, port: Int) -> Unit)? = null

    var listeningPort: Int = -1

    fun startHost(onPortReady: (Int) -> Unit) {
        Thread {
            try {
                serverSocket = ServerSocket(0)
                listeningPort = serverSocket!!.localPort
                onPortReady(listeningPort)
                
                Log.d("ChatManager", "Waiting for peer on port $listeningPort")
                socket = serverSocket!!.accept()
                peerIp = socket!!.inetAddress.hostAddress ?: ""
                Log.d("ChatManager", "Peer joined from $peerIp")
                
                _isConnected.value = true
                running = true
                setupStreams()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun connectAsClient(ip: String, port: Int) {
        Thread {
            try {
                peerIp = ip
                Log.d("ChatManager", "Connecting to $ip:$port")
                socket = Socket(ip, port)
                _isConnected.value = true
                running = true
                setupStreams()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun setupStreams() {
        try {
            socket?.let { s ->
                outStream = PrintWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8), true)
                val inStream = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                
                while (running) {
                    val line = inStream.readLine()
                    if (line != null) {
                        handleIncomingMessage(line)
                    } else {
                        running = false
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isConnected.value = false
            running = false
        }
    }

    private fun handleIncomingMessage(line: String) {
        try {
            val json = JSONObject(line)
            val type = json.getString("type")
            if (type == "CHAT") {
                val text = json.getString("text")
                addMessage(ChatMessage(isMe = false, text = text))
            } else if (type == "FILE_OFFER") {
                val fileName = json.getString("fileName")
                val fileSize = json.getLong("fileSize")
                val filePort = json.getInt("port")
                addMessage(ChatMessage(isMe = false, isFileCommand = true, fileName = fileName, fileSize = fileSize, filePort = filePort))
                onFileOfferReceived?.invoke(fileName, fileSize, filePort)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendTextMessage(text: String) {
        if (!running) return
        val json = JSONObject().apply {
            put("type", "CHAT")
            put("text", text)
        }
        Thread {
            outStream?.println(json.toString())
        }.start()
        addMessage(ChatMessage(isMe = true, text = text))
    }

    fun sendFileOffer(fileName: String, fileSize: Long, port: Int) {
        if (!running) return
        val json = JSONObject().apply {
            put("type", "FILE_OFFER")
            put("fileName", fileName)
            put("fileSize", fileSize)
            put("port", port)
        }
        Thread {
            outStream?.println(json.toString())
        }.start()
        addMessage(ChatMessage(isMe = true, isFileCommand = true, fileName = fileName, fileSize = fileSize, filePort = port))
    }

    private fun addMessage(msg: ChatMessage) {
        _messages.value = _messages.value + msg
    }

    fun reset() {
        running = false
        _messages.value = emptyList()
        _isConnected.value = false
        try { socket?.close() } catch (e: Exception) {}
        try { serverSocket?.close() } catch (e: Exception) {}
    }
}
