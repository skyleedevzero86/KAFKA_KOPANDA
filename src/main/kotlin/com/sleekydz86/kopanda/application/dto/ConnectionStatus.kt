package com.sleekydz86.kopanda.application.dto

import java.time.LocalDateTime

data class ConnectionStatus(
    val connectionId: String,
    val status: ConnectionStatusType,
    val lastChecked: LocalDateTime,
    val errorMessage: String? = null,
    val brokerCount: Int? = null,
    val topicCount: Int? = null,
    val latency: Long? = null
)