package com.sleekydz86.kopanda.application.ports.out

import com.sleekydz86.kopanda.domain.entities.Alert
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType

interface AlertRepository {
    fun save(alert: Alert): Alert
    fun findById(id: String): Alert?
    fun findActiveAlerts(connectionId: String? = null): List<Alert>
    fun findBySeverity(severity: IssueSeverity): List<Alert>
    fun findByTypeAndConnectionAndTopic(
        type: IssueType,
        connectionId: String,
        topicName: String
    ): List<Alert>
    fun clearAllAlerts(connectionId: String? = null)
    fun deleteOldAlerts(daysToKeep: Int)
}