package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.response.BrokerMetricsDto
import com.sleekydz86.kopanda.application.dto.response.PerformanceMetricsDto
import com.sleekydz86.kopanda.application.dto.response.TopicMetricsDto
import com.sleekydz86.kopanda.application.dto.response.PartitionMetricsDto
import com.sleekydz86.kopanda.domain.entities.Connection
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import javax.management.*
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@Component
class JmxMetricsCollector {

    private val logger = LoggerFactory.getLogger(JmxMetricsCollector::class.java)
    private val jmxConnectors = ConcurrentHashMap<String, JMXConnector>()
    private val mbeanServers = ConcurrentHashMap<String, MBeanServerConnection>()

    companion object {
        private const val KAFKA_SERVER_METRICS_DOMAIN = "kafka.server"
        private const val KAFKA_CONTROLLER_METRICS_DOMAIN = "kafka.controller"
        private const val KAFKA_NETWORK_METRICS_DOMAIN = "kafka.network"
        private const val KAFKA_LOG_METRICS_DOMAIN = "kafka.log"
        private const val KAFKA_CLUSTER_METRICS_DOMAIN = "kafka.cluster"

        private const val BROKER_METRICS = "kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"
        private const val BYTES_IN_METRICS = "kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec"
        private const val BYTES_OUT_METRICS = "kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec"
        private const val REQUEST_METRICS = "kafka.network:type=RequestMetrics,name=RequestsPerSec,request=Produce"
        private const val REPLICATION_METRICS = "kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions"
        private const val CONTROLLER_METRICS = "kafka.controller:type=KafkaController,name=ActiveControllerCount"
        private const val LOG_METRICS = "kafka.log:type=LogFlushStats,name=LogFlushRateAndTimeMs"
        private const val CLUSTER_METRICS = "kafka.cluster:type=Partition,name=UnderReplicated,partition=*"
    }

    fun connectToJmx(connection: Connection): Boolean {
        val connectionId = connection.getId().value
        val jmxPort = connection.port.value + 1000

        return try {
            val jmxUrl = JMXServiceURL("service:jmx:rmi:///jndi/rmi://${connection.host.value}:$jmxPort/jmxrmi")
            val jmxConnector = JMXConnectorFactory.connect(jmxUrl)
            val mbeanServer = jmxConnector.mBeanServerConnection

            jmxConnectors[connectionId] = jmxConnector
            mbeanServers[connectionId] = mbeanServer

            logger.info("Successfully connected to JMX for connection: $connectionId")
            true
        } catch (e: Exception) {
            logger.error("Failed to connect to JMX for connection: $connectionId", e)
            false
        }
    }

    fun disconnectFromJmx(connectionId: String) {
        try {
            jmxConnectors[connectionId]?.close()
            jmxConnectors.remove(connectionId)
            mbeanServers.remove(connectionId)
            logger.info("Disconnected from JMX for connection: $connectionId")
        } catch (e: Exception) {
            logger.error("Error disconnecting from JMX for connection: $connectionId", e)
        }
    }

    fun collectBrokerMetrics(connection: Connection): BrokerMetricsDto {
        val connectionId = connection.getId().value
        val mbeanServer = mbeanServers[connectionId] ?: return createDefaultBrokerMetrics()

        return try {
            val totalBrokers = getClusterBrokerCount(mbeanServer)
            val activeBrokers = getActiveBrokerCount(mbeanServer)
            val offlineBrokers = totalBrokers - activeBrokers
            val averageResponseTime = getAverageResponseTime(mbeanServer)
            val diskUsage = getDiskUsage(mbeanServer)
            val availableDiskSpace = getAvailableDiskSpace(mbeanServer)

            BrokerMetricsDto(
                totalBrokers = totalBrokers,
                activeBrokers = activeBrokers,
                offlineBrokers = offlineBrokers,
                averageResponseTime = averageResponseTime,
                totalDiskUsage = diskUsage,
                availableDiskSpace = availableDiskSpace
            )
        } catch (e: Exception) {
            logger.error("Error collecting broker metrics for connection: $connectionId", e)
            createDefaultBrokerMetrics()
        }
    }

    fun collectPerformanceMetrics(connection: Connection): PerformanceMetricsDto {
        val connectionId = connection.getId().value
        val mbeanServer = mbeanServers[connectionId] ?: return createDefaultPerformanceMetrics()

        return try {
            val messagesPerSecond = getMessagesPerSecond(mbeanServer)
            val bytesInPerSec = getBytesInPerSecond(mbeanServer)
            val bytesOutPerSec = getBytesOutPerSecond(mbeanServer)
            val requestsPerSec = getRequestsPerSecond(mbeanServer)
            val averageRequestLatency = getAverageRequestLatency(mbeanServer)
            val maxRequestLatency = getMaxRequestLatency(mbeanServer)
            val activeConnections = getActiveConnections(mbeanServer)
            val totalConnections = getTotalConnections(mbeanServer)

            PerformanceMetricsDto(
                messagesPerSecond = messagesPerSecond,
                bytesInPerSec = bytesInPerSec,
                bytesOutPerSec = bytesOutPerSec,
                requestsPerSec = requestsPerSec,
                averageRequestLatency = averageRequestLatency,
                maxRequestLatency = maxRequestLatency,
                activeConnections = activeConnections,
                totalConnections = totalConnections
            )
        } catch (e: Exception) {
            logger.error("Error collecting performance metrics for connection: $connectionId", e)
            createDefaultPerformanceMetrics()
        }
    }

    fun collectTopicMetrics(connection: Connection): TopicMetricsDto {
        val connectionId = connection.getId().value
        val mbeanServer = mbeanServers[connectionId] ?: return createDefaultTopicMetrics()

        return try {
            val totalTopics = getTotalTopics(mbeanServer)
            val internalTopics = getInternalTopics(mbeanServer)
            val userTopics = totalTopics - internalTopics
            val totalPartitions = getTotalPartitions(mbeanServer)
            val underReplicatedPartitions = getUnderReplicatedPartitions(mbeanServer)
            val offlinePartitions = getOfflinePartitions(mbeanServer)
            val totalMessages = getTotalMessages(mbeanServer)
            val messagesPerSecond = getMessagesPerSecond(mbeanServer)

            TopicMetricsDto(
                totalTopics = totalTopics,
                internalTopics = internalTopics,
                userTopics = userTopics,
                totalPartitions = totalPartitions,
                underReplicatedPartitions = underReplicatedPartitions,
                offlinePartitions = offlinePartitions,
                totalMessages = totalMessages,
                messagesPerSecond = messagesPerSecond
            )
        } catch (e: Exception) {
            logger.error("Error collecting topic metrics for connection: $connectionId", e)
            createDefaultTopicMetrics()
        }
    }

    fun collectPartitionMetrics(connection: Connection): PartitionMetricsDto {
        val connectionId = connection.getId().value
        val mbeanServer = mbeanServers[connectionId] ?: return createDefaultPartitionMetrics()

        return try {
            val totalPartitions = getTotalPartitions(mbeanServer)
            val healthyPartitions = getHealthyPartitions(mbeanServer)
            val underReplicatedPartitions = getUnderReplicatedPartitions(mbeanServer)
            val offlinePartitions = getOfflinePartitions(mbeanServer)
            val averageReplicationFactor = getAverageReplicationFactor(mbeanServer)
            val totalMessages = getTotalMessages(mbeanServer)
            val averageMessagesPerPartition = if (totalPartitions > 0) totalMessages.toDouble() / totalPartitions else 0.0

            PartitionMetricsDto(
                totalPartitions = totalPartitions,
                healthyPartitions = healthyPartitions,
                underReplicatedPartitions = underReplicatedPartitions,
                offlinePartitions = offlinePartitions,
                averageReplicationFactor = averageReplicationFactor,
                totalMessages = totalMessages,
                averageMessagesPerPartition = averageMessagesPerPartition
            )
        } catch (e: Exception) {
            logger.error("Error collecting partition metrics for connection: $connectionId", e)
            createDefaultPartitionMetrics()
        }
    }

    private fun getClusterBrokerCount(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_CLUSTER_METRICS_DOMAIN:type=Partition,name=UnderReplicated,partition=*")
            val queryNames = mbeanServer.queryNames(objectName, null)
            queryNames.size
        } catch (e: Exception) {
            logger.warn("Could not get cluster broker count", e)
            1
        }
    }

    private fun getActiveBrokerCount(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_CONTROLLER_METRICS_DOMAIN:type=KafkaController,name=ActiveControllerCount")
            val activeControllerCount = mbeanServer.getAttribute(objectName, "Value") as? Number ?: 0
            activeControllerCount.toInt()
        } catch (e: Exception) {
            logger.warn("Could not get active broker count", e)
            1
        }
    }

    private fun getAverageResponseTime(mbeanServer: MBeanServerConnection): Long {
        return try {
            val objectName = ObjectName("$KAFKA_NETWORK_METRICS_DOMAIN:type=RequestMetrics,name=LocalTimeMs,request=*")
            val queryNames = mbeanServer.queryNames(objectName, null)

            if (queryNames.isEmpty()) return 0L

            val totalLatency = queryNames.sumOf { name ->
                try {
                    (mbeanServer.getAttribute(name, "Mean") as? Number ?: 0).toLong()
                } catch (e: Exception) {
                    0L
                }
            }

            (totalLatency.toDouble() / queryNames.size).roundToInt().toLong()
        } catch (e: Exception) {
            logger.warn("Could not get average response time", e)
            0L
        }
    }

    private fun getDiskUsage(mbeanServer: MBeanServerConnection): Long {
        return try {
            val objectName = ObjectName("$KAFKA_LOG_METRICS_DOMAIN:type=LogFlushStats,name=LogFlushRateAndTimeMs")
            val diskUsage = mbeanServer.getAttribute(objectName, "TotalLogSize") as? Number ?: 0
            diskUsage.toLong()
        } catch (e: Exception) {
            logger.warn("Could not get disk usage", e)
            0L
        }
    }

    private fun getAvailableDiskSpace(mbeanServer: MBeanServerConnection): Long {
        return try {
            val objectName = ObjectName("$KAFKA_LOG_METRICS_DOMAIN:type=LogFlushStats,name=LogFlushRateAndTimeMs")
            val availableSpace = mbeanServer.getAttribute(objectName, "AvailableLogSize") as? Number ?: 0
            availableSpace.toLong()
        } catch (e: Exception) {
            logger.warn("Could not get available disk space", e)
            0L
        }
    }

    private fun getMessagesPerSecond(mbeanServer: MBeanServerConnection): Double {
        return try {
            val objectName = ObjectName(BROKER_METRICS)
            val messagesPerSec = mbeanServer.getAttribute(objectName, "OneMinuteRate") as? Number ?: 0
            messagesPerSec.toDouble()
        } catch (e: Exception) {
            logger.warn("Could not get messages per second", e)
            0.0
        }
    }

    private fun getBytesInPerSecond(mbeanServer: MBeanServerConnection): Double {
        return try {
            val objectName = ObjectName(BYTES_IN_METRICS)
            val bytesInPerSec = mbeanServer.getAttribute(objectName, "OneMinuteRate") as? Number ?: 0
            bytesInPerSec.toDouble()
        } catch (e: Exception) {
            logger.warn("Could not get bytes in per second", e)
            0.0
        }
    }

    private fun getBytesOutPerSecond(mbeanServer: MBeanServerConnection): Double {
        return try {
            val objectName = ObjectName(BYTES_OUT_METRICS)
            val bytesOutPerSec = mbeanServer.getAttribute(objectName, "OneMinuteRate") as? Number ?: 0
            bytesOutPerSec.toDouble()
        } catch (e: Exception) {
            logger.warn("Could not get bytes out per second", e)
            0.0
        }
    }

    private fun getRequestsPerSecond(mbeanServer: MBeanServerConnection): Double {
        return try {
            val objectName = ObjectName(REQUEST_METRICS)
            val requestsPerSec = mbeanServer.getAttribute(objectName, "OneMinuteRate") as? Number ?: 0
            requestsPerSec.toDouble()
        } catch (e: Exception) {
            logger.warn("Could not get requests per second", e)
            0.0
        }
    }

    private fun getAverageRequestLatency(mbeanServer: MBeanServerConnection): Long {
        return try {
            val objectName = ObjectName("$KAFKA_NETWORK_METRICS_DOMAIN:type=RequestMetrics,name=LocalTimeMs,request=*")
            val queryNames = mbeanServer.queryNames(objectName, null)

            if (queryNames.isEmpty()) return 0L

            val totalLatency = queryNames.sumOf { name ->
                try {
                    (mbeanServer.getAttribute(name, "Mean") as? Number ?: 0).toLong()
                } catch (e: Exception) {
                    0L
                }
            }

            (totalLatency.toDouble() / queryNames.size).roundToInt().toLong()
        } catch (e: Exception) {
            logger.warn("Could not get average request latency", e)
            0L
        }
    }

    private fun getMaxRequestLatency(mbeanServer: MBeanServerConnection): Long {
        return try {
            val objectName = ObjectName("$KAFKA_NETWORK_METRICS_DOMAIN:type=RequestMetrics,name=LocalTimeMs,request=*")
            val queryNames = mbeanServer.queryNames(objectName, null)

            if (queryNames.isEmpty()) return 0L

            val maxLatency = queryNames.maxOfOrNull { name ->
                try {
                    (mbeanServer.getAttribute(name, "Max") as? Number ?: 0).toLong()
                } catch (e: Exception) {
                    0L
                }
            }

            maxLatency ?: 0L
        } catch (e: Exception) {
            logger.warn("Could not get max request latency", e)
            0L
        }
    }

    private fun getActiveConnections(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_NETWORK_METRICS_DOMAIN:type=SocketServer,name=NetworkProcessorAvgIdlePercent")
            val activeConnections = mbeanServer.getAttribute(objectName, "Value") as? Number ?: 0
            activeConnections.toInt()
        } catch (e: Exception) {
            logger.warn("Could not get active connections", e)
            0
        }
    }

    private fun getTotalConnections(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_NETWORK_METRICS_DOMAIN:type=SocketServer,name=NetworkProcessorAvgIdlePercent")
            val totalConnections = mbeanServer.getAttribute(objectName, "Count") as? Number ?: 0
            totalConnections.toInt()
        } catch (e: Exception) {
            logger.warn("Could not get total connections", e)
            0
        }
    }

    private fun getTotalTopics(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_SERVER_METRICS_DOMAIN:type=BrokerTopicMetrics,name=*")
            val queryNames = mbeanServer.queryNames(objectName, null)
            queryNames.size
        } catch (e: Exception) {
            logger.warn("Could not get total topics", e)
            0
        }
    }

    private fun getInternalTopics(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_SERVER_METRICS_DOMAIN:type=BrokerTopicMetrics,name=*")
            val queryNames = mbeanServer.queryNames(objectName, null)
            queryNames.count { name ->
                name.getKeyProperty("name").startsWith("__")
            }
        } catch (e: Exception) {
            logger.warn("Could not get internal topics", e)
            0
        }
    }

    private fun getTotalPartitions(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_CLUSTER_METRICS_DOMAIN:type=Partition,name=UnderReplicated,partition=*")
            val queryNames = mbeanServer.queryNames(objectName, null)
            queryNames.size
        } catch (e: Exception) {
            logger.warn("Could not get total partitions", e)
            0
        }
    }

    private fun getUnderReplicatedPartitions(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName(REPLICATION_METRICS)
            val underReplicatedPartitions = mbeanServer.getAttribute(objectName, "Value") as? Number ?: 0
            underReplicatedPartitions.toInt()
        } catch (e: Exception) {
            logger.warn("Could not get under replicated partitions", e)
            0
        }
    }

    private fun getOfflinePartitions(mbeanServer: MBeanServerConnection): Int {
        return try {
            val objectName = ObjectName("$KAFKA_CLUSTER_METRICS_DOMAIN:type=Partition,name=OfflinePartitionsCount")
            val offlinePartitions = mbeanServer.getAttribute(objectName, "Value") as? Number ?: 0
            offlinePartitions.toInt()
        } catch (e: Exception) {
            logger.warn("Could not get offline partitions", e)
            0
        }
    }

    private fun getTotalMessages(mbeanServer: MBeanServerConnection): Long {
        return try {
            val objectName = ObjectName(BROKER_METRICS)
            val totalMessages = mbeanServer.getAttribute(objectName, "Count") as? Number ?: 0
            totalMessages.toLong()
        } catch (e: Exception) {
            logger.warn("Could not get total messages", e)
            0L
        }
    }

    private fun getHealthyPartitions(mbeanServer: MBeanServerConnection): Int {
        val totalPartitions = getTotalPartitions(mbeanServer)
        val underReplicatedPartitions = getUnderReplicatedPartitions(mbeanServer)
        val offlinePartitions = getOfflinePartitions(mbeanServer)
        return totalPartitions - underReplicatedPartitions - offlinePartitions
    }

    private fun getAverageReplicationFactor(mbeanServer: MBeanServerConnection): Double {
        return try {
            val objectName = ObjectName("$KAFKA_CLUSTER_METRICS_DOMAIN:type=Partition,name=ReplicationFactor,partition=*")
            val queryNames = mbeanServer.queryNames(objectName, null)

            if (queryNames.isEmpty()) return 1.0

            val totalReplicationFactor = queryNames.sumOf { name ->
                try {
                    (mbeanServer.getAttribute(name, "Value") as? Number ?: 1).toInt()
                } catch (e: Exception) {
                    1
                }
            }

            totalReplicationFactor.toDouble() / queryNames.size
        } catch (e: Exception) {
            logger.warn("Could not get average replication factor", e)
            1.0
        }
    }

    private fun createDefaultBrokerMetrics(): BrokerMetricsDto {
        return BrokerMetricsDto(
            totalBrokers = 1,
            activeBrokers = 1,
            offlineBrokers = 0,
            averageResponseTime = 0L,
            totalDiskUsage = 0L,
            availableDiskSpace = 0L
        )
    }

    private fun createDefaultPerformanceMetrics(): PerformanceMetricsDto {
        return PerformanceMetricsDto(
            messagesPerSecond = 0.0,
            bytesInPerSec = 0.0,
            bytesOutPerSec = 0.0,
            requestsPerSec = 0.0,
            averageRequestLatency = 0L,
            maxRequestLatency = 0L,
            activeConnections = 0,
            totalConnections = 0
        )
    }

    private fun createDefaultTopicMetrics(): TopicMetricsDto {
        return TopicMetricsDto(
            totalTopics = 0,
            internalTopics = 0,
            userTopics = 0,
            totalPartitions = 0,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            totalMessages = 0L,
            messagesPerSecond = 0.0
        )
    }

    private fun createDefaultPartitionMetrics(): PartitionMetricsDto {
        return PartitionMetricsDto(
            totalPartitions = 0,
            healthyPartitions = 0,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            averageReplicationFactor = 1.0,
            totalMessages = 0L,
            averageMessagesPerPartition = 0.0
        )
    }
}