package com.sleekydz86.kopanda.application.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateConnectionRequest(
    val name: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val sslEnabled: Boolean? = null,
    val saslEnabled: Boolean? = null,
    val username: String? = null,
    val password: String? = null
)