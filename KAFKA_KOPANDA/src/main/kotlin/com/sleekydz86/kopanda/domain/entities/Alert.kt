package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType
import com.sleekydz86.kopanda.shared.domain.AggregateRoot
import java.time.LocalDateTime

class Alert(
    val type: IssueType,
    val severity: IssueSeverity,
    val title: String,
    val message: String,
    val connectionId: String?,
    val topicName: String?,
    val partitionNumber: Int?,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    var isAcknowledged: Boolean = false,
    var acknowledgedAt: LocalDateTime? = null,
    var acknowledgedBy: String? = null
) : AggregateRoot() {

    val id: String = java.util.UUID.randomUUID().toString()

    fun acknowledge(acknowledgedBy: String) {
        this.isAcknowledged = true
        this.acknowledgedAt = LocalDateTime.now()
        this.acknowledgedBy = acknowledgedBy
    }

    fun isActive(): Boolean = !isAcknowledged

    fun toDto() = com.sleekydz86.kopanda.application.dto.response.AlertDto(
        id = id,
        type = type,
        severity = severity,
        title = title,
        message = message,
        connectionId = connectionId,
        topicName = topicName,
        partitionNumber = partitionNumber,
        timestamp = timestamp,
        isAcknowledged = isAcknowledged,
        acknowledgedAt = acknowledgedAt,
        acknowledgedBy = acknowledgedBy
    )

    companion object {
        fun create(
            type: IssueType,
            severity: IssueSeverity,
            title: String,
            message: String,
            connectionId: String? = null,
            topicName: String? = null,
            partitionNumber: Int? = null
        ): Alert {
            return Alert(
                type = type,
                severity = severity,
                title = title,
                message = message,
                connectionId = connectionId,
                topicName = topicName,
                partitionNumber = partitionNumber
            )
        }
    }
}