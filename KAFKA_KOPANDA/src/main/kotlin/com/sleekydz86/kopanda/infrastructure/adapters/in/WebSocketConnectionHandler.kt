package com.sleekydz86.kopanda.infrastructure.adapters.`in`

import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import org.slf4j.LoggerFactory

@Component
class WebSocketConnectionHandler(
    private val webSocketController: WebSocketController
) {

    private val logger = LoggerFactory.getLogger(WebSocketConnectionHandler::class.java)

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        logger.info("WebSocket client connected - Session ID: $sessionId")

        // 연결된 클라이언트에게 초기 상태 정보 전송
        // TODO: 클라이언트별 구독 정보 관리
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        logger.info("WebSocket client disconnected - Session ID: $sessionId")

        // 연결 해제 시 정리 작업
        // TODO: 클라이언트별 구독 정보 정리
    }

    @EventListener
    fun handleWebSocketSubscribeListener(event: SessionSubscribeEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        val destination = headerAccessor.destination

        logger.info("WebSocket client subscribed - Session ID: $sessionId, Destination: $destination")

        // 구독 시 초기 데이터 전송
        when {
            destination?.startsWith("/topic/connection-status/") == true -> {
                val connectionId = destination.substringAfterLast("/")
                // TODO: 해당 연결의 초기 상태 전송
            }
            destination?.startsWith("/topic/metrics/") == true -> {
                val connectionId = destination.substringAfterLast("/")
                // TODO: 해당 연결의 초기 메트릭스 전송
            }
        }
    }

    @EventListener
    fun handleWebSocketUnsubscribeListener(event: SessionUnsubscribeEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        val destination = headerAccessor.destination

        logger.info("WebSocket client unsubscribed - Session ID: $sessionId, Destination: $destination")

        // 구독 해제 시 정리 작업
        // TODO: 클라이언트별 구독 정보에서 제거
    }
} 