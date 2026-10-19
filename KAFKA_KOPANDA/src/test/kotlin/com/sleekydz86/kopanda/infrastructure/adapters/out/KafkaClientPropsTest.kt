package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.domain.entities.Connection
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KafkaClientPropsTest {

    @Test
    fun `SSL만 켜면 security protocol은 SSL이고 sasl 설정은 없다`() {
        // given
        val connection = connection(sslEnabled = true)

        // when
        val props = KafkaClientProps.base(connection)

        // then
        assertThat(props.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092")
        assertThat(props.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isEqualTo("SSL")
        assertThat(props.getProperty(SaslConfigs.SASL_MECHANISM)).isNull()
        assertThat(props.getProperty(SaslConfigs.SASL_JAAS_CONFIG)).isNull()
    }

    @Test
    fun `SASL만 켜면 SASL_PLAINTEXT와 PLAIN JAAS를 넣는다`() {
        // given
        val connection = connection(saslEnabled = true, username = "admin", password = "secret")

        // when
        val props = KafkaClientProps.base(connection)

        // then
        assertThat(props.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isEqualTo("SASL_PLAINTEXT")
        assertThat(props.getProperty(SaslConfigs.SASL_MECHANISM)).isEqualTo("PLAIN")
        assertThat(props.getProperty(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"admin\" password=\"secret\";"
        )
    }

    @Test
    fun `SSL과 SASL을 같이 켜면 security protocol은 SASL_SSL이다`() {
        // given
        val connection = connection(sslEnabled = true, saslEnabled = true, username = "admin", password = "secret")

        // when
        val props = KafkaClientProps.base(connection)

        // then
        assertThat(props.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isEqualTo("SASL_SSL")
        assertThat(props.getProperty(SaslConfigs.SASL_MECHANISM)).isEqualTo("PLAIN")
    }

    @Test
    fun `비밀번호의 따옴표와 역슬래시는 JAAS 문자열에서 이스케이프된다`() {
        // given
        val connection = connection(saslEnabled = true, username = "admin", password = "pa\"ss\\word")

        // when
        val props = KafkaClientProps.base(connection)

        // then
        assertThat(props.getProperty(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"admin\" password=\"pa\\\"ss\\\\word\";"
        )
    }

    @Test
    fun `SSL과 SASL이 꺼지면 bootstrap만 있다`() {
        // given
        val connection = connection()

        // when
        val props = KafkaClientProps.base(connection)

        // then
        assertThat(props.stringPropertyNames()).containsExactly(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)
        assertThat(props.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092")
        assertThat(props.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isNull()
    }

    private fun connection(
        sslEnabled: Boolean = false,
        saslEnabled: Boolean = false,
        username: String? = null,
        password: String? = null
    ): Connection = Connection.create(
        name = "test-connection",
        host = "localhost",
        port = 9092,
        sslEnabled = sslEnabled,
        saslEnabled = saslEnabled,
        username = username,
        password = password
    )
}
