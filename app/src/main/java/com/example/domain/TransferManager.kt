package com.example.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import com.example.transfer.FileReceiver
import com.example.transfer.FileSender
import com.example.transfer.TransferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.chat.ChatManager

class TransferManager(private val context: Context) {
    private var transferService: TransferService? = null
    val chatManager = ChatManager()
    
    private val _stats = MutableStateFlow(TransferStats())
    val stats: StateFlow<TransferStats> = _stats.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            if (service == null) return
            val binder = service as TransferService.LocalBinder
            transferService = binder.getService()
        }
        override fun onServiceDisconnected(arg0: ComponentName?) {
            transferService = null
        }
    }

    init {
        Intent(context, TransferService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun startSending(uri: Uri, onApproval: suspend (String) -> Boolean, onPortReady: (Int, Int) -> Unit) {
        Intent(context, TransferService::class.java).also { intent ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        val sender = FileSender(context)
        transferService?.fileSender = sender
        
        CoroutineScope(Dispatchers.IO).launch {
            sender.stats.collect { 
                _stats.value = it
                transferService?.updateProgress(it.progress, it.speedMBps)
                if (it.isComplete) transferService?.stopTransfer()
            }
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val port = sender.startListening()
            chatManager.startHost { chatPort ->
                 onPortReady(port, chatPort)
            }
            sender.acceptAndSend(uri, onApproval)
        }
    }

    fun startReceiving(ip: String, port: Int, onApproval: suspend (String) -> Boolean) {
        Intent(context, TransferService::class.java).also { intent ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        val receiver = FileReceiver(context)
        transferService?.fileReceiver = receiver

        CoroutineScope(Dispatchers.IO).launch {
            receiver.stats.collect { 
                _stats.value = it
                transferService?.updateProgress(it.progress, it.speedMBps)
                if (it.isComplete) transferService?.stopTransfer()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            receiver.connectAndReceive(ip, port, onApproval)
        }
    }

    fun monitorSender(sender: FileSender) {
        transferService?.fileSender = sender
        CoroutineScope(Dispatchers.IO).launch {
            sender.stats.collect { 
                _stats.value = it
                transferService?.updateProgress(it.progress, it.speedMBps)
                if (it.isComplete) transferService?.stopTransfer()
            }
        }
    }

    fun reset() {
        transferService?.fileSender?.cancel()
        transferService?.fileReceiver?.cancel()
        transferService?.stopTransfer()
        _stats.value = TransferStats()
    }
}
