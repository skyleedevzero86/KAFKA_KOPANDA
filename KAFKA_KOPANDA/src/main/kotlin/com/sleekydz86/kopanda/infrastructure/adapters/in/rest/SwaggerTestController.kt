package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/test")
@Tag(name = "Test", description = "API 테스트용 엔드포인트")
class SwaggerTestController {

    @GetMapping("/health")
    @Operation(
        summary = "헬스 체크",
        description = "API 서버의 상태를 확인합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "서버 정상 동작",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = HealthResponse::class)
                )]
            )
        ]
    )
    fun healthCheck(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                message = "Kafka Kopanda API is running",
                timestamp = java.time.LocalDateTime.now()
            )
        )
    }

    @PostMapping("/echo")
    @Operation(
        summary = "에코 테스트",
        description = "전송된 데이터를 그대로 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "에코 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = EchoResponse::class)
                )]
            )
        ]
    )
    fun echo(@RequestBody request: EchoRequest): ResponseEntity<EchoResponse> {
        return ResponseEntity.ok(
            EchoResponse(
                message = request.message,
                timestamp = java.time.LocalDateTime.now(),
                receivedData = request.data
            )
        )
    }
}

@Schema(description = "헬스 체크 응답")
data class HealthResponse(
    @field:Schema(description = "서버 상태", example = "UP")
    val status: String,

    @field:Schema(description = "상태 메시지", example = "Kafka Kopanda API is running")
    val message: String,

    @field:Schema(description = "응답 시간", example = "2024-01-01T10:00:00")
    val timestamp: java.time.LocalDateTime
)

@Schema(description = "에코 요청")
data class EchoRequest(
    @field:Schema(description = "메시지", example = "Hello World")
    val message: String,

    @field:Schema(description = "데이터", example = "{\"key\": \"value\"}")
    val data: Map<String, Any> = emptyMap()
)

@Schema(description = "에코 응답")
data class EchoResponse(
    @field:Schema(description = "메시지", example = "Hello World")
    val message: String,

    @field:Schema(description = "응답 시간", example = "2024-01-01T10:00:00")
    val timestamp: java.time.LocalDateTime,

    @field:Schema(description = "수신된 데이터", example = "{\"key\": \"value\"}")
    val receivedData: Map<String, Any>
)