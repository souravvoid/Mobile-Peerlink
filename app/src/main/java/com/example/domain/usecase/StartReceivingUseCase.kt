package com.example.domain.usecase

import com.example.domain.repository.PeerLinkRepository
import javax.inject.Inject

class StartReceivingUseCase @Inject constructor(
    private val repository: PeerLinkRepository
) {
    operator fun invoke(ip: String, port: Int, onApproval: suspend (String) -> Boolean) {
        repository.startReceiving(ip, port, onApproval)
    }
}
