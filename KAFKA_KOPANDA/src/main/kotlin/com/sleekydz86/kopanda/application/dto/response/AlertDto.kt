package com.sleekydz86.kopanda.application.dto.response

import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType
import java.time.LocalDateTime

data class AlertDto(
    val id: String,
    val type: IssueType,
    val severity: IssueSeverity,
    val title: String,
    val message: String,
    val connectionId: String? = null,
    val topicName: String? = null,
    val partitionNumber: Int? = null,
    val timestamp: LocalDateTime,
    val isAcknowledged: Boolean = false,
    val acknowledgedAt: LocalDateTime? = null,
    val acknowledgedBy: String? = null
)