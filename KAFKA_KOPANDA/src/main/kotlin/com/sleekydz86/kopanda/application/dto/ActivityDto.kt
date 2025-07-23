package com.sleekydz86.kopanda.application.dto

import java.time.LocalDateTime

data class ActivityDto(
    val id: String,
    val type: ActivityType,
    val title: String,
    val message: String,
    val connectionId: String? = null,
    val topicName: String? = null,
    val timestamp: LocalDateTime,
    val icon: String
)