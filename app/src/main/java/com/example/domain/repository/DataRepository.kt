package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

interface DataRepository {
    suspend fun saveTransferHistory(
        fileName: String,
        sizeBytes: Long,
        isSender: Boolean,
        success: Boolean,
        speedMBps: Float
    )
    fun getTransferHistory(): Flow<List<TransferHistoryEntity>>
    suspend fun clearHistory()
}

data class TransferHistoryEntity(
    val id: Long = 0,
    val fileName: String,
    val sizeBytes: Long,
    val isSender: Boolean,
    val success: Boolean,
    val speedMBps: Float,
    val timestamp: Long
)
