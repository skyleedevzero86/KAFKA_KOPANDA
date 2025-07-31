package com.sleekydz86.kopanda.infrastructure.adapters.`in`.scheduler

import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MonitoringScheduler(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    private val logger = LoggerFactory.getLogger(MonitoringScheduler::class.java)

    @Scheduled(fixedRate = 30000)
    suspend fun monitorConnections() {
        logger.info("Starting scheduled connection monitoring")
        try {
            connectionManagementUseCase.refreshAllConnectionStatuses()
            logger.info("Connection monitoring completed successfully")
        } catch (e: Exception) {
            logger.error("Error during connection monitoring", e)
        }
    }

    @Scheduled(fixedRate = 60000)
    suspend fun monitorTopicsHealth() {
        logger.info("Starting scheduled topic health monitoring")
        try {
            val connections = connectionManagementUseCase.getConnections()
            connections.forEach { connection ->
                try {
                    val topicsHealth = kafkaManagementUseCase.getAllTopicsHealth(connection.id)
                    val unhealthyTopics = topicsHealth.filter { !it.isHealthy }

                    if (unhealthyTopics.isNotEmpty()) {
                        logger.warn("Found ${unhealthyTopics.size} unhealthy topics in connection ${connection.id}")
                        unhealthyTopics.forEach { topic ->
                            logger.warn("Unhealthy topic: ${topic.topicName}, Health score: ${topic.healthScore}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error monitoring topics for connection ${connection.id}", e)
                }
            }
            logger.info("Topic health monitoring completed successfully")
        } catch (e: Exception) {
            logger.error("Error during topic health monitoring", e)
        }
    }

    @Scheduled(fixedRate = 120000)
    suspend fun monitorConsumerGroups() {
        logger.info("Starting scheduled consumer group monitoring")
        try {
            val connections = connectionManagementUseCase.getConnections()
            connections.forEach { connection ->
                try {
                    val consumerGroupMetrics = kafkaManagementUseCase.getAllConsumerGroupMetrics(connection.id)
                    val highLagGroups = consumerGroupMetrics.filter { it.totalLag > 1000 }

                    if (highLagGroups.isNotEmpty()) {
                        logger.warn("Found ${highLagGroups.size} consumer groups with high lag in connection ${connection.id}")
                        highLagGroups.forEach { group ->
                            logger.warn("High lag group: ${group.groupId}, Total lag: ${group.totalLag}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error monitoring consumer groups for connection ${connection.id}", e)
                }
            }
            logger.info("Consumer group monitoring completed successfully")
        } catch (e: Exception) {
            logger.error("Error during consumer group monitoring", e)
        }
    }
}