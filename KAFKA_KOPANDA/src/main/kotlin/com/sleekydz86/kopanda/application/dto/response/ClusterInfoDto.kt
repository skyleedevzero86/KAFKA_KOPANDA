package com.sleekydz86.kopanda.application.dto.response

import java.time.LocalDateTime

data class ClusterInfoDto(
    val clusterId: String,
    val controllerId: Int,
    val totalBrokers: Int,
    val activeBrokers: Int,
    val totalTopics: Int,
    val totalPartitions: Int,
    val version: String?,
    val lastUpdated: LocalDateTime
)