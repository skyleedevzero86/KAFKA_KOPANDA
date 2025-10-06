package com.sleekydz86.kopanda.application.dto.response

import com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity
import com.sleekydz86.kopanda.domain.valueobjects.common.IssueType


data class TopicIssueDto(
    val type: IssueType,
    val severity: IssueSeverity,
    val description: String,
    val partitionId: Int? = null
)