package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.domain.entities.Connection
import jakarta.annotation.PreDestroy
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.common.errors.AuthenticationException
import org.apache.kafka.common.errors.BrokerNotAvailableException
import org.apache.kafka.common.errors.DisconnectException
import org.apache.kafka.common.errors.NetworkException
import org.apache.kafka.common.errors.TimeoutException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.ClosedChannelException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class AdminClientRegistry {

    private val logger = LoggerFactory.getLogger(AdminClientRegistry::class.java)
    private val clients = ConcurrentHashMap<String, Entry>()

    fun get(connection: Connection): AdminClient {
        val id = connection.getId().value
        val fingerprint = fingerprintOf(connection)
        return clients.compute(id) { _, current ->
            if (current != null && current.fingerprint == fingerprint) {
                current
            } else {
                val created = Entry(open(connection), fingerprint)
                if (current != null) {
                    close(current.admin)
                }
                created
            }
        }!!.admin
    }

    fun evict(connectionId: String) {
        val removed = clients.remove(connectionId) ?: return
        close(removed.admin)
    }

    fun evictOnConnectionFailure(connectionId: String, error: Throwable) {
        if (!isConnectionFailure(error) || !clients.containsKey(connectionId)) {
            return
        }
        logger.warn("연결 오류로 AdminClient를 버립니다: {}", connectionId, error)
        evict(connectionId)
    }

    @PreDestroy
    fun closeAll() {
        clients.keys.toList().forEach { evict(it) }
    }

    override fun toString(): String = "AdminClientRegistry(size=${clients.size})"

    private fun open(connection: Connection): AdminClient {
        val config = KafkaClientProps.base(connection)
        config[AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000
        config[AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = 10000
        config[AdminClientConfig.RETRIES_CONFIG] = 3
        config[AdminClientConfig.RETRY_BACKOFF_MS_CONFIG] = 100
        config[AdminClientConfig.CLIENT_ID_CONFIG] = "kopanda-admin-client"
        config[AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG] = 300000
        return AdminClient.create(config)
    }

    private fun close(admin: AdminClient) {
        try {
            admin.close(Duration.ofSeconds(5))
        } catch (e: Exception) {
            logger.warn("AdminClient 종료에 실패했습니다", e)
        }
    }

    private fun fingerprintOf(connection: Connection): Fingerprint {
        return Fingerprint(
            host = connection.host.value,
            port = connection.port.value,
            sslEnabled = connection.sslEnabled,
            saslEnabled = connection.saslEnabled,
            username = connection.username,
            password = connection.password
        )
    }

    private fun isConnectionFailure(error: Throwable): Boolean {
        return generateSequence(error) { it.cause }.any { throwable ->
            throwable is TimeoutException ||
                throwable is java.util.concurrent.TimeoutException ||
                throwable is AuthenticationException ||
                throwable is DisconnectException ||
                throwable is NetworkException ||
                throwable is BrokerNotAvailableException ||
                throwable is ConnectException ||
                throwable is UnknownHostException ||
                throwable is SocketTimeoutException ||
                throwable is NoRouteToHostException ||
                throwable is ClosedChannelException ||
                (throwable is IllegalStateException && throwable.message?.contains("closed", ignoreCase = true) == true)
        }
    }

    private class Entry(
        val admin: AdminClient,
        val fingerprint: Fingerprint
    ) {
        override fun toString(): String = "Entry(fingerprint=$fingerprint)"
    }

    private data class Fingerprint(
        val host: String,
        val port: Int,
        val sslEnabled: Boolean,
        val saslEnabled: Boolean,
        val username: String?,
        val password: String?
    ) {
        override fun toString(): String {
            return "Fingerprint(host=$host, port=$port, sslEnabled=$sslEnabled, saslEnabled=$saslEnabled, username=$username)"
        }
    }
}
