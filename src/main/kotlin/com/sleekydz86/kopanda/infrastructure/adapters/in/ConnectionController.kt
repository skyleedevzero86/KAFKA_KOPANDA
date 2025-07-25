package com.sleekydz86.kopanda.infrastructure.adapters.`in`

import com.sleekydz86.kopanda.application.dto.*
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/connections")
class ConnectionController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @GetMapping
    suspend fun getConnections(): ResponseEntity<List<ConnectionDto>> {
        val connections = connectionManagementUseCase.getConnections()
        return ResponseEntity.ok(connections)
    }

    @PostMapping
    suspend fun createConnection(@RequestBody request: CreateConnectionRequest): ResponseEntity<ConnectionDto> {
        val connection = connectionManagementUseCase.createConnection(request)
        return ResponseEntity.ok(connection)
    }

    @PostMapping("/test")
    suspend fun testConnection(@RequestBody request: CreateConnectionRequest): ResponseEntity<ConnectionTestResult> {
        val result = connectionManagementUseCase.testConnection(request)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{connectionId}/topics")
    suspend fun getTopics(@PathVariable connectionId: String): ResponseEntity<List<TopicDto>> {
        val topics = kafkaManagementUseCase.getTopics(connectionId)
        return ResponseEntity.ok(topics)
    }

    @GetMapping("/{connectionId}/metrics")
    suspend fun getMetrics(@PathVariable connectionId: String): ResponseEntity<KafkaMetricsDto> {
        val metrics = kafkaManagementUseCase.getMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/{connectionId}/consumer-groups")
    suspend fun getConsumerGroups(@PathVariable connectionId: String): ResponseEntity<List<ConsumerGroupDto>> {
        val groups = kafkaManagementUseCase.getConsumerGroups(connectionId)
        return ResponseEntity.ok(groups)
    }
}