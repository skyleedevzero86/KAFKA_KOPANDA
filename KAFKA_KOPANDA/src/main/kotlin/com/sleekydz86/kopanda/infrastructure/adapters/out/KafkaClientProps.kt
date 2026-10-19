package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.domain.entities.Connection
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import java.util.Properties

object KafkaClientProps {

    fun base(connection: Connection): Properties {
        val props = Properties()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] =
            "${connection.host.value}:${connection.port.value}"

        val sasl = connection.saslEnabled && connection.username != null
        when {
            connection.sslEnabled && sasl ->
                props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
            sasl ->
                props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
            connection.sslEnabled ->
                props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        }

        if (sasl) {
            props[SaslConfigs.SASL_MECHANISM] = "PLAIN"
            props[SaslConfigs.SASL_JAAS_CONFIG] =
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"${escapeJaas(connection.username!!)}\" " +
                    "password=\"${escapeJaas(connection.password ?: "")}\";"
        }

        return props
    }

    private fun escapeJaas(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
