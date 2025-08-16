package com.sleekydz86.kopanda.domain.valueobjects.names

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ConnectionNameTest {

    @Test
    fun `유효한 연결 이름을 생성할 수 있다`() {
        // given
        val validNames = listOf(
            "test-connection",
            "kafka_prod",
            "local-dev",
            "연결테스트",
            "Connection 01",
            "my_connection_123"
        )

        // when & then
        validNames.forEach { name ->
            val connectionName = ConnectionName(name)
            assertThat(connectionName.value).isEqualTo(name)
        }
    }

    @Test
    fun `빈 문자열로 연결 이름을 생성할 수 없다`() {
        // given
        val emptyName = ""

        // when & then
        assertThatThrownBy { ConnectionName(emptyName) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Connection name cannot be blank")
    }

    @Test
    fun `공백만으로 연결 이름을 생성할 수 없다`() {
        // given
        val blankName = "   "

        // when & then
        assertThatThrownBy { ConnectionName(blankName) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Connection name cannot be blank")
    }

    @Test
    fun `100자를 초과하는 연결 이름을 생성할 수 없다`() {
        // given
        val longName = "a".repeat(101)

        // when & then
        assertThatThrownBy { ConnectionName(longName) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Connection name cannot exceed 100 characters")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "test@connection",
        "kafka#prod",
        "local$dev",
        "test*connection",
        "kafka+prod"
    ])
    fun `특수문자가 포함된 연결 이름을 생성할 수 없다`(invalidName: String) {
        // when & then
        assertThatThrownBy { ConnectionName(invalidName) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Connection name can only contain alphanumeric characters, Korean characters, spaces, underscores, and hyphens")
    }

    @Test
    fun `한글이 포함된 연결 이름을 생성할 수 있다`() {
        // given
        val koreanName = "카프카연결테스트"

        // when
        val connectionName = ConnectionName(koreanName)

        // then
        assertThat(connectionName.value).isEqualTo(koreanName)
    }

    @Test
    fun `숫자가 포함된 연결 이름을 생성할 수 있다`() {
        // given
        val numericName = "connection123"

        // when
        val connectionName = ConnectionName(numericName)

        // then
        assertThat(connectionName.value).isEqualTo(numericName)
    }

    @Test
    fun `언더스코어가 포함된 연결 이름을 생성할 수 있다`() {
        // given
        val underscoreName = "test_connection"

        // when
        val connectionName = ConnectionName(underscoreName)

        // then
        assertThat(connectionName.value).isEqualTo(underscoreName)
    }

    @Test
    fun `하이픈이 포함된 연결 이름을 생성할 수 있다`() {
        // given
        val hyphenName = "test-connection"

        // when
        val connectionName = ConnectionName(hyphenName)

        // then
        assertThat(connectionName.value).isEqualTo(hyphenName)
    }

    @Test
    fun `공백이 포함된 연결 이름을 생성할 수 있다`() {
        // given
        val spaceName = "test connection"

        // when
        val connectionName = ConnectionName(spaceName)

        // then
        assertThat(connectionName.value).isEqualTo(spaceName)
    }

    @Test
    fun `toString 메서드가 올바르게 동작한다`() {
        // given
        val name = "test-connection"
        val connectionName = ConnectionName(name)

        // when
        val result = connectionName.toString()

        // then
        assertThat(result).isEqualTo(name)
    }

    @Test
    fun `동일한 값으로 생성된 ConnectionName은 같다`() {
        // given
        val name = "test-connection"
        val connectionName1 = ConnectionName(name)
        val connectionName2 = ConnectionName(name)

        // when & then
        assertThat(connectionName1).isEqualTo(connectionName2)
        assertThat(connectionName1.hashCode()).isEqualTo(connectionName2.hashCode())
    }

    @Test
    fun `다른 값으로 생성된 ConnectionName은 다르다`() {
        // given
        val connectionName1 = ConnectionName("connection1")
        val connectionName2 = ConnectionName("connection2")

        // when & then
        assertThat(connectionName1).isNotEqualTo(connectionName2)
        assertThat(connectionName1.hashCode()).isNotEqualTo(connectionName2.hashCode())
    }
}
