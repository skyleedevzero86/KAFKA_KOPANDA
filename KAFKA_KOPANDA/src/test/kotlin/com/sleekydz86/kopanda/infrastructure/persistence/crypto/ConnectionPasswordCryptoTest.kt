package com.sleekydz86.kopanda.infrastructure.persistence.crypto

import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.Base64

class ConnectionPasswordCryptoTest {

    private val crypto = ConnectionPasswordCrypto(TEST_KEY)

    @Test
    fun `암호화한 값을 복호화하면 원문과 같다`() {
        // given
        val plain = "my-Secret-pw!"

        // when
        val stored = crypto.encrypt(plain)

        // then
        assertThat(stored).startsWith("enc:v1:")
        assertThat(stored).doesNotContain(plain)
        assertThat(crypto.decrypt(stored)).isEqualTo(plain)
    }

    @Test
    fun `같은 비밀번호도 암호문은 매번 다르다`() {
        // given
        val plain = "same-password"

        // when
        val first = crypto.encrypt(plain)
        val second = crypto.encrypt(plain)

        // then
        assertThat(first).isNotEqualTo(second)
        assertThat(crypto.decrypt(first)).isEqualTo(plain)
        assertThat(crypto.decrypt(second)).isEqualTo(plain)
    }

    @Test
    fun `암호문을 한 글자 바꾸면 복호화에 실패한다`() {
        // given
        val stored = crypto.encrypt("my-Secret-pw!")!!
        val index = ConnectionPasswordCrypto.PREFIX.length + 4
        val replacement = if (stored[index] == 'A') "B" else "A"
        val flipped = stored.replaceRange(index, index + 1, replacement)

        // when & then
        assertThatThrownBy { crypto.decrypt(flipped) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("저장된 비밀번호를 복호화하지 못했습니다")
    }

    @Test
    fun `enc v1 접두사가 없는 값은 그대로 읽힌다`() {
        // given
        val legacy = "my-Secret-pw!"

        // when
        val read = crypto.decrypt(legacy)

        // then
        assertThat(read).isEqualTo(legacy)
    }

    @Test
    fun `null 은 null 로 남는다`() {
        assertThat(crypto.encrypt(null)).isNull()
        assertThat(crypto.decrypt(null)).isNull()
    }

    @Test
    fun `128자 비밀번호 암호문은 컬럼 길이에 들어간다`() {
        // given
        val plain = "a".repeat(128)

        // when
        val stored = crypto.encrypt(plain)!!

        // then
        assertThat(stored.length).isLessThanOrEqualTo(ConnectionPasswordCrypto.COLUMN_LENGTH)
        assertThat(crypto.decrypt(stored)).isEqualTo(plain)
    }

    @Test
    fun `평문 158바이트는 컬럼 255자에 맞고 159바이트는 거절된다`() {
        // given
        val fit = "a".repeat(ConnectionPasswordCrypto.MAX_PLAIN_BYTES)

        // when
        val stored = crypto.encrypt(fit)!!

        // then
        assertThat(stored).hasSize(ConnectionPasswordCrypto.COLUMN_LENGTH)
        assertThat(crypto.decrypt(stored)).isEqualTo(fit)
        assertThatThrownBy { crypto.encrypt("a".repeat(ConnectionPasswordCrypto.MAX_PLAIN_BYTES + 1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("비밀번호가 저장 가능한 길이를 넘었습니다")
    }

    @Test
    fun `글자 수나 UTF-8 바이트가 제한을 넘으면 거절한다`() {
        assertThatThrownBy { crypto.requireStorable("a".repeat(129)) }
            .isInstanceOf(PasswordTooLongException::class.java)
            .hasMessageNotContaining("aaa")

        val hangul = "가".repeat(53)
        assertThat(hangul.toByteArray(Charsets.UTF_8)).hasSize(159)
        assertThatThrownBy { crypto.requireStorable(hangul) }
            .isInstanceOf(PasswordTooLongException::class.java)
            .hasMessageNotContaining("가")
    }

    @Test
    fun `요청 toString 에 비밀번호가 없다`() {
        val secret = "my-Secret-pw!"
        val create = CreateConnectionRequest(
            name = "n",
            host = "localhost",
            port = 9092,
            password = secret
        )
        val update = UpdateConnectionRequest(password = secret)

        assertThat(create.toString()).doesNotContain(secret)
        assertThat(update.toString()).doesNotContain(secret)
    }

    @Test
    fun `키가 없으면 시작에 실패한다`() {
        ApplicationContextRunner()
            .withUserConfiguration(ConnectionPasswordCrypto::class.java)
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).rootCause()
                    .isInstanceOf(IllegalStateException::class.java)
                    .hasMessageContaining("KOPANDA_CRYPTO_KEY가 없습니다")
            }
    }

    @Test
    fun `키가 32바이트가 아니면 시작에 실패한다`() {
        val shortKey = Base64.getEncoder().encodeToString(ByteArray(31))
        val longKey = Base64.getEncoder().encodeToString(ByteArray(33))

        listOf(shortKey, longKey, "!!!!").forEach { key ->
            ApplicationContextRunner()
                .withUserConfiguration(ConnectionPasswordCrypto::class.java)
                .withPropertyValues("kopanda.crypto.key=$key")
                .run { context ->
                    assertThat(context).hasFailed()
                    assertThat(context.startupFailure).rootCause()
                        .isInstanceOf(IllegalStateException::class.java)
                        .hasMessageContaining("32바이트")
                }
        }
    }

    companion object {
        private const val TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
}
