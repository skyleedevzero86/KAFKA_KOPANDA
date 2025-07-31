package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.request.CreateTopicRequest
import com.sleekydz86.kopanda.application.dto.response.*
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
@RequestMapping("/topics")
@Tag(name = "Topic Management", description = "Kafka 토픽 관리 API")
class TopicController(
    private val kafkaManagementUseCase: KafkaManagementUseCase
) {

    private val logger = LoggerFactory.getLogger(TopicController::class.java)

    @GetMapping
    @Operation(
        summary = "토픽 목록 조회",
        description = "특정 연결의 모든 토픽 목록을 조회합니다."
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
        @RequestParam connectionId: String
    ): ResponseEntity<List<TopicDto>> {
        return try {
            logger.info("토픽 목록 조회 요청: $connectionId")
            val topics = kafkaManagementUseCase.getTopics(connectionId)
            logger.info("토픽 목록 조회 성공: ${topics.size}개")
            ResponseEntity.ok(topics)
        } catch (e: DomainException) {
            logger.warn("연결을 찾을 수 없음: $connectionId")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("토픽 목록 조회 실패: $connectionId", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{topicName}")
    @Operation(
        summary = "토픽 상세 조회",
        description = "특정 토픽의 상세 정보를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토픽 상세 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TopicDetailDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "토픽을 찾을 수 없음"
            )
        ]
    )
    suspend fun getTopicDetails(
        @Parameter(description = "연결 ID", required = true)
        @RequestParam connectionId: String,
        @Parameter(description = "토픽 이름", required = true)
        @PathVariable topicName: String
    ): ResponseEntity<TopicDetailDto> {
        return try {
            logger.info("토픽 상세 조회 요청: $connectionId/$topicName")
            val topic = kafkaManagementUseCase.getTopicDetails(connectionId, topicName)
            ResponseEntity.ok(topic)
        } catch (e: DomainException) {
            logger.warn("토픽을 찾을 수 없음: $topicName")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("토픽 상세 조회 실패: $connectionId/$topicName", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping
    @Operation(
        summary = "토픽 생성",
        description = "새로운 Kafka 토픽을 생성합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "토픽 생성 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TopicDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 데이터"
            )
        ]
    )
    suspend fun createTopic(
        @Parameter(description = "연결 ID", required = true)
        @RequestParam connectionId: String,
        @Parameter(description = "토픽 생성 요청", required = true)
        @RequestBody request: CreateTopicRequest
    ): ResponseEntity<TopicDto> {
        return try {
            logger.info("토픽 생성 요청: $connectionId/${request.name}")
            val topic = kafkaManagementUseCase.createTopic(connectionId, request)
            logger.info("토픽 생성 성공: ${request.name}")
            ResponseEntity.status(201).body(topic)
        } catch (e: DomainException) {
            logger.warn("토픽 생성 실패 (도메인 예외): ${request.name}", e)
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("토픽 생성 실패: $connectionId/${request.name}", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/{topicName}")
    @Operation(
        summary = "토픽 삭제",
        description = "Kafka 토픽을 삭제합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "토픽 삭제 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "토픽을 찾을 수 없음"
            )
        ]
    )
    suspend fun deleteTopic(
        @Parameter(description = "연결 ID", required = true)
        @RequestParam connectionId: String,
        @Parameter(description = "토픽 이름", required = true)
        @PathVariable topicName: String
    ): ResponseEntity<Unit> {
        return try {
            logger.info("토픽 삭제 요청: $connectionId/$topicName")
            kafkaManagementUseCase.deleteTopic(connectionId, topicName)
            logger.info("토픽 삭제 성공: $topicName")
            ResponseEntity.noContent().build()
        } catch (e: DomainException) {
            logger.warn("토픽을 찾을 수 없음: $topicName")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("토픽 삭제 실패: $connectionId/$topicName", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{topicName}/health")
    @Operation(
        summary = "토픽 상태 점검",
        description = "특정 토픽의 상태를 점검합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토픽 상태 점검 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TopicHealthDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "토픽을 찾을 수 없음"
            )
        ]
    )
    suspend fun getTopicHealth(
        @Parameter(description = "연결 ID", required = true)
        @RequestParam connectionId: String,
        @Parameter(description = "토픽 이름", required = true)
        @PathVariable topicName: String
    ): ResponseEntity<TopicHealthDto> {
        return try {
            logger.info("토픽 상태 점검 요청: $connectionId/$topicName")
            val health = kafkaManagementUseCase.getTopicHealth(connectionId, topicName)
            ResponseEntity.ok(health)
        } catch (e: DomainException) {
            logger.warn("토픽을 찾을 수 없음: $topicName")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("토픽 상태 점검 실패: $connectionId/$topicName", e)
            ResponseEntity.internalServerError().build()
        }
    }
}
 