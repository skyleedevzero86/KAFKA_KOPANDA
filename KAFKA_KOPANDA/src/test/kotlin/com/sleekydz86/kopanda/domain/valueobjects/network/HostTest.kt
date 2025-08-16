package com.sleekydz86.kopanda.domain.valueobjects.network

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class HostTest {

    @Test
    fun `유효한 호스트를 생성할 수 있다`() {
        // given
        val validHosts = listOf(
            "localhost",
            "192.168.1.1",
            "kafka.example.com",
            "prod-kafka-01",
            "10.0.0.1"
        )

        // when & then
        validHosts.forEach { host ->
            val hostValue = Host(host)
            assertThat(hostValue.value).isEqualTo(host)
        }
    }

    @Test
    fun `빈 문자열로 호스트를 생성할 수 없다`() {
        // given
        val emptyHost = ""

        // when & then
        assertThatThrownBy { Host(emptyHost) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Host cannot be blank")
    }

    @Test
    fun `공백만으로 호스트를 생성할 수 없다`() {
        // given
        val blankHost = "   "

        // when & then
        assertThatThrownBy { Host(blankHost) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Host cannot be blank")
    }

    @ParameterizedTest
    @MethodSource("invalidHostsWithSpecialCharacters")
    fun `특수문자가 포함된 호스트를 생성할 수 없다`(invalidHost: String) {
        // when & then
        assertThatThrownBy { Host(invalidHost) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Host can only contain alphanumeric characters, dots, and hyphens")
    }

    @Test
    fun `숫자와 점으로 구성된 IP 주소를 생성할 수 있다`() {
        // given
        val ipAddresses = listOf(
            "127.0.0.1",
            "192.168.1.1",
            "10.0.0.1",
            "172.16.0.1",
            "8.8.8.8"
        )

        // when & then
        ipAddresses.forEach { ip ->
            val host = Host(ip)
            assertThat(host.value).isEqualTo(ip)
        }
    }

    @Test
    fun `도메인 이름을 생성할 수 있다`() {
        // given
        val domainNames = listOf(
            "example.com",
            "sub.example.com",
            "kafka.prod.example.com",
            "localhost",
            "prod-kafka-01"
        )

        // when & then
        domainNames.forEach { domain ->
            val host = Host(domain)
            assertThat(host.value).isEqualTo(domain)
        }
    }

    @Test
    fun `하이픈이 포함된 호스트를 생성할 수 있다`() {
        // given
        val hyphenatedHosts = listOf(
            "prod-kafka-01",
            "test-server-02",
            "kafka-cluster-01"
        )

        // when & then
        hyphenatedHosts.forEach { host ->
            val hostValue = Host(host)
            assertThat(hostValue.value).isEqualTo(host)
        }
    }

    @Test
    fun `점이 포함된 호스트를 생성할 수 있다`() {
        // given
        val dottedHosts = listOf(
            "kafka.example.com",
            "prod.kafka.cluster",
            "192.168.1.1"
        )

        // when & then
        dottedHosts.forEach { host ->
            val hostValue = Host(host)
            assertThat(hostValue.value).isEqualTo(host)
        }
    }

    @Test
    fun `isValid 메서드가 올바르게 동작한다`() {
        // given
        val validHosts = listOf(
            "localhost",
            "127.0.0.1",
            "kafka.example.com"
        )
        val invalidHosts = listOf(
            "invalid-host-name-that-is-way-too-long-and-exceeds-normal-length-limits",
            "host with spaces",
            "host@invalid.com"
        )

        // when & then
        validHosts.forEach { host ->
            val hostValue = Host(host)
            assertThat(hostValue.isValid()).isTrue()
        }
    }

    @Test
    fun `toString 메서드가 올바르게 동작한다`() {
        // given
        val hostValue = "kafka.example.com"
        val host = Host(hostValue)

        // when
        val result = host.toString()

        // then
        assertThat(result).isEqualTo(hostValue)
    }

    @Test
    fun `동일한 값으로 생성된 Host는 같다`() {
        // given
        val hostValue = "kafka.example.com"
        val host1 = Host(hostValue)
        val host2 = Host(hostValue)

        // when & then
        assertThat(host1).isEqualTo(host2)
        assertThat(host1.hashCode()).isEqualTo(host2.hashCode())
    }

    @Test
    fun `다른 값으로 생성된 Host는 다르다`() {
        // given
        val host1 = Host("kafka1.example.com")
        val host2 = Host("kafka2.example.com")

        // when & then
        assertThat(host1).isNotEqualTo(host2)
        assertThat(host1.hashCode()).isNotEqualTo(host2.hashCode())
    }

    companion object {
        @JvmStatic
        fun invalidHostsWithSpecialCharacters(): Stream<String> {
            return Stream.of(
                "host@example.com",
                "host#example.com",
                "host\$example.com",
                "host*example.com",
                "host+example.com",
                "host(example.com",
                "host)example.com",
                "host[example.com",
                "host]example.com",
                "host{example.com",
                "host}example.com",
                "host|example.com",
                "host\\example.com",
                "host/example.com",
                "host:example.com",
                "host;example.com",
                "host<example.com",
                "host>example.com",
                "host=example.com",
                "host?example.com",
                "host!example.com",
                "host~example.com",
                "host`example.com"
            )
        }
    }
}