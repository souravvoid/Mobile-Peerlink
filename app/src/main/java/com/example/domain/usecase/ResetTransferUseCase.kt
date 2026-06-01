package com.example.domain.usecase

import com.example.domain.repository.PeerLinkRepository
import javax.inject.Inject

class ResetTransferUseCase @Inject constructor(
    private val repository: PeerLinkRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
