package com.sleekydz86.kopanda.domain.valueobjects.network

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PortTest {

    @Test
    fun `유효한 포트를 생성할 수 있다`() {
        // given
        val validPorts = listOf(
            1, 80, 443, 8080, 9092, 65535
        )

        // when & then
        validPorts.forEach { port ->
            val portValue = Port(port)
            assertThat(portValue.value).isEqualTo(port)
        }
    }

    @Test
    fun `0으로 포트를 생성할 수 없다`() {
        // given
        val invalidPort = 0

        // when & then
        assertThatThrownBy { Port(invalidPort) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Port must be between 1 and 65535")
    }

    @Test
    fun `음수로 포트를 생성할 수 없다`() {
        // given
        val invalidPorts = listOf(-1, -100, -65535)

        // when & then
        invalidPorts.forEach { port ->
            assertThatThrownBy { Port(port) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Port must be between 1 and 65535")
        }
    }

    @Test
    fun `65535를 초과하는 포트를 생성할 수 없다`() {
        // given
        val invalidPorts = listOf(65536, 70000, 100000)

        // when & then
        invalidPorts.forEach { port ->
            assertThatThrownBy { Port(port) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Port must be between 1 and 65535")
    }
    }

    @Test
    fun `일반적인 서비스 포트를 생성할 수 있다`() {
        // given
        val commonPorts = mapOf(
            21 to "FTP",
            22 to "SSH",
            23 to "Telnet",
            25 to "SMTP",
            53 to "DNS",
            80 to "HTTP",
            110 to "POP3",
            143 to "IMAP",
            443 to "HTTPS",
            3306 to "MySQL",
            5432 to "PostgreSQL",
            6379 to "Redis",
            8080 to "HTTP Alternative",
            9092 to "Kafka"
        )

        // when & then
        commonPorts.forEach { (port, service) ->
            val portValue = Port(port)
            assertThat(portValue.value).isEqualTo(port)
        }
    }

    @Test
    fun `isEqualTo 메서드가 올바르게 동작한다`() {
        // given
        val port1 = Port(8080)
        val port2 = Port(8080)
        val port3 = Port(9092)

        // when & then
        assertThat(port1.isEqualTo(port2)).isTrue()
        assertThat(port1.isEqualTo(port3)).isFalse()
    }

    @Test
    fun `isLessThan 메서드가 올바르게 동작한다`() {
        // given
        val port1 = Port(8080)
        val port2 = Port(9092)

        // when & then
        assertThat(port1.isLessThan(port2)).isTrue()
        assertThat(port2.isLessThan(port1)).isFalse()
        assertThat(port1.isLessThan(port1)).isFalse()
    }

    @Test
    fun `isGreaterThan 메서드가 올바르게 동작한다`() {
        // given
        val port1 = Port(8080)
        val port2 = Port(9092)

        // when & then
        assertThat(port2.isGreaterThan(port1)).isTrue()
        assertThat(port1.isGreaterThan(port2)).isFalse()
        assertThat(port1.isGreaterThan(port1)).isFalse()
    }

    @Test
    fun `toString 메서드가 올바르게 동작한다`() {
        // given
        val portValue = 8080
        val port = Port(portValue)

        // when
        val result = port.toString()

        // then
        assertThat(result).isEqualTo(portValue.toString())
    }

    @Test
    fun `동일한 값으로 생성된 Port는 같다`() {
        // given
        val portValue = 8080
        val port1 = Port(portValue)
        val port2 = Port(portValue)

        // when & then
        assertThat(port1).isEqualTo(port2)
        assertThat(port1.hashCode()).isEqualTo(port2.hashCode())
    }

    @Test
    fun `다른 값으로 생성된 Port는 다르다`() {
        // given
        val port1 = Port(8080)
        val port2 = Port(9092)

        // when & then
        assertThat(port1).isNotEqualTo(port2)
        assertThat(port1.hashCode()).isNotEqualTo(port2.hashCode())
    }

    @Test
    fun `포트 범위 경계값을 테스트한다`() {
        // given - 경계값들
        val boundaryPorts = listOf(1, 65535)

        // when & then
        boundaryPorts.forEach { port ->
            val portValue = Port(port)
            assertThat(portValue.value).isEqualTo(port)
        }
    }

    @Test
    fun `포트 비교 연산자들이 올바르게 동작한다`() {
        // given
        val smallPort = Port(1024)
        val mediumPort = Port(8080)
        val largePort = Port(65535)

        // when & then
        assertThat(smallPort.isLessThan(mediumPort)).isTrue()
        assertThat(mediumPort.isLessThan(largePort)).isTrue()
        assertThat(largePort.isGreaterThan(mediumPort)).isTrue()
        assertThat(mediumPort.isGreaterThan(smallPort)).isTrue()
        assertThat(smallPort.isEqualTo(smallPort)).isTrue()
    }
}
