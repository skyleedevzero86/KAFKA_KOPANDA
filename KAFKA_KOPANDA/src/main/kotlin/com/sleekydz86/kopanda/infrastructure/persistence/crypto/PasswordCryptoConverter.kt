package com.sleekydz86.kopanda.infrastructure.persistence.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class PasswordCryptoConverter : AttributeConverter<String?, String?> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return crypto().encrypt(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return crypto().decrypt(dbData)
    }

    companion object {
        @Volatile
        private var installed: ConnectionPasswordCrypto? = null

        fun install(crypto: ConnectionPasswordCrypto) {
            installed = crypto
        }

        private fun crypto(): ConnectionPasswordCrypto {
            return installed ?: throw IllegalStateException("비밀번호 암호화 키가 초기화되지 않았습니다")
        }
    }
}
