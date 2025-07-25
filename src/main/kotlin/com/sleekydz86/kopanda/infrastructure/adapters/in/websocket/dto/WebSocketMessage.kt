package com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.dto

import java.time.LocalDateTime

data class WebSocketMessage<T>(
    val type: String,
    val data: T,
    val timestamp: LocalDateTime
)