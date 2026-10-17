package com.sleekydz86.kopanda.infrastructure.adapters.`in`.websocket.handlers

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

@Component
class WebSocketConnectionHandler {

    private val logger = LoggerFactory.getLogger(WebSocketConnectionHandler::class.java)

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        logger.info("웹소켓 클라이언트가 연결되었습니다. 세션 ID: $sessionId")
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        logger.info("웹소켓 클라이언트 연결이 끊어졌습니다. 세션 ID: $sessionId")
    }

    @EventListener
    fun handleWebSocketSubscribeListener(event: SessionSubscribeEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        val destination = headerAccessor.destination

        logger.info("웹소켓 클라이언트가 구독했습니다. 세션 ID: $sessionId, 대상: $destination")
    }

    @EventListener
    fun handleWebSocketUnsubscribeListener(event: SessionUnsubscribeEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        val destination = headerAccessor.destination

        logger.info("웹소켓 클라이언트 구독이 해제되었습니다. 세션 ID: $sessionId, 대상: $destination")
    }
}