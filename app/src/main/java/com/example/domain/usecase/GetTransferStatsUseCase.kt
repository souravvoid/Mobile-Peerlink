package com.example.domain.usecase

import com.example.domain.TransferStats
import com.example.domain.repository.PeerLinkRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetTransferStatsUseCase @Inject constructor(
    private val repository: PeerLinkRepository
) {
    operator fun invoke(): StateFlow<TransferStats> {
        return repository.transferStats
    }
}
