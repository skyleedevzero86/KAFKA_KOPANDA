package com.sleekydz86.kopanda.application.services

import com.sleekydz86.kopanda.application.dto.enums.ActivityType
import com.sleekydz86.kopanda.domain.valueobjects.common.ActivityType as DomainActivityType
import com.sleekydz86.kopanda.application.dto.response.ActivityDto
import com.sleekydz86.kopanda.application.ports.out.ActivityRepository
import com.sleekydz86.kopanda.domain.entities.Activity
import com.sleekydz86.kopanda.domain.valueobjects.ids.ActivityId
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityMessage
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityTitle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ActivityServiceTest {

    private lateinit var activityRepository: ActivityRepository
    private lateinit var activityService: ActivityService

    @BeforeEach
    fun setUp() {
        activityRepository = mockk()
        activityService = ActivityService(activityRepository)
    }

    @Test
    fun `최근 활동 목록을 조회할 수 있다`() = runTest {
        // given
        val activities = listOf(
            createTestActivity(ActivityType.CONNECTION_CREATED, "연결 생성"),
            createTestActivity(ActivityType.TOPIC_CREATED, "토픽 생성")
        )
        coEvery { activityRepository.findRecent(10) } returns activities

        // when
        val result = activityService.getRecentActivities(10)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].type).isEqualTo(ActivityType.CONNECTION_CREATED)
        assertThat(result[1].type).isEqualTo(ActivityType.TOPIC_CREATED)
        coVerify { activityRepository.findRecent(10) }
    }

    @Test
    fun `연결별 활동 목록을 조회할 수 있다`() = runTest {
        // given
        val connectionId = "conn-123"
        val activities = listOf(
            createTestActivity(ActivityType.CONNECTION_CREATED, "연결 생성", connectionId)
        )
        coEvery { activityRepository.findByConnectionId(connectionId, 10) } returns activities

        // when
        val result = activityService.getActivitiesByConnection(connectionId, 10)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].connectionId).isEqualTo(connectionId)
        coVerify { activityRepository.findByConnectionId(connectionId, 10) }
    }

    @Test
    fun `연결 생성 활동을 로깅할 수 있다`() = runTest {
        // given
        val connectionName = "test-connection"
        val connectionId = "conn-123"
        coEvery { activityRepository.save(any()) } returns mockk()

        // when
        activityService.logConnectionCreated(connectionName, connectionId)

        // then
        coVerify { activityRepository.save(any()) }
    }

    @Test
    fun `토픽 생성 활동을 로깅할 수 있다`() = runTest {
        // given
        val topicName = "test-topic"
        coEvery { activityRepository.save(any()) } returns mockk()

        // when
        activityService.logTopicCreated(topicName)

        // then
        coVerify { activityRepository.save(any()) }
    }

    @Test
    fun `연결 오프라인 활동을 로깅할 수 있다`() = runTest {
        // given
        val connectionName = "test-connection"
        val connectionId = "conn-123"
        coEvery { activityRepository.save(any()) } returns mockk()

        // when
        activityService.logConnectionOffline(connectionName, connectionId)

        // then
        coVerify { activityRepository.save(any()) }
    }

    @Test
    fun `오류 활동을 로깅할 수 있다`() = runTest {
        // given
        val errorMessage = "연결 실패"
        val connectionId = "conn-123"
        coEvery { activityRepository.save(any()) } returns mockk()

        // when
        activityService.logError(errorMessage, connectionId)

        // then
        coVerify { activityRepository.save(any()) }
    }

    @Test
    fun `오류 활동을 연결 ID 없이 로깅할 수 있다`() = runTest {
        // given
        val errorMessage = "시스템 오류"
        coEvery { activityRepository.save(any()) } returns mockk()

        // when
        activityService.logError(errorMessage)

        // then
        coVerify { activityRepository.save(any()) }
    }

    @Test
    fun `Activity를 ActivityDto로 변환할 수 있다`() = runTest {
        // given
        val activity = createTestActivity(ActivityType.CONNECTION_CREATED, "연결 생성", "conn-123")
        coEvery { activityRepository.findRecent(1) } returns listOf(activity)

        // when
        val result = activityService.getRecentActivities(1)

        // then
        assertThat(result).hasSize(1)
        val activityDto = result[0]
        assertThat(activityDto.id).isEqualTo(activity.getId().value)
        assertThat(activityDto.type).isEqualTo(activity.type)
        assertThat(activityDto.title).isEqualTo(activity.title.value)
        assertThat(activityDto.message).isEqualTo(activity.message.value)
        assertThat(activityDto.connectionId).isEqualTo(activity.connectionId)
        assertThat(activityDto.icon).isEqualTo("🔗")
    }

    @Test
    fun `활동 유형에 따른 아이콘을 올바르게 반환한다`() = runTest {
        // given
        val activities = listOf(
            createTestActivity(ActivityType.CONNECTION_CREATED, "연결 생성"),
            createTestActivity(ActivityType.TOPIC_CREATED, "토픽 생성"),
            createTestActivity(ActivityType.CONNECTION_OFFLINE, "연결 오프라인"),
            createTestActivity(ActivityType.ERROR_OCCURRED, "오류 발생"),
            createTestActivity(ActivityType.CONNECTION_UPDATED, "연결 업데이트"),
            createTestActivity(ActivityType.CONNECTION_DELETED, "연결 삭제"),
            createTestActivity(ActivityType.MESSAGE_SENT, "메시지 전송"),
            createTestActivity(ActivityType.TOPIC_DELETED, "토픽 삭제")
        )
        coEvery { activityRepository.findRecent(8) } returns activities

        // when
        val result = activityService.getRecentActivities(8)

        // then
        val expectedIcons = listOf("🔗", "📝", "⚠️", "❌", "🔄", "🗑️", "📤", "️")
        result.forEachIndexed { index, activityDto ->
            assertThat(activityDto.icon).isEqualTo(expectedIcons[index])
        }
    }

    private fun createTestActivity(
        type: ActivityType,
        title: String,
        connectionId: String? = null
    ): Activity {
        return Activity(
            type = DomainActivityType(type.name),
            title = ActivityTitle(title),
            message = ActivityMessage("테스트 메시지"),
            connectionId = connectionId,
            timestamp = LocalDateTime.now()
        ).apply {
            setId(ActivityId.generate())
        }
    }
}
