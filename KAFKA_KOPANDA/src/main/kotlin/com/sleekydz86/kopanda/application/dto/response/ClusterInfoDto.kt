package com.sleekydz86.kopanda.application.dto.response

data class ClusterInfoDto(
    val clusterId: String,
    val controllerId: Int,
    val totalBrokers: Int,
    val activeBrokers: Int,
    val totalTopics: Int,
    val totalPartitions: Int,
    val version: String,
    val lastUpdated: java.time.LocalDateTime
)