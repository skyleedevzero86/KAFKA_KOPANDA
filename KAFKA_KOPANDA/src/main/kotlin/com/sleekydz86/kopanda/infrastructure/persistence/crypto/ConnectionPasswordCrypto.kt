package com.sleekydz86.kopanda.infrastructure.persistence.crypto

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class ConnectionPasswordCrypto(
    @Value("\${kopanda.crypto.key:}") encodedKey: String
) {
    private val key: SecretKeySpec = decodeKey(encodedKey)
    private val random = SecureRandom()

    init {
        PasswordCryptoConverter.install(this)
    }

    fun requireStorable(password: String?) {
        if (password == null) {
            return
        }
        val byteLength = password.toByteArray(Charsets.UTF_8).size
        if (password.length > MAX_PASSWORD_CHARS || byteLength > MAX_PLAIN_BYTES) {
            throw PasswordTooLongException()
        }
    }

    fun encrypt(plain: String?): String? {
        if (plain == null) {
            return null
        }
        val iv = ByteArray(IV_BYTES)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(encrypted, 0, packed, iv.size, encrypted.size)
        val stored = PREFIX + Base64.getEncoder().encodeToString(packed)
        if (stored.length > COLUMN_LENGTH) {
            throw IllegalArgumentException("비밀번호가 저장 가능한 길이를 넘었습니다")
        }
        return stored
    }

    fun decrypt(stored: String?): String? {
        if (stored == null) {
            return null
        }
        if (!stored.startsWith(PREFIX)) {
            return stored
        }
        try {
            val packed = Base64.getDecoder().decode(stored.substring(PREFIX.length))
            if (packed.size < IV_BYTES + TAG_BYTES) {
                throw IllegalStateException("저장된 비밀번호를 복호화하지 못했습니다")
            }
            val iv = packed.copyOfRange(0, IV_BYTES)
            val cipherBytes = packed.copyOfRange(IV_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("저장된 비밀번호를 복호화하지 못했습니다", e)
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException("저장된 비밀번호를 복호화하지 못했습니다", e)
        }
    }

    private fun decodeKey(encodedKey: String): SecretKeySpec {
        val trimmed = encodedKey.trim()
        if (trimmed.isEmpty()) {
            throw IllegalStateException("KOPANDA_CRYPTO_KEY가 없습니다. Base64로 인코딩된 32바이트 키를 설정해야 합니다")
        }
        val raw = try {
            Base64.getDecoder().decode(trimmed)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("KOPANDA_CRYPTO_KEY는 Base64로 인코딩된 32바이트 키여야 합니다")
        }
        try {
            if (raw.size != KEY_BYTES) {
                throw IllegalStateException("KOPANDA_CRYPTO_KEY는 Base64로 인코딩된 32바이트 키여야 합니다")
            }
            return SecretKeySpec(raw, "AES")
        } finally {
            raw.fill(0)
        }
    }

    companion object {
        const val PREFIX = "enc:v1:"
        const val MAX_PASSWORD_CHARS = 128
        const val MAX_PLAIN_BYTES = 158
        const val COLUMN_LENGTH = 255
        private const val KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val TAG_BYTES = TAG_BITS / 8
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class PasswordTooLongException : IllegalArgumentException(
    "비밀번호는 ${ConnectionPasswordCrypto.MAX_PASSWORD_CHARS}자 또는 UTF-8 ${ConnectionPasswordCrypto.MAX_PLAIN_BYTES}바이트를 넘을 수 없습니다"
)
