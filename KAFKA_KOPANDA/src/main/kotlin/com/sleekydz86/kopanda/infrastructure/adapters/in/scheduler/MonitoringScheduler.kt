package com.sleekydz86.kopanda.infrastructure.adapters.`in`.scheduler

import com.sleekydz86.kopanda.application.ports.`in`.AlertManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.ConnectionRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MonitoringScheduler(
    private val alertManagementUseCase: AlertManagementUseCase,
    private val connectionRepository: ConnectionRepository
) {
    
    private val logger = LoggerFactory.getLogger(MonitoringScheduler::class.java)

    @Scheduled(fixedRate = 30000) // 30초마다 실행
    fun checkAlerts() {
        logger.info("Monitoring scheduler running...")

        runBlocking {
            try {
                val connections = connectionRepository.findAll()
                logger.info("Found ${connections.size} connections to check")
                
                connections.forEach { connection ->
                    logger.debug("Checking alerts for connection: ${connection.name.value}")

                    val connectionId = getConnectionId(connection)
                    if (connectionId != null) {
                        try {
                            alertManagementUseCase.checkAndCreateAlerts(connectionId)
                            logger.debug("Alert check completed for connection: ${connection.name.value}")
                        } catch (e: Exception) {
                            logger.error("Failed to check alerts for connection ${connection.name.value}: ${e.message}", e)
                        }
                    } else {
                        logger.warn("Could not get connection ID for: ${connection.name.value}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in monitoring scheduler: ${e.message}", e)
            }
        }
    }

    private fun getConnectionId(connection: Connection): String? {
        return try {
            val idField = connection.javaClass.getDeclaredField("id")
            idField.isAccessible = true
            val id = idField.get(connection)
            if (id is ConnectionId) {
                id.value
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to get connection ID: ${e.message}", e)
            null
        }
    }
}