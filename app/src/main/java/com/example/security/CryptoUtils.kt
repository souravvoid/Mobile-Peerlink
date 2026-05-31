package com.example.security

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    const val GCM_TAG_LENGTH = 128
    const val GCM_IV_LENGTH = 12

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }

    fun decodePublicKey(encoded: ByteArray): PublicKey {
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(X509EncodedKeySpec(encoded))
    }

    fun deriveAESKey(privateKey: java.security.PrivateKey, publicKey: PublicKey): SecretKey {
        val keyAgree = KeyAgreement.getInstance("ECDH")
        keyAgree.init(privateKey)
        keyAgree.doPhase(publicKey, true)
        val sharedSecret = keyAgree.generateSecret()
        
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sharedSecret)
        return SecretKeySpec(hash, "AES")
    }

    fun generateFingerprint(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKeyBytes)
        return hash.joinToString("") { "%02x".format(it) }.take(8).uppercase()
    }

    fun encrypt(plainText: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        return cipher.doFinal(plainText)
    }

    fun decrypt(cipherText: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(cipherText)
    }
}
