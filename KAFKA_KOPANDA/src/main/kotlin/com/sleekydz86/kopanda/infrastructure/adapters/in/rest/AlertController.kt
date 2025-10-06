package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.response.AlertDto
import com.sleekydz86.kopanda.application.ports.`in`.AlertManagementUseCase
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
@RequestMapping("/api/alerts")
@Tag(name = "Alert Management", description = "모니터링 알람 관리 API")
class AlertController(
    private val alertManagementUseCase: AlertManagementUseCase
) {

    @GetMapping
    @Operation(
        summary = "활성 알람 목록 조회",
        description = "현재 활성화된 모든 모니터링 알람을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "알람 목록 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Array<AlertDto>::class)
                )]
            )
        ]
    )
    fun getActiveAlerts(
        @RequestParam(required = false) connectionId: String?
    ): ResponseEntity<List<AlertDto>> {
        try {
            val alerts = alertManagementUseCase.getActiveAlerts(connectionId)
            return ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            return ResponseEntity.status(500).body(emptyList())
        }
    }

    @GetMapping("/severity/{severity}")
    @Operation(
        summary = "심각도별 알람 조회",
        description = "지정된 심각도의 알람들을 조회합니다."
    )
    fun getAlertsBySeverity(
        @Parameter(description = "알람 심각도")
        @PathVariable severity: String
    ): ResponseEntity<List<AlertDto>> {
        val alerts = alertManagementUseCase.getAlertsBySeverity(severity)
        return ResponseEntity.ok(alerts)
    }

    @PostMapping("/{alertId}/acknowledge")
    @Operation(
        summary = "알람 확인 처리",
        description = "알람을 확인 처리합니다."
    )
    fun acknowledgeAlert(
        @Parameter(description = "알람 ID")
        @PathVariable alertId: String,
        @Parameter(description = "확인 처리자")
        @RequestParam acknowledgedBy: String
    ): ResponseEntity<AlertDto> {
        val alert = alertManagementUseCase.acknowledgeAlert(alertId, acknowledgedBy)
        return ResponseEntity.ok(alert)
    }

    @DeleteMapping
    @Operation(
        summary = "모든 알람 지우기",
        description = "모든 알람을 지웁니다."
    )
    fun clearAllAlerts(
        @Parameter(description = "연결 ID (선택사항)")
        @RequestParam(required = false) connectionId: String?
    ): ResponseEntity<Unit> {
        alertManagementUseCase.clearAllAlerts(connectionId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/check/{connectionId}")
    @Operation(
        summary = "알람 체크 및 생성",
        description = "지정된 연결에 대해 알람을 체크하고 필요한 경우 생성합니다."
    )
    suspend fun checkAndCreateAlerts(
        @Parameter(description = "연결 ID")
        @PathVariable connectionId: String
    ): ResponseEntity<Unit> {
        alertManagementUseCase.checkAndCreateAlerts(connectionId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    fun getAlert(@PathVariable id: String): ResponseEntity<AlertDto> {
        try {
            val alert = alertManagementUseCase.getAlertById(id)
            return ResponseEntity.ok(alert)
        } catch (e: Exception) {
            return ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/test")
    @Operation(
        summary = "테스트 알람 생성",
        description = "테스트용 알람을 생성합니다."
    )
    suspend fun createTestAlert(
        @Parameter(description = "연결 ID")
        @RequestParam connectionId: String
    ): ResponseEntity<AlertDto> {
        try {
            alertManagementUseCase.checkAndCreateAlerts(connectionId)
            val alerts = alertManagementUseCase.getActiveAlerts(connectionId)
            return ResponseEntity.ok(alerts.firstOrNull() ?: AlertDto(
                id = "test-alert-${System.currentTimeMillis()}",
                type = com.sleekydz86.kopanda.domain.valueobjects.common.IssueType.CONNECTION_ERROR,
                severity = com.sleekydz86.kopanda.domain.valueobjects.common.IssueSeverity.MEDIUM,
                title = "테스트 알림",
                message = "이것은 테스트 알림입니다.",
                connectionId = connectionId,
                timestamp = java.time.LocalDateTime.now()
            ))
        } catch (e: Exception) {
            return ResponseEntity.status(500).build()
        }
    }

}