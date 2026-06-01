package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FileItem(
    val fileName: String,
    val size: Long,
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class TransferMetadata(
    val files: List<FileItem>,
    val totalSize: Long,
    val transferId: String
)
