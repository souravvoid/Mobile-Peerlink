package com.example.domain

import com.example.domain.model.TransferMetadata

data class TransferStats(
    val progress: Float = 0f,
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 1,
    val speedMBps: Float = 0f,
    val isComplete: Boolean = false,
    val error: String? = null,
    val isConnecting: Boolean = false,
    val isWaitingForApproval: Boolean = false,
    val currentFileName: String? = null,
    val remoteFingerprint: String? = null,
    val metadata: TransferMetadata? = null,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0
)
