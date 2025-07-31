package com.sleekydz86.kopanda.infrastructure.config

import com.sleekydz86.kopanda.application.ports.`in`.ConnectionManagementUseCase
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import kotlinx.coroutines.runBlocking

@Component
class DataInitializer(
    private val connectionManagementUseCase: ConnectionManagementUseCase
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    override fun run(vararg args: String?) {
        logger.info("데이터 초기화 시작")

        runBlocking {
            try {
                val defaultConnection = CreateConnectionRequest(
                    name = "Local Kafka",
                    host = "localhost",
                    port = 9092,
                    sslEnabled = false,
                    saslEnabled = false,
                    username = null,
                    password = null
                )

                connectionManagementUseCase.createConnection(defaultConnection)
                logger.info("기본 Kafka 연결이 생성되었습니다: Local Kafka")

            } catch (e: Exception) {
                logger.warn("기본 연결 생성 중 오류 발생: ${e.message}")
            }
        }

        logger.info("데이터 초기화 완료")
    }
}
 