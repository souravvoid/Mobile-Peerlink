package com.example.domain.repository

import java.security.KeyPair
import java.security.PublicKey
import javax.crypto.SecretKey

interface SecurityRepository {
    fun generateKeyPair(): KeyPair
    fun decodePublicKey(encoded: ByteArray): PublicKey
    fun deriveSharedSecret(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): SecretKey
    fun getFingerprint(publicKey: PublicKey): String
    fun encrypt(data: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray
    fun decrypt(data: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray
}
