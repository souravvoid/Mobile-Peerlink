package com.example.domain.usecase

import android.net.Uri
import com.example.domain.repository.PeerLinkRepository
import javax.inject.Inject

class StartSendingUseCase @Inject constructor(
    private val repository: PeerLinkRepository
) {
    operator fun invoke(uris: List<Uri>, onApproval: suspend (String) -> Boolean, configurePort: (Int, Int) -> Unit) {
        repository.startSending(uris, onApproval, configurePort)
    }
}
