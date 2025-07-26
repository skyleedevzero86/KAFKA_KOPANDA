package com.sleekydz86.kopanda.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Kafka Kopanda API")
                    .description("""
                        Kafka Kopanda는 Kafka 클러스터를 관리하고 모니터링하는 REST API입니다.
                        
                        ## 주요 기능
                        - **연결 관리**: Kafka 클러스터 연결 생성, 수정, 삭제
                        - **토픽 관리**: 토픽 생성, 삭제, 조회
                        - **메시지 관리**: 메시지 조회, 전송, 검색
                        - **모니터링**: 클러스터 상태, 메트릭, 컨슈머 그룹 모니터링
                        - **활동 로그**: 시스템 활동 히스토리 추적
                        
                        ## 인증
                        현재 버전에서는 인증이 필요하지 않습니다.
                        
                        ## 사용 예시
                        1. 연결 생성: `POST /connections`
                        2. 토픽 조회: `GET /connections/{connectionId}/topics`
                        3. 메시지 전송: `POST /connections/{connectionId}/topics/{topicName}/messages`
                    """.trimIndent())
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Kafka Kopanda Team")
                            .email("support@kopanda.com")
                            .url("https://github.com/kopanda/kafka-kopanda")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080/api")
                        .description("개발 서버"),
                    Server()
                        .url("https://api.kopanda.com/api")
                        .description("프로덕션 서버")
                )
            )
    }
}