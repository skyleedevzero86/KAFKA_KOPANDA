package com.sleekydz86.kopanda.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
@Component
@ConfigurationProperties(prefix = "kafka.jmx")
class KafkaJmxConfig {

    var enabled: Boolean = true
    var port: Int = 9999
    var host: String = "localhost"
    var username: String? = null
    var password: String? = null
    var ssl: Boolean = false
    var sslTruststore: String? = null
    var sslTruststorePassword: String? = null
    var sslKeystore: String? = null
    var sslKeystorePassword: String? = null

    fun getJmxUrl(host: String, port: Int): String {
        return "service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi"
    }

    fun getJmxEnvironment(): Map<String, Any> {
        val environment = mutableMapOf<String, Any>()

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            environment["jmx.remote.credentials"] = arrayOf(username, password)
        }

        if (ssl) {
            environment["jmx.remote.ssl"] = true
            environment["jmx.remote.ssl.need.client.auth"] = true

            val truststore = sslTruststore
            val keystore = sslKeystore

            if (!truststore.isNullOrBlank()) {
                environment["com.sun.management.jmxremote.ssl.truststore"] = truststore
                environment["com.sun.management.jmxremote.ssl.truststore.password"] = sslTruststorePassword ?: ""
            }

            if (!keystore.isNullOrBlank()) {
                environment["com.sun.management.jmxremote.ssl.keystore"] = keystore
                environment["com.sun.management.jmxremote.ssl.keystore.password"] = sslKeystorePassword ?: ""
            }
        }

        return environment
    }
}