package com.sleekydz86.kopanda.application.services

import com.sleekydz86.kopanda.application.dto.response.TopicDto
import com.sleekydz86.kopanda.application.dto.response.TopicHealthDto
import com.sleekydz86.kopanda.application.ports.`in`.KafkaManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.AlertRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AlertServiceTest {

    @Test
    fun `파티션이 하나이고 복제가 정상이면 복제 부족 알림을 만들지 않는다`() = runTest {
        // given
        val alertRepository = mockk<AlertRepository>()
        val kafkaManagementUseCase = mockk<KafkaManagementUseCase>()
        val service = AlertService(alertRepository, kafkaManagementUseCase)
        val now = LocalDateTime.of(2026, 10, 18, 12, 0)
        coEvery { kafkaManagementUseCase.getTopics("conn-1") } returns listOf(
            TopicDto(
                name = "orders",
                partitionCount = 1,
                replicationFactor = 1,
                messageCount = 30L,
                isInternal = false,
                isHealthy = true,
                createdAt = now,
                updatedAt = now
            )
        )
        coEvery { kafkaManagementUseCase.getTopicHealth("conn-1", "orders") } returns TopicHealthDto(
            topicName = "orders",
            isHealthy = true,
            healthScore = 100,
            underReplicatedPartitions = 0,
            offlinePartitions = 0,
            totalPartitions = 1,
            replicationFactor = 1,
            averageReplicationFactor = 1.0,
            lastUpdated = now,
            issues = emptyList()
        )
        coEvery { kafkaManagementUseCase.getConsumerGroups("conn-1") } returns emptyList()

        // when
        service.checkAndCreateAlerts("conn-1")

        // then
        coVerify(exactly = 0) { alertRepository.save(any()) }
    }
}
