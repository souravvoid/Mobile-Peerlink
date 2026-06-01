package com.example.domain.model

import android.net.Uri

data class LocalFile(
    val uri: Uri,
    val name: String,
    val size: Long
)
