package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.response.ActivityDto
import com.sleekydz86.kopanda.application.dto.response.ErrorResponse
import com.sleekydz86.kopanda.application.ports.`in`.ActivityManagementUseCase
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
@RequestMapping("/activities")
@Tag(
    name = "Activity Management",
    description = "시스템 활동 로그 관리 API - Kafka 연결, 토픽 생성/삭제, 오류 발생 등의 시스템 활동을 추적하고 조회합니다."
)
class ActivityController(
    private val activityManagementUseCase: ActivityManagementUseCase
) {

    @GetMapping
    @Operation(
        summary = "최근 활동 목록 조회",
        description = "시스템에서 발생한 최근 활동들을 시간순으로 조회합니다. 조회 가능한 활동 유형: CONNECTION_CREATED, CONNECTION_UPDATED, CONNECTION_DELETED, CONNECTION_OFFLINE, TOPIC_CREATED, TOPIC_DELETED, MESSAGE_SENT, ERROR_OCCURRED"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "활동 목록 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = Array<ActivityDto>::class,
                            description = "활동 목록 배열"
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
    suspend fun getRecentActivities(
        @Parameter(
            description = "조회할 활동 개수 (기본값: 10, 최대값: 100)",
            example = "10",
            schema = Schema(
                type = "integer",
                minimum = "1",
                maximum = "100",
                defaultValue = "10"
            )
        )
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ActivityDto>> {
        val activities = activityManagementUseCase.getRecentActivities(limit)
        return ResponseEntity.ok(activities)
    }

    @GetMapping("/connection/{connectionId}")
    @Operation(
        summary = "특정 연결의 활동 목록 조회",
        description = "지정된 Kafka 연결과 관련된 활동들을 시간순으로 조회합니다. 조회 가능한 활동 유형: CONNECTION_CREATED, CONNECTION_UPDATED, CONNECTION_DELETED, CONNECTION_OFFLINE, TOPIC_CREATED, TOPIC_DELETED, MESSAGE_SENT, ERROR_OCCURRED"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결별 활동 목록 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(
                            implementation = Array<ActivityDto>::class,
                            description = "활동 목록 배열"
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
    suspend fun getActivitiesByConnection(
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
            description = "조회할 활동 개수 (기본값: 10, 최대값: 100)",
            example = "10",
            schema = Schema(
                type = "integer",
                minimum = "1",
                maximum = "100",
                defaultValue = "10"
            )
        )
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ActivityDto>> {
        val activities = activityManagementUseCase.getActivitiesByConnection(connectionId, limit)
        return ResponseEntity.ok(activities)
    }
}