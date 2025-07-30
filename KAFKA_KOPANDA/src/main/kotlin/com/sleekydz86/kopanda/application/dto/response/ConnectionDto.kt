package com.sleekydz86.kopanda.application.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ConnectionDto(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val sslEnabled: Boolean,
    val saslEnabled: Boolean,
    val username: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val lastConnected: LocalDateTime?
)