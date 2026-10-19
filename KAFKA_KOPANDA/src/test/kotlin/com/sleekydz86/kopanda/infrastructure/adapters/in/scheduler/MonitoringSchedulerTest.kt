package com.sleekydz86.kopanda.infrastructure.adapters.`in`.scheduler

import com.sleekydz86.kopanda.application.ports.`in`.AlertManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.ConnectionRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MonitoringSchedulerTest {

    @Test
    fun `응답 없는 연결이 있어도 다른 연결은 확인하고 5초 안팎에 끝난다`() {
        val slow = Connection.create(name = "slow", host = "localhost", port = 9092)
        val fast = Connection.create(name = "fast", host = "localhost", port = 9093)
        val repository = mockk<ConnectionRepository>()
        val alerts = mockk<AlertManagementUseCase>()
        coEvery { repository.findAll() } returns listOf(slow, fast)
        coEvery { alerts.checkAndCreateAlerts(slow.getId().value) } coAnswers { Thread.sleep(20_000) }
        coEvery { alerts.checkAndCreateAlerts(fast.getId().value) } just runs
        val scheduler = MonitoringScheduler(alerts, repository)

        val started = System.currentTimeMillis()
        scheduler.checkAlerts()
        val elapsed = System.currentTimeMillis() - started

        assertThat(elapsed).isLessThan(8_000)
        coVerify(exactly = 1) { alerts.checkAndCreateAlerts(fast.getId().value) }
        scheduler.shutdown()
    }
}
