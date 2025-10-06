package com.sleekydz86.kopanda.application.services

import com.sleekydz86.kopanda.application.dto.response.AlertDto
import com.sleekydz86.kopanda.application.ports.`in`.AlertManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.AlertRepository
import com.sleekydz86.kopanda.domain.entities.Alert
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity as DomainIssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType as DomainIssueType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) : AlertManagementUseCase {

    override fun getActiveAlerts(connectionId: String?): List<AlertDto> {
        return alertRepository.findActiveAlerts(connectionId).map { it.toDto() }
    }

    override fun getAlertsBySeverity(severity: String): List<AlertDto> {
        val domainSeverity = DomainIssueSeverity.valueOf(severity)
        return alertRepository.findBySeverity(domainSeverity).map { it.toDto() }
    }

    override fun acknowledgeAlert(alertId: String, acknowledgedBy: String): AlertDto {
        val alert = alertRepository.findById(alertId)
            ?: throw IllegalArgumentException("Alert not found with id: $alertId")

        alert.acknowledge(acknowledgedBy)
        val savedAlert = alertRepository.save(alert)
        return savedAlert.toDto()
    }

    @Transactional
    override fun clearAllAlerts(connectionId: String?) {
        alertRepository.clearAllAlerts(connectionId)
    }

    override suspend fun checkAndCreateAlerts(connectionId: String) {
        checkTopicHealth(connectionId)
        checkConsumerLag(connectionId)
    }

    override fun getAlertById(id: String): AlertDto {
        val alert = alertRepository.findById(id)
            ?: throw IllegalArgumentException("Alert not found with id: $id")
        return alert.toDto()
    }

    suspend fun checkTopicHealth(connectionId: String) {
        try {
            val topics = kafkaManagementUseCase.getTopics(connectionId)

            topics.forEach { topic ->
                if (topic.partitionCount < 3) {
                    createAlert(
                        type = DomainIssueType.UNDER_REPLICATED,
                        severity = DomainIssueSeverity.MEDIUM,
                        title = "복제 부족 파티션 감지",
                        message = "토픽 '${topic.name}'에 복제 부족 파티션이 있습니다. 파티션 수: ${topic.partitionCount}",
                        connectionId = connectionId,
                        topicName = topic.name
                    )
                }

                if (!topic.isHealthy) {
                    createAlert(
                        type = DomainIssueType.OFFLINE_PARTITION,
                        severity = DomainIssueSeverity.HIGH,
                        title = "비정상 토픽 감지",
                        message = "토픽 '${topic.name}'이 비정상 상태입니다. 즉시 확인이 필요합니다.",
                        connectionId = connectionId,
                        topicName = topic.name
                    )
                }

                if (topic.messageCount == 0L) {
                    createAlert(
                        type = DomainIssueType.UNDER_REPLICATED,
                        severity = DomainIssueSeverity.LOW,
                        title = "비활성 토픽 감지",
                        message = "토픽 '${topic.name}'에 메시지가 없습니다. 토픽이 사용되지 않고 있을 수 있습니다.",
                        connectionId = connectionId,
                        topicName = topic.name
                    )
                }
            }
        } catch (e: Exception) {
            createAlert(
                type = DomainIssueType.CONNECTION_ERROR,
                severity = DomainIssueSeverity.HIGH,
                title = "토픽 헬스 체크 오류",
                message = "토픽 헬스 체크 중 오류가 발생했습니다: ${e.message}",
                connectionId = connectionId
            )
        }
    }

    suspend fun checkConsumerLag(connectionId: String) {
        try {
            val consumerGroups = kafkaManagementUseCase.getConsumerGroups(connectionId)

            consumerGroups.forEach { group ->
                if (group.state != "Stable") {
                    createAlert(
                        type = DomainIssueType.CONSUMER_LAG,
                        severity = DomainIssueSeverity.MEDIUM,
                        title = "컨슈머 그룹 문제 감지",
                        message = "컨슈머 그룹 '${group.groupId}'이 안정적이지 않습니다. 상태: ${group.state}",
                        connectionId = connectionId
                    )
                }

                if (group.memberCount == 0) {
                    createAlert(
                        type = DomainIssueType.CONSUMER_LAG,
                        severity = DomainIssueSeverity.LOW,
                        title = "비활성 컨슈머 그룹",
                        message = "컨슈머 그룹 '${group.groupId}'에 활성 멤버가 없습니다.",
                        connectionId = connectionId
                    )
                }
            }
        } catch (e: Exception) {
            createAlert(
                type = DomainIssueType.CONNECTION_ERROR,
                severity = DomainIssueSeverity.MEDIUM,
                title = "컨슈머 랙 체크 오류",
                message = "컨슈머 랙 체크 중 오류가 발생했습니다: ${e.message}",
                connectionId = connectionId
            )
        }
    }

    private fun createAlert(
        type: DomainIssueType,
        severity: DomainIssueSeverity,
        title: String,
        message: String,
        connectionId: String?,
        topicName: String? = null,
        partitionNumber: Int? = null
    ) {
        try {
            val existingAlerts = alertRepository.findByTypeAndConnectionAndTopic(
                type = type,
                connectionId = connectionId ?: "",
                topicName = topicName ?: ""
            )

            if (existingAlerts.any { !it.isAcknowledged }) {
                return
            }

            val alert = Alert.create(
                type = type,
                severity = severity,
                title = title,
                message = message,
                connectionId = connectionId,
                topicName = topicName,
                partitionNumber = partitionNumber
            )

            val savedAlert = alertRepository.save(alert)
            println("Alert created successfully: ${savedAlert.id} - ${savedAlert.title}")
        } catch (e: Exception) {
            println("Failed to create alert: ${e.message}")
            e.printStackTrace()
        }
    }
}