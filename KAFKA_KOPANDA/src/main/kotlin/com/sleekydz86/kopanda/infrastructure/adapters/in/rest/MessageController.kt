package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.common.OffsetType
import com.sleekydz86.kopanda.application.dto.request.SendMessageRequest
import com.sleekydz86.kopanda.application.dto.response.MessageDto
import com.sleekydz86.kopanda.application.dto.response.PaginatedResponse
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
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/messages")
@Tag(name = "Message Management", description = "Kafka 메시지 관리 API")
class MessageController(
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {
    private val logger = LoggerFactory.getLogger(MessageController::class.java)

    @GetMapping
    @Operation(
        summary = "메시지 목록 조회",
        description = "특정 토픽의 메시지를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "메시지 목록 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PaginatedResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류"
            )
        ]
    )
    suspend fun getMessages(
        @Parameter(description = "연결 ID", required = true)
        @RequestParam connectionId: String,
        @Parameter(description = "토픽 이름", required = true)
        @RequestParam topicName: String,
        @Parameter(description = "파티션 번호", required = true)
        @RequestParam partitionNumber: Int,
        @Parameter(description = "오프셋", required = false)
        @RequestParam(required = false) offset: Long?,
        @Parameter(description = "오프셋 타입", required = true)
        @RequestParam offsetType: String,
        @Parameter(description = "제한 개수", required = true)
        @RequestParam limit: Int
    ): ResponseEntity<PaginatedResponse<MessageDto>> {
        return try {
            logger.info("메시지 목록 조회 요청: $connectionId/$topicName")
            
            val offsetTypeEnum = when (offsetType.uppercase()) {
                "EARLIEST" -> OffsetType.EARLIEST
                "LATEST" -> OffsetType.LATEST
                "SPECIFIC" -> OffsetType.SPECIFIC
                else -> throw IllegalArgumentException("Invalid offset type: $offsetType")
            }
            
            val messages = kafkaManagementUseCase.getMessages(
                connectionId = connectionId,
                topicName = topicName,
                partitionNumber = partitionNumber,
                offset = offset,
                offsetType = offsetTypeEnum,
                limit = limit
            )
            logger.info("메시지 목록 조회 성공: ${messages.items.size}개")
            ResponseEntity.ok(messages)
        } catch (e: DomainException) {
            logger.warn("메시지 조회 실패 (도메인 오류): $connectionId/$topicName", e)
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("메시지 목록 조회 실패: $connectionId/$topicName", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping
    @Operation(
        summary = "메시지 전송",
        description = "특정 토픽에 메시지를 전송합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "메시지 전송 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류"
            )
        ]
    )
    suspend fun sendMessage(
        @Parameter(description = "연결 ID", required = true)
        @RequestParam connectionId: String,
        @Parameter(description = "토픽 이름", required = true)
        @RequestParam topicName: String,
        @Parameter(description = "메시지 전송 요청", required = true)
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<Unit> {
        return try {
            logger.info("메시지 전송 요청: $connectionId/$topicName")
            kafkaManagementUseCase.sendMessage(
                connectionId = connectionId,
                topicName = topicName,
                request = request
            )
            logger.info("메시지 전송 성공: $connectionId/$topicName")
            ResponseEntity.ok().build()
        } catch (e: DomainException) {
            logger.warn("메시지 전송 실패 (도메인 오류): $connectionId/$topicName", e)
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("메시지 전송 실패: $connectionId/$topicName", e)
            ResponseEntity.internalServerError().build()
        }
    }
}