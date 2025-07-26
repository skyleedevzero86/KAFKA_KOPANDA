package com.sleekydz86.kopanda.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

data class CreateConnectionRequest(
    @field:NotBlank(message = "연결 이름은 필수입니다")
    val name: String,

    @field:NotBlank(message = "호스트는 필수입니다")
    val host: String,

    @field:Min(value = 1, message = "포트는 1-65535 사이여야 합니다")
    @field:Max(value = 65535, message = "포트는 1-65535 사이여야 합니다")
    val port: Int,

    val sslEnabled: Boolean = false,
    val saslEnabled: Boolean = false,
    val username: String? = null,
    val password: String? = null
)