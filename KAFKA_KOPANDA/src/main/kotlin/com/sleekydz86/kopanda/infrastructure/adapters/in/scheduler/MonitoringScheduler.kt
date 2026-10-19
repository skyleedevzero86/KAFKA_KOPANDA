package com.sleekydz86.kopanda.infrastructure.adapters.`in`.scheduler

import com.sleekydz86.kopanda.application.ports.`in`.AlertManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.ConnectionRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class MonitoringScheduler(
    private val alertManagementUseCase: AlertManagementUseCase,
    private val connectionRepository: ConnectionRepository
) {

    private val logger = LoggerFactory.getLogger(MonitoringScheduler::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = ConcurrentHashMap<String, Job>()

    @Scheduled(fixedDelay = 30_000)
    fun checkAlerts() {
        runBlocking {
            val connections = try {
                connectionRepository.findAll()
            } catch (e: Exception) {
                logger.error("연결 목록을 읽지 못했습니다: ${e.message}", e)
                return@runBlocking
            }
            connections.map { connection -> async { checkOne(connection) } }.awaitAll()
        }
    }

    private suspend fun checkOne(connection: Connection) {
        val id = connection.getId().value
        val name = connection.name.value
        if (running[id]?.isActive == true) {
            logger.warn("이전 알림 확인이 아직 끝나지 않아 이번 회차는 건너뜁니다: $name")
            return
        }
        val job = scope.launch {
            try {
                alertManagementUseCase.checkAndCreateAlerts(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("알림 확인 실패: $name: ${e.message}", e)
            }
        }
        running[id] = job
        if (withTimeoutOrNull(CHECK_TIMEOUT_MS) { job.join() } == null) {
            logger.warn("알림 확인이 ${CHECK_TIMEOUT_MS}ms 안에 끝나지 않아 기다리지 않고 넘어갑니다: $name")
        }
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    companion object {
        const val CHECK_TIMEOUT_MS = 5_000L
    }
}
