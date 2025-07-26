package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.request.PartitionDetailDto
import com.sleekydz86.kopanda.application.dto.response.*
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
@RequestMapping("/monitoring")
@Tag(
    name = "Monitoring Management",
    description = "Kafka 클러스터 및 연결 상태 모니터링을 위한 API 엔드포인트들입니다. 상세 메트릭, 토픽 헬스, 컨슈머 그룹 메트릭, 성능 지표, 파티션 정보, 오프셋 관리, 클러스터 정보, 연결 상태, 핑 테스트, 연결 히스토리, 대시보드 데이터 등을 제공합니다."
)
class MonitoringController(
    private val connectionManagementUseCase: ConnectionManagementUseCase,
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    @GetMapping("/connections/{connectionId}/detailed-metrics")
    @Operation(
        summary = "상세 메트릭 조회",
        description = "지정된 Kafka 연결의 브로커, 토픽, 파티션, 성능 메트릭을 포함한 상세한 모니터링 정보를 조회합니다. 브로커 상태, 토픽별 메시지 처리량, 파티션 복제 상태, 성능 지표 등을 종합적으로 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상세 메트릭 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = DetailedMetricsDto::class,
                            description = "브로커, 토픽, 파티션, 성능 메트릭을 포함한 상세 모니터링 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getDetailedMetrics(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<DetailedMetricsDto> {
        val metrics = kafkaManagementUseCase.getDetailedMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/topics/health")
    @Operation(
        summary = "모든 토픽 헬스 상태 조회",
        description = "지정된 Kafka 연결의 모든 토픽에 대한 헬스 상태를 조회합니다. 각 토픽의 복제 상태, 오프라인 파티션 수, 복제 팩터, 평균 복제 팩터, 마지막 업데이트 시간, 발견된 이슈 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토픽 헬스 상태 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = Array<TopicHealthDto>::class,
                            description = "모든 토픽의 헬스 상태 정보 배열"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getAllTopicsHealth(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<List<TopicHealthDto>> {
        val health = kafkaManagementUseCase.getAllTopicsHealth(connectionId)
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/topics/{topicName}/health")
    @Operation(
        summary = "특정 토픽 헬스 상태 조회",
        description = "지정된 Kafka 연결의 특정 토픽에 대한 헬스 상태를 조회합니다. 토픽의 복제 상태, 오프라인 파티션 수, 복제 팩터, 평균 복제 팩터, 마지막 업데이트 시간, 발견된 이슈 등을 상세히 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토픽 헬스 상태 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = TopicHealthDto::class,
                            description = "특정 토픽의 헬스 상태 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결 또는 토픽을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getTopicHealth(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String,
        @Parameter(
            description = "헬스 상태를 조회할 토픽 이름",
            example = "user-events",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9._-]+$",
                minLength = 1,
                maxLength = 249
            )
        )
        @PathVariable topicName: String
    ): ResponseEntity<TopicHealthDto> {
        val health = kafkaManagementUseCase.getTopicHealth(connectionId, topicName)
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/consumer-groups/metrics")
    @Operation(
        summary = "모든 컨슈머 그룹 메트릭 조회",
        description = "지정된 Kafka 연결의 모든 컨슈머 그룹에 대한 메트릭을 조회합니다. 각 컨슈머 그룹의 상태, 멤버 수, 토픽 수, 총 지연량, 평균 지연량, 최대/최소 지연량, 마지막 커밋 시간, 파티션별 지연량, 멤버별 메트릭 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "컨슈머 그룹 메트릭 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = Array<ConsumerGroupMetricsDto>::class,
                            description = "모든 컨슈머 그룹의 메트릭 정보 배열"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getAllConsumerGroupMetrics(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<List<ConsumerGroupMetricsDto>> {
        val metrics = kafkaManagementUseCase.getAllConsumerGroupMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/consumer-groups/{groupId}/metrics")
    @Operation(
        summary = "특정 컨슈머 그룹 메트릭 조회",
        description = "지정된 Kafka 연결의 특정 컨슈머 그룹에 대한 상세 메트릭을 조회합니다. 컨슈머 그룹의 상태, 멤버 수, 토픽 수, 총 지연량, 평균 지연량, 최대/최소 지연량, 마지막 커밋 시간, 파티션별 지연량, 멤버별 메트릭 등을 상세히 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "컨슈머 그룹 메트릭 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ConsumerGroupMetricsDto::class,
                            description = "특정 컨슈머 그룹의 상세 메트릭 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결 또는 컨슈머 그룹을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getConsumerGroupMetrics(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String,
        @Parameter(
            description = "메트릭을 조회할 컨슈머 그룹 ID",
            example = "user-events-consumer",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9._-]+$",
                minLength = 1,
                maxLength = 255
            )
        )
        @PathVariable groupId: String
    ): ResponseEntity<ConsumerGroupMetricsDto> {
        val metrics = kafkaManagementUseCase.getConsumerGroupMetrics(connectionId, groupId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/performance")
    @Operation(
        summary = "성능 메트릭 조회",
        description = "지정된 Kafka 연결의 성능 메트릭을 조회합니다. 초당 메시지 처리량, 초당 입력/출력 바이트, 초당 요청 수, 평균/최대 요청 지연시간, 활성/총 연결 수 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "성능 메트릭 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = PerformanceMetricsDto::class,
                            description = "Kafka 클러스터의 성능 메트릭 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getPerformanceMetrics(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<PerformanceMetricsDto> {
        val metrics = kafkaManagementUseCase.getPerformanceMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/connections/{connectionId}/topics/{topicName}/partitions/{partitionNumber}")
    @Operation(
        summary = "파티션 상세 정보 조회",
        description = "지정된 Kafka 연결의 특정 토픽과 파티션에 대한 상세 정보를 조회합니다. 파티션 번호, 리더 브로커, 복제본 목록, 동기화된 복제본 목록, 최초/최신 오프셋, 메시지 수, 헬스 상태, 복제 부족 여부, 마지막 업데이트 시간 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "파티션 상세 정보 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = PartitionDetailDto::class,
                            description = "파티션의 상세 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결, 토픽 또는 파티션을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getPartitionDetails(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String,
        @Parameter(
            description = "파티션 정보를 조회할 토픽 이름",
            example = "user-events",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9._-]+$",
                minLength = 1,
                maxLength = 249
            )
        )
        @PathVariable topicName: String,
        @Parameter(
            description = "조회할 파티션 번호",
            example = "0",
            schema = Schema(
                type = "integer",
                minimum = "0",
                maximum = "999999"
            )
        )
        @PathVariable partitionNumber: Int
    ): ResponseEntity<PartitionDetailDto> {
        val details = kafkaManagementUseCase.getPartitionDetails(connectionId, topicName, partitionNumber)
        return ResponseEntity.ok(details)
    }

    @GetMapping("/connections/{connectionId}/topics/{topicName}/partitions/{partitionNumber}/offset")
    @Operation(
        summary = "파티션 오프셋 정보 조회",
        description = "지정된 Kafka 연결의 특정 토픽과 파티션에 대한 오프셋 정보를 조회합니다. 현재 오프셋, 커밋된 오프셋, 끝 오프셋, 지연량, 컨슈머 그룹, 마지막 커밋 시간 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "오프셋 정보 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = OffsetInfoDto::class,
                            description = "파티션의 오프셋 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결, 토픽 또는 파티션을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getOffsetInfo(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String,
        @Parameter(
            description = "오프셋 정보를 조회할 토픽 이름",
            example = "user-events",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9._-]+$",
                minLength = 1,
                maxLength = 249
            )
        )
        @PathVariable topicName: String,
        @Parameter(
            description = "조회할 파티션 번호",
            example = "0",
            schema = Schema(
                type = "integer",
                minimum = "0",
                maximum = "999999"
            )
        )
        @PathVariable partitionNumber: Int
    ): ResponseEntity<OffsetInfoDto> {
        val offsetInfo = kafkaManagementUseCase.getOffsetInfo(connectionId, topicName, partitionNumber)
        return ResponseEntity.ok(offsetInfo)
    }

    @PostMapping("/connections/{connectionId}/topics/{topicName}/partitions/{partitionNumber}/offset")
    @Operation(
        summary = "파티션 오프셋 설정",
        description = "지정된 Kafka 연결의 특정 토픽과 파티션에 대한 오프셋을 설정합니다. 컨슈머 그룹의 커밋된 오프셋을 지정된 값으로 변경하여 메시지 소비 위치를 조정할 수 있습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "오프셋 설정 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            type = "object",
                            description = "오프셋 설정 결과"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결, 토픽 또는 파티션을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun setOffset(
        @Parameter(
            description = "오프셋을 설정할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String,
        @Parameter(
            description = "오프셋을 설정할 토픽 이름",
            example = "user-events",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9._-]+$",
                minLength = 1,
                maxLength = 249
            )
        )
        @PathVariable topicName: String,
        @Parameter(
            description = "오프셋을 설정할 파티션 번호",
            example = "0",
            schema = Schema(
                type = "integer",
                minimum = "0",
                maximum = "999999"
            )
        )
        @PathVariable partitionNumber: Int,
        @Parameter(
            description = "설정할 오프셋 값",
            example = "1000",
            schema = Schema(
                type = "integer",
                minimum = "0"
            )
        )
        @RequestParam offset: Long
    ): ResponseEntity<Map<String, Any>> {
        val success = kafkaManagementUseCase.setOffset(connectionId, topicName, partitionNumber, offset)
        return ResponseEntity.ok(mapOf(
            "success" to success,
            "message" to if (success) "Offset set successfully" else "Failed to set offset"
        ))
    }

    @GetMapping("/connections/{connectionId}/cluster-info")
    @Operation(
        summary = "클러스터 정보 조회",
        description = "지정된 Kafka 연결의 클러스터 정보를 조회합니다. 클러스터 ID, 컨트롤러 ID, 총/활성 브로커 수, 총 토픽 수, 총 파티션 수, 버전, 마지막 업데이트 시간 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "클러스터 정보 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ClusterInfoDto::class,
                            description = "Kafka 클러스터의 기본 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getClusterInfo(
        @Parameter(
            description = "조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<ClusterInfoDto> {
        val clusterInfo = kafkaManagementUseCase.getClusterInfo(connectionId)
        return ResponseEntity.ok(clusterInfo)
    }

    @GetMapping("/connections/health")
    @Operation(
        summary = "모든 연결 헬스 상태 조회",
        description = "시스템에 등록된 모든 Kafka 연결의 헬스 상태를 조회합니다. 각 연결의 헬스 점수, 응답 시간, 오류 수, 성공률, 발견된 이슈 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 헬스 상태 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = Array<ConnectionHealthDto>::class,
                            description = "모든 연결의 헬스 상태 정보 배열"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getAllConnectionsHealth(): ResponseEntity<List<ConnectionHealthDto>> {
        val health = connectionManagementUseCase.getAllConnectionsHealth()
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/health")
    @Operation(
        summary = "특정 연결 헬스 상태 조회",
        description = "지정된 Kafka 연결의 헬스 상태를 조회합니다. 연결의 헬스 점수, 응답 시간, 오류 수, 성공률, 발견된 이슈 등을 상세히 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 헬스 상태 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ConnectionHealthDto::class,
                            description = "특정 연결의 헬스 상태 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getConnectionHealth(
        @Parameter(
            description = "헬스 상태를 조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<ConnectionHealthDto> {
        val health = connectionManagementUseCase.getConnectionHealth(connectionId)
        return ResponseEntity.ok(health)
    }

    @GetMapping("/connections/{connectionId}/metrics")
    @Operation(
        summary = "연결 메트릭 조회",
        description = "지정된 Kafka 연결의 메트릭을 조회합니다. 업타임, 총 요청 수, 성공/실패 요청 수, 평균/최대/최소 응답 시간, 마지막 활동 시간 등을 포함합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 메트릭 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ConnectionMetricsDto::class,
                            description = "연결의 메트릭 정보"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getConnectionMetrics(
        @Parameter(
            description = "메트릭을 조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<ConnectionMetricsDto> {
        val metrics = connectionManagementUseCase.getConnectionMetrics(connectionId)
        return ResponseEntity.ok(metrics)
    }

    @PostMapping("/connections/{connectionId}/ping")
    @Operation(
        summary = "연결 핑 테스트",
        description = "지정된 Kafka 연결에 대한 핑 테스트를 수행합니다. 연결의 응답 시간을 측정하고 연결 상태를 확인합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "핑 테스트 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = PingResultDto::class,
                            description = "핑 테스트 결과"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun pingConnection(
        @Parameter(
            description = "핑 테스트를 수행할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<PingResultDto> {
        val pingResult = connectionManagementUseCase.pingConnection(connectionId)
        return ResponseEntity.ok(pingResult)
    }

    @GetMapping("/connections/{connectionId}/history")
    @Operation(
        summary = "연결 히스토리 조회",
        description = "지정된 Kafka 연결의 히스토리를 조회합니다. 연결 생성, 업데이트, 상태 변경, 오류 발생 등의 이벤트 기록을 시간순으로 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 히스토리 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = Array<ConnectionHistoryDto>::class,
                            description = "연결 히스토리 정보 배열"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getConnectionHistory(
        @Parameter(
            description = "히스토리를 조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String,
        @Parameter(
            description = "조회할 히스토리 개수 (기본값: 10, 최대값: 100)",
            example = "10",
            schema = Schema(
                type = "integer",
                minimum = "1",
                maximum = "100",
                defaultValue = "10"
            )
        )
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ConnectionHistoryDto>> {
        val history = connectionManagementUseCase.getConnectionHistory(connectionId, limit)
        return ResponseEntity.ok(history)
    }

    @GetMapping("/dashboard/{connectionId}")
    @Operation(
        summary = "대시보드 데이터 조회",
        description = "지정된 Kafka 연결에 대한 종합적인 대시보드 데이터를 조회합니다. 기본 메트릭, 상세 메트릭, 토픽 헬스, 컨슈머 그룹 메트릭, 성능 메트릭을 모두 포함하여 한 번의 요청으로 전체 모니터링 정보를 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "대시보드 데이터 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            type = "object",
                            description = "종합적인 대시보드 데이터"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "연결을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = ErrorResponse::class,
                            description = "오류 응답"
                        )
                    )
                ]
            )
        ]
    )
    suspend fun getDashboard(
        @Parameter(
            description = "대시보드 데이터를 조회할 Kafka 연결의 고유 식별자",
            example = "conn_001",
            schema = Schema(
                type = "string",
                pattern = "^[a-zA-Z0-9_-]+$",
                minLength = 1,
                maxLength = 50
            )
        )
        @PathVariable connectionId: String
    ): ResponseEntity<Map<String, Any>> {
        val basicMetrics = kafkaManagementUseCase.getMetrics(connectionId)
        val detailedMetrics = kafkaManagementUseCase.getDetailedMetrics(connectionId)
        val topicsHealth = kafkaManagementUseCase.getAllTopicsHealth(connectionId)
        val consumerGroupMetrics = kafkaManagementUseCase.getAllConsumerGroupMetrics(connectionId)
        val performanceMetrics = kafkaManagementUseCase.getPerformanceMetrics(connectionId)

        val dashboardData = mapOf(
            "basicMetrics" to basicMetrics,
            "detailedMetrics" to detailedMetrics,
            "topicsHealth" to topicsHealth,
            "consumerGroupMetrics" to consumerGroupMetrics,
            "performanceMetrics" to performanceMetrics,
            "timestamp" to java.time.LocalDateTime.now()
        )

        return ResponseEntity.ok(dashboardData)
    }
}