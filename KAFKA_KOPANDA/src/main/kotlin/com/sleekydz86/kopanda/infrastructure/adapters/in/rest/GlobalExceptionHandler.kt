package com.sleekydz86.kopanda.infrastructure.adapters.`in`.rest

import com.sleekydz86.kopanda.application.dto.response.ErrorResponse
import com.sleekydz86.kopanda.shared.domain.DomainException
import org.apache.kafka.common.KafkaException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("도메인 오류: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "잘못된 요청",
            message = ex.message ?: "도메인 검증에 실패했습니다.",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("입력값 검증 오류: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "잘못된 요청",
            message = ex.message ?: "입력값 검증에 실패했습니다.",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val path = request.getDescription(false).removePrefix("uri=")
        logger.debug("요청한 주소를 찾을 수 없습니다: $path")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = HttpStatus.NOT_FOUND.value(),
            error = "찾을 수 없음",
            message = "요청한 주소를 찾을 수 없습니다.",
            path = path
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(KafkaException::class, ExecutionException::class, TimeoutException::class)
    fun handleKafkaQueryException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Kafka 조회에 실패했습니다: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "서비스를 사용할 수 없음",
            message = "Kafka 조회에 실패했습니다.",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("예상하지 못한 오류가 발생했습니다: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "내부 서버 오류",
            message = "예상하지 못한 오류가 발생했습니다: ${ex.message}",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.internalServerError().body(errorResponse)
    }
}
