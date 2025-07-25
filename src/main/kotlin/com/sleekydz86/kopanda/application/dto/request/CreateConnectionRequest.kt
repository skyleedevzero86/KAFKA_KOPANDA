package com.sleekydz86.kopanda.application.dto.request

data class CreateConnectionRequest(
    val name: String,
    val host: String,
    val port: Int,
    val sslEnabled: Boolean = false,
    val saslEnabled: Boolean = false,
    val username: String? = null,
    val password: String? = null
)