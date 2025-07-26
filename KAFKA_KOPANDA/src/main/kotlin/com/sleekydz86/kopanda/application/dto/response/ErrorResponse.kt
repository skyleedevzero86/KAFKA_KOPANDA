package com.sleekydz86.kopanda.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "API 오류 응답")
data class ErrorResponse(
    @field:Schema(description = "오류 발생 시간", example = "2025-07-26T15:30:00")
    val timestamp: String,

    @field:Schema(description = "HTTP 상태 코드", example = "400")
    val status: Int,

    @field:Schema(description = "오류 유형", example = "Bad Request")
    val error: String,

    @field:Schema(description = "오류 메시지", example = "잘못된 요청 파라미터입니다.")
    val message: String,

    @field:Schema(description = "요청 경로", example = "/api/activities")
    val path: String
)