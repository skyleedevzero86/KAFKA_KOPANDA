package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.*
import com.sleekydz86.kopanda.application.dto.common.ConnectionTestResult
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto
import com.sleekydz86.kopanda.application.dto.response.ConsumerGroupDto
import com.sleekydz86.kopanda.application.dto.response.KafkaMetricsDto
import com.sleekydz86.kopanda.application.dto.response.TopicDto
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/connections")
@Tag(name = "Connection Management", description = "Kafka 연결 관리 API")
class ConnectionController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @GetMapping
    @Operation(
        summary = "연결 목록 조회",
        description = "등록된 모든 Kafka 연결 목록을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 목록 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionDto::class)
                )]
            )
        ]
    )
    suspend fun getConnections(): ResponseEntity<List<ConnectionDto>> {
        val connections = connectionManagementUseCase.getConnections()
        return ResponseEntity.ok(connections)
    }

    @PostMapping
    @Operation(
        summary = "연결 생성",
        description = "새로운 Kafka 연결을 생성합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 생성 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 데이터"
            )
        ]
    )
    suspend fun createConnection(
        @Parameter(description = "연결 생성 요청", required = true)
        @RequestBody request: CreateConnectionRequest
    ): ResponseEntity<ConnectionDto> {
        val connection = connectionManagementUseCase.createConnection(request)
        return ResponseEntity.ok(connection)
    }

    @PostMapping("/test")
    @Operation(
        summary = "연결 테스트",
        description = "Kafka 연결을 테스트합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 테스트 완료",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionTestResult::class)
                )]
            )
        ]
    )
    suspend fun testConnection(
        @Parameter(description = "연결 테스트 요청", required = true)
        @RequestBody request: CreateConnectionRequest
    ): ResponseEntity<ConnectionTestResult> {
        val result = connectionManagementUseCase.testConnection(request)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{connectionId}/topics")
    @Operation(
        summary = "토픽 목록 조회",
        description = "특정 연결의 토픽 목록을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토픽 목록 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TopicDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getTopics(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<List<TopicDto>> {
        val topics = kafkaManagementUseCase.getTopics(connectionId)
        return ResponseEntity.ok(topics)
    }

    @GetMapping("/{connectionId}/metrics")
    @Operation(
        summary = "메트릭 조회",
        description = "특정 연결의 Kafka 메트릭을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "메트릭 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = KafkaMetricsDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getMetrics(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<KafkaMetricsDto> {
        val metrics = kafkaManagementUseCase.getMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/{connectionId}/consumer-groups")
    @Operation(
        summary = "컨슈머 그룹 조회",
        description = "특정 연결의 컨슈머 그룹 목록을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "컨슈머 그룹 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConsumerGroupDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getConsumerGroups(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<List<ConsumerGroupDto>> {
        val groups = kafkaManagementUseCase.getConsumerGroups(connectionId)
        return ResponseEntity.ok(groups)
    }
}