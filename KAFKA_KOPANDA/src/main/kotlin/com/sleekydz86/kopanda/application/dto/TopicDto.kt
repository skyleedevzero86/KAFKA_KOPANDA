package com.sleekydz86.kopanda.application.dto

import java.time.LocalDateTime

data class TopicDto(
    val name: String,
    val partitionCount: Int,
    val replicationFactor: Int,
    val messageCount: Long,
    val isInternal: Boolean,
    val isHealthy: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)