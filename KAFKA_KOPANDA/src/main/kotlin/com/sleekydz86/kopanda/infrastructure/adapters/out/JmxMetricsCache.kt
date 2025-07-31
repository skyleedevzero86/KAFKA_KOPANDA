package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.response.*
import com.sleekydz86.kopanda.domain.entities.Connection
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class JmxMetricsCache(
    private val jmxMetricsCollector: JmxMetricsCollector
) {

    private val metricsCache = ConcurrentHashMap<String, CachedMetrics>()

    data class CachedMetrics(
        val brokerMetrics: BrokerMetricsDto,
        val performanceMetrics: PerformanceMetricsDto,
        val topicMetrics: TopicMetricsDto,
        val partitionMetrics: PartitionMetricsDto,
        val timestamp: LocalDateTime
    )

    @Cacheable("brokerMetrics", key = "#connection.getId().value")
    fun getBrokerMetrics(connection: Connection): BrokerMetricsDto {
        return jmxMetricsCollector.collectBrokerMetrics(connection)
    }

    @Cacheable("performanceMetrics", key = "#connection.getId().value")
    fun getPerformanceMetrics(connection: Connection): PerformanceMetricsDto {
        return jmxMetricsCollector.collectPerformanceMetrics(connection)
    }

    @Cacheable("topicMetrics", key = "#connection.getId().value")
    fun getTopicMetrics(connection: Connection): TopicMetricsDto {
        return jmxMetricsCollector.collectTopicMetrics(connection)
    }

    @Cacheable("partitionMetrics", key = "#connection.getId().value")
    fun getPartitionMetrics(connection: Connection): PartitionMetricsDto {
        return jmxMetricsCollector.collectPartitionMetrics(connection)
    }

    fun getAllMetrics(connection: Connection): CachedMetrics {
        val connectionId = connection.getId().value
        val cached = metricsCache[connectionId]

        if (cached != null &&
            java.time.Duration.between(cached.timestamp, LocalDateTime.now()).seconds < 30) {
            return cached
        }

        val newMetrics = CachedMetrics(
            brokerMetrics = jmxMetricsCollector.collectBrokerMetrics(connection),
            performanceMetrics = jmxMetricsCollector.collectPerformanceMetrics(connection),
            topicMetrics = jmxMetricsCollector.collectTopicMetrics(connection),
            partitionMetrics = jmxMetricsCollector.collectPartitionMetrics(connection),
            timestamp = LocalDateTime.now()
        )

        metricsCache[connectionId] = newMetrics
        return newMetrics
    }

    @Scheduled(fixedRate = 30000)
    @CacheEvict(value = ["brokerMetrics", "performanceMetrics", "topicMetrics", "partitionMetrics"], allEntries = true)
    fun evictCache() {
        val cutoffTime = LocalDateTime.now().minusMinutes(5)
        metricsCache.entries.removeIf { (_, cachedMetrics) ->
            cachedMetrics.timestamp.isBefore(cutoffTime)
        }
    }

    fun clearCacheForConnection(connectionId: String) {
        metricsCache.remove(connectionId)
    }
}