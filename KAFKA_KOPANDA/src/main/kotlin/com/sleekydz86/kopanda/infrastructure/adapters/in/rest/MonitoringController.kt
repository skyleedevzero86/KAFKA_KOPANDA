package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto
import com.sleekydz86.kopanda.application.dto.response.*
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/monitoring")
class MonitoringController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @GetMapping("/connections/{connectionId}/detailed-metrics")
    suspend fun getDetailedMetrics(@PathVariable connectionId: String): ResponseEntity<DetailedMetricsDto> {
        val metrics = kafkaManagementUseCase.getDetailedMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/topics/health")
    suspend fun getAllTopicsHealth(@PathVariable connectionId: String): ResponseEntity<List<TopicHealthDto>> {
        val health = kafkaManagementUseCase.getAllTopicsHealth(connectionId)
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/topics/{topicName}/health")
    suspend fun getTopicHealth(
        @PathVariable connectionId: String,
        @PathVariable topicName: String
    ): ResponseEntity<TopicHealthDto> {
        val health = kafkaManagementUseCase.getTopicHealth(connectionId, topicName)
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/consumer-groups/metrics")
    suspend fun getAllConsumerGroupMetrics(@PathVariable connectionId: String): ResponseEntity<List<ConsumerGroupMetricsDto>> {
        val metrics = kafkaManagementUseCase.getAllConsumerGroupMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/consumer-groups/{groupId}/metrics")
    suspend fun getConsumerGroupMetrics(
        @PathVariable connectionId: String,
        @PathVariable groupId: String
    ): ResponseEntity<ConsumerGroupMetricsDto> {
        val metrics = kafkaManagementUseCase.getConsumerGroupMetrics(connectionId, groupId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/performance")
    suspend fun getPerformanceMetrics(@PathVariable connectionId: String): ResponseEntity<PerformanceMetricsDto> {
        val metrics = kafkaManagementUseCase.getPerformanceMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/topics/{topicName}/partitions/{partitionNumber}")
    suspend fun getPartitionDetails(
        @PathVariable connectionId: String,
        @PathVariable topicName: String,
        @PathVariable partitionNumber: Int
    ): ResponseEntity<PartitionDetailDto> {
        val details = kafkaManagementUseCase.getPartitionDetails(connectionId, topicName, partitionNumber)
        return ResponseEntity.ok(details)
    }

    @GetMapping("/connections/{connectionId}/topics/{topicName}/partitions/{partitionNumber}/offset")
    suspend fun getOffsetInfo(
        @PathVariable connectionId: String,
        @PathVariable topicName: String,
        @PathVariable partitionNumber: Int
    ): ResponseEntity<OffsetInfoDto> {
        val offsetInfo = kafkaManagementUseCase.getOffsetInfo(connectionId, topicName, partitionNumber)
        return ResponseEntity.ok(offsetInfo)
    }

    @PostMapping("/connections/{connectionId}/topics/{topicName}/partitions/{partitionNumber}/offset")
    suspend fun setOffset(
        @PathVariable connectionId: String,
        @PathVariable topicName: String,
        @PathVariable partitionNumber: Int,
        @RequestParam offset: Long
    ): ResponseEntity<Map<String, Any>> {
        val success = kafkaManagementUseCase.setOffset(connectionId, topicName, partitionNumber, offset)
        return ResponseEntity.ok(mapOf(
            "success" to success,
            "message" to if (success) "Offset set successfully" else "Failed to set offset"
        ))
    }

    @GetMapping("/connections/{connectionId}/cluster-info")
    suspend fun getClusterInfo(@PathVariable connectionId: String): ResponseEntity<ClusterInfoDto> {
        val clusterInfo = kafkaManagementUseCase.getClusterInfo(connectionId)
        return ResponseEntity.ok(clusterInfo)
    }

    @GetMapping("/connections/health")
    suspend fun getAllConnectionsHealth(): ResponseEntity<List<ConnectionHealthDto>> {
        val health = connectionManagementUseCase.getAllConnectionsHealth()
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/health")
    suspend fun getConnectionHealth(@PathVariable connectionId: String): ResponseEntity<ConnectionHealthDto> {
        val health = connectionManagementUseCase.getConnectionHealth(connectionId)
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/metrics")
    suspend fun getConnectionMetrics(@PathVariable connectionId: String): ResponseEntity<ConnectionMetricsDto> {
        val metrics = connectionManagementUseCase.getConnectionMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @PostMapping("/connections/{connectionId}/ping")
    suspend fun pingConnection(@PathVariable connectionId: String): ResponseEntity<PingResultDto> {
        val pingResult = connectionManagementUseCase.pingConnection(connectionId)
        return ResponseEntity.ok(pingResult)
    }

    @GetMapping("/connections/{connectionId}/history")
    suspend fun getConnectionHistory(
        @PathVariable connectionId: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ConnectionHistoryDto>> {
        val history = connectionManagementUseCase.getConnectionHistory(connectionId, limit)
        return ResponseEntity.ok(history)
    }

    @GetMapping("/dashboard/{connectionId}")
    suspend fun getDashboard(@PathVariable connectionId: String): ResponseEntity<Map<String, Any>> {
        val basicMetrics = kafkaManagementUseCase.getMetrics(connectionId)
        val detailedMetrics = kafkaManagementUseCase.getDetailedMetrics(connectionId)
        val topicsHealth = kafkaManagementUseCase.getAllTopicsHealth(connectionId)
        val consumerGroupMetrics = kafkaManagementUseCase.getAllConsumerGroupMetrics(connectionId)
        val performanceMetrics = kafkaManagementUseCase.getPerformanceMetrics(connectionId)

        val dashboardData = mapOf(
            "basicMetrics" to basicMetrics,
            "detailedMetrics" to detailedMetrics,
            "topicsHealth" to topicsHealth,
            "consumerGroupMetrics" to consumerGroupMetrics,
            "performanceMetrics" to performanceMetrics,
            "timestamp" to java.time.LocalDateTime.now()
        )

        return ResponseEntity.ok(dashboardData)
    }
}