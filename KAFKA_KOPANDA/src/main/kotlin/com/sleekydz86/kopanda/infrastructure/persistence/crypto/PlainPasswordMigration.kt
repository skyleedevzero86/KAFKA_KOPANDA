package com.sleekydz86.kopanda.infrastructure.persistence.crypto

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PlainPasswordMigration(
    private val jdbcTemplate: JdbcTemplate,
    private val crypto: ConnectionPasswordCrypto
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(PlainPasswordMigration::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val changed = encryptStoredPlaintext()
        logger.info("평문으로 저장된 연결 비밀번호 {}건을 암호화했습니다", changed)
    }

    fun encryptStoredPlaintext(): Int {
        val rows = jdbcTemplate.query(
            """
            SELECT connection_id, password
            FROM connections
            WHERE password IS NOT NULL
              AND password NOT LIKE ?
            """.trimIndent(),
            { rs, _ -> rs.getString("connection_id") to rs.getString("password") },
            "${ConnectionPasswordCrypto.PREFIX}%"
        )
        rows.forEach { (id, plain) ->
            jdbcTemplate.update(
                "UPDATE connections SET password = ? WHERE connection_id = ?",
                crypto.encrypt(plain),
                id
            )
        }
        return rows.size
    }
}
