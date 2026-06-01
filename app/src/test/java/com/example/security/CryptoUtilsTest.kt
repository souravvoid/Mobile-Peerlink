package com.example.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.security.SecureRandom

class CryptoUtilsTest {

    @Test
    fun testKeyPairGeneration() {
        val keyPair = CryptoUtils.generateKeyPair()
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }

    @Test
    fun testAESKeyDerivation() {
        val alice = CryptoUtils.generateKeyPair()
        val bob = CryptoUtils.generateKeyPair()

        val aliceShared = CryptoUtils.deriveAESKey(alice.private, bob.public)
        val bobShared = CryptoUtils.deriveAESKey(bob.private, alice.public)

        assertArrayEquals(aliceShared.encoded, bobShared.encoded)
    }

    @Test
    fun testEncryptionAndDecryption() {
        val keyPair1 = CryptoUtils.generateKeyPair()
        val keyPair2 = CryptoUtils.generateKeyPair()
        val aesKey = CryptoUtils.deriveAESKey(keyPair1.private, keyPair2.public)

        val plaintext = "Hello PeerLink Secure Protocol".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(CryptoUtils.GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }

        val ciphertext = CryptoUtils.encrypt(plaintext, aesKey, iv)
        assertNotEquals(plaintext, ciphertext)

        val decrypted = CryptoUtils.decrypt(ciphertext, aesKey, iv)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun testFingerprintGeneration() {
        val keyPair = CryptoUtils.generateKeyPair()
        val fingerprint = CryptoUtils.generateFingerprint(keyPair.public.encoded)
        
        assertNotNull(fingerprint)
        assertEquals(8, fingerprint.length)
        assertEquals(fingerprint, fingerprint.uppercase())
    }
}
