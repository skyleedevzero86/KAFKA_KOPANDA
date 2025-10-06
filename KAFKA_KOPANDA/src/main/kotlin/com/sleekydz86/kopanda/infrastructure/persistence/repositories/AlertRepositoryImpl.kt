package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.application.ports.out.AlertRepository
import com.sleekydz86.kopanda.domain.entities.Alert
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType
import com.sleekydz86.kopanda.infrastructure.persistence.entities.AlertEntity
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AlertRepositoryImpl(
    private val alertJpaRepository: AlertJpaRepository
) : AlertRepository {

    override fun save(alert: Alert): Alert {
        val entity = alert.toEntity()
        val savedEntity = alertJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: String): Alert? {
        val entity = alertJpaRepository.findById(id).orElse(null)
        return entity?.toDomain()
    }

    override fun findActiveAlerts(connectionId: String?): List<Alert> {
        val entities = if (connectionId != null) {
            alertJpaRepository.findByConnectionIdAndIsAcknowledgedFalse(connectionId)
        } else {
            alertJpaRepository.findByIsAcknowledgedFalse()
        }
        return entities.map { it.toDomain() }
    }

    override fun findBySeverity(severity: IssueSeverity): List<Alert> {
        val entities = alertJpaRepository.findBySeverity(severity.name)
        return entities.map { it.toDomain() }
    }

    override fun findByTypeAndConnectionAndTopic(
        type: IssueType,
        connectionId: String,
        topicName: String
    ): List<Alert> {
        val entities = alertJpaRepository.findByTypeAndConnectionIdAndTopicName(
            type.name, connectionId, topicName
        )
        return entities.map { it.toDomain() }
    }

    override fun clearAllAlerts(connectionId: String?) {
        if (connectionId != null) {
            alertJpaRepository.deleteByConnectionId(connectionId)
        } else {
            alertJpaRepository.deleteAll()
        }
    }

    override fun deleteOldAlerts(daysToKeep: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(daysToKeep.toLong())
        alertJpaRepository.deleteByTimestampBefore(cutoffDate)
    }
}

fun Alert.toEntity(): AlertEntity {
    return AlertEntity(
        id = this.id,
        type = this.type.name,
        severity = this.severity.name,
        title = this.title,
        message = this.message,
        connectionId = this.connectionId,
        topicName = this.topicName,
        partitionNumber = this.partitionNumber,
        timestamp = this.timestamp,
        isAcknowledged = this.isAcknowledged,
        acknowledgedAt = this.acknowledgedAt,
        acknowledgedBy = this.acknowledgedBy
    )
}

fun AlertEntity.toDomain(): Alert {
    return Alert.create(
        type = IssueType.valueOf(this.type),
        severity = IssueSeverity.valueOf(this.severity),
        title = this.title,
        message = this.message,
        connectionId = this.connectionId,
        topicName = this.topicName,
        partitionNumber = this.partitionNumber
    ).apply {
        if (this@toDomain.isAcknowledged) {
            this.acknowledge(this@toDomain.acknowledgedBy ?: "system")
        }
    }
}