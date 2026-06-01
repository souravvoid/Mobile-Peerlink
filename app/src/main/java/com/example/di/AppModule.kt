package com.example.di

import android.content.Context
import com.example.data.repository.PeerLinkRepositoryImpl
import com.example.domain.TransferManager
import com.example.domain.repository.PeerLinkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTransferManager(@ApplicationContext context: Context): TransferManager {
        return TransferManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPeerLinkRepository(
        peerLinkRepositoryImpl: PeerLinkRepositoryImpl
    ): PeerLinkRepository
}
