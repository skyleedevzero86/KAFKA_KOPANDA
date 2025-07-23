package com.sleekydz86.kopanda.application.dto

import java.time.LocalDateTime

data class ConnectionDto(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val sslEnabled: Boolean,
    val saslEnabled: Boolean,
    val username: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastConnected: LocalDateTime?
)