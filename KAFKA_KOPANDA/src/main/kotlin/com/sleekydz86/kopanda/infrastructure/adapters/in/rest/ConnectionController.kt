package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.common.ConnectionTestResult
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.*
import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.shared.domain.DomainException
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
@RequestMapping("/api/connections")
@Tag(name = "Connection Management", description = "Kafka 연결 관리 API")
class ConnectionController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @GetMapping
    @Operation(
        summary = "연결 목록 조회",
        description = "모든 Kafka 연결 목록을 조회합니다."
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

    @GetMapping("/{connectionId}")
    @Operation(
        summary = "연결 상세 조회",
        description = "특정 Kafka 연결의 상세 정보를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 상세 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getConnection(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<ConnectionDto> {
        return try {
            val connection = connectionManagementUseCase.getConnection(connectionId)
            ResponseEntity.ok(connection)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @Operation(
        summary = "연결 생성",
        description = "새로운 Kafka 연결을 생성합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
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
        return ResponseEntity.status(201).body(connection)
    }

    @PutMapping("/{connectionId}")
    @Operation(
        summary = "연결 수정",
        description = "기존 Kafka 연결을 수정합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 수정 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 데이터"
            )
        ]
    )
    suspend fun updateConnection(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String,
        @Parameter(description = "연결 수정 요청", required = true)
        @RequestBody request: UpdateConnectionRequest
    ): ResponseEntity<ConnectionDto> {
        return try {
            val connection = connectionManagementUseCase.updateConnection(connectionId, request)
            ResponseEntity.ok(connection)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{connectionId}")
    @Operation(
        summary = "연결 삭제",
        description = "Kafka 연결을 삭제합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "연결 삭제 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun deleteConnection(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<Unit> {
        return try {
            connectionManagementUseCase.deleteConnection(connectionId)
            ResponseEntity.noContent().build()
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
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

    @GetMapping("/{connectionId}/status")
    @Operation(
        summary = "연결 상태 조회",
        description = "특정 연결의 현재 상태를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 상태 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionStatus::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getConnectionStatus(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<ConnectionStatus> {
        return try {
            val status = connectionManagementUseCase.getConnectionStatus(connectionId)
            ResponseEntity.ok(status)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
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
        return try {
            val topics = kafkaManagementUseCase.getTopics(connectionId)
            ResponseEntity.ok(topics)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
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
        return try {
            val metrics = kafkaManagementUseCase.getMetrics(connectionId)
            ResponseEntity.ok(metrics)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{connectionId}/health")
    @Operation(
        summary = "연결 상태 점검",
        description = "특정 연결의 상태를 점검합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 상태 점검 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionHealthDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getConnectionHealth(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<ConnectionHealthDto> {
        return try {
            val health = connectionManagementUseCase.getConnectionHealth(connectionId)
            ResponseEntity.ok(health)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{connectionId}/ping")
    @Operation(
        summary = "연결 핑",
        description = "특정 연결에 핑을 보냅니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "핑 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PingResultDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun pingConnection(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String
    ): ResponseEntity<PingResultDto> {
        return try {
            val pingResult = connectionManagementUseCase.pingConnection(connectionId)
            ResponseEntity.ok(pingResult)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{connectionId}/history")
    @Operation(
        summary = "연결 히스토리 조회",
        description = "특정 연결의 히스토리를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 히스토리 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ConnectionHistoryDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음"
            )
        ]
    )
    suspend fun getConnectionHistory(
        @Parameter(description = "연결 ID", required = true)
        @PathVariable connectionId: String,
        @Parameter(description = "조회할 히스토리 개수", required = false)
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ConnectionHistoryDto>> {
        return try {
            val history = connectionManagementUseCase.getConnectionHistory(connectionId, limit)
            ResponseEntity.ok(history)
        } catch (e: DomainException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/refresh-status")
    @Operation(
        summary = "모든 연결 상태 새로고침",
        description = "모든 연결의 상태를 새로고침합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상태 새로고침 완료"
            )
        ]
    )
    suspend fun refreshAllConnectionStatuses(): ResponseEntity<Unit> {
        connectionManagementUseCase.refreshAllConnectionStatuses()
        return ResponseEntity.ok().build()
    }
}