package com.example

import android.content.Context
import com.example.domain.TransferManager

class AppContainer(private val context: Context) {
    val transferManager by lazy { TransferManager(context.applicationContext) }
}
