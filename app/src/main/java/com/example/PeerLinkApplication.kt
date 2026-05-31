package com.example

import android.app.Application

class PeerLinkApplication : Application() {
    
    lateinit var appContainer: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
