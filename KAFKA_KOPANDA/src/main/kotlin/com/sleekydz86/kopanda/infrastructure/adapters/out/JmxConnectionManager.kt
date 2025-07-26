package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.domain.entities.Connection
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PreDestroy

@Service
class JmxConnectionManager(
    private val jmxMetricsCollector: JmxMetricsCollector
) {

    private val logger = LoggerFactory.getLogger(JmxConnectionManager::class.java)
    private val activeConnections = ConcurrentHashMap<String, Connection>()

    fun addConnection(connection: Connection) {
        val connectionId = connection.getId().value
        activeConnections[connectionId] = connection

        val connected = jmxMetricsCollector.connectToJmx(connection)
        if (connected) {
            logger.info("Successfully connected to JMX for connection: $connectionId")
        } else {
            logger.warn("Failed to connect to JMX for connection: $connectionId")
        }
    }

    fun removeConnection(connectionId: String) {
        activeConnections.remove(connectionId)
        jmxMetricsCollector.disconnectFromJmx(connectionId)
        logger.info("Removed JMX connection: $connectionId")
    }

    fun getActiveConnections(): List<Connection> {
        return activeConnections.values.toList()
    }

    @Scheduled(fixedRate = 30000) // 30초마다 실행
    fun healthCheckConnections() {
        logger.debug("Starting JMX connection health check")

        activeConnections.forEach { (connectionId, connection) ->
            try {
                val isHealthy = jmxMetricsCollector.connectToJmx(connection)
                if (!isHealthy) {
                    logger.warn("JMX connection health check failed for: $connectionId")
                }
            } catch (e: Exception) {
                logger.error("Error during JMX health check for connection: $connectionId", e)
            }
        }
    }

    @PreDestroy
    fun cleanup() {
        logger.info("Cleaning up JMX connections")
        activeConnections.keys.forEach { connectionId ->
            jmxMetricsCollector.disconnectFromJmx(connectionId)
        }
        activeConnections.clear()
    }
}