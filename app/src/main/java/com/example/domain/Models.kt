package com.example.domain

data class TransferStats(
    val progress: Float = 0f,
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 1,
    val speedMBps: Float = 0f,
    val isComplete: Boolean = false,
    val error: String? = null,
    val isConnecting: Boolean = true,
    val isWaitingForApproval: Boolean = false,
    val currentFileName: String? = null,
    val remoteFingerprint: String? = null
)
