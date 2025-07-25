package com.sleekydz86.kopanda.application.services

import com.sleekydz86.kopanda.application.dto.response.ActivityDto
import com.sleekydz86.kopanda.application.ports.`in`.ActivityManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.ActivityRepository
import com.sleekydz86.kopanda.domain.entities.Activity
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityMessage
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityTitle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.sleekydz86.kopanda.domain.valueobjects.common.ActivityType

@Service
@Transactional
class ActivityService(
    private val activityRepository: ActivityRepository
) : ActivityManagementUseCase {

    override suspend fun getRecentActivities(limit: Int): List<ActivityDto> {
        return activityRepository.findRecent(limit).map { it.toActivityDto() }
    }

    override suspend fun getActivitiesByConnection(connectionId: String, limit: Int): List<ActivityDto> {
        return activityRepository.findByConnectionId(connectionId, limit).map { it.toActivityDto() }
    }

    override suspend fun logConnectionCreated(connectionName: String, connectionId: String) {
        val activity = Activity.createConnectionCreated(connectionName, connectionId)
        activityRepository.save(activity)
    }

    override suspend fun logTopicCreated(topicName: String) {
        val activity = Activity.createTopicCreated(topicName)
        activityRepository.save(activity)
    }

    override suspend fun logConnectionOffline(connectionName: String, connectionId: String) {
        val activity = Activity.createConnectionOffline(connectionName, connectionId)
        activityRepository.save(activity)
    }

    override suspend fun logError(message: String, connectionId: String?) {
        val activity = Activity(
            type = ActivityType("ERROR_OCCURRED"),
            title = ActivityTitle("오류 발생"),
            message = ActivityMessage(message),
            connectionId = connectionId
        )
        activityRepository.save(activity)
    }

    private fun Activity.toActivityDto(): ActivityDto {
        return ActivityDto(
            id = this.getId().value,
            type = when (this.type.value) {
                "CONNECTION_CREATED" -> com.sleekydz86.kopanda.application.dto.enums.ActivityType.CONNECTION_CREATED
                "TOPIC_CREATED" -> com.sleekydz86.kopanda.application.dto.enums.ActivityType.TOPIC_CREATED
                "CONNECTION_OFFLINE" -> com.sleekydz86.kopanda.application.dto.enums.ActivityType.CONNECTION_OFFLINE
                "ERROR_OCCURRED" -> com.sleekydz86.kopanda.application.dto.enums.ActivityType.ERROR_OCCURRED
                else -> com.sleekydz86.kopanda.application.dto.enums.ActivityType.ERROR_OCCURRED
            },
            title = this.title.value,
            message = this.message.value,
            connectionId = this.connectionId,
            topicName = this.topicName,
            timestamp = this.timestamp,
            icon = getIconForType(this.type.value)
        )
    }

    private fun getIconForType(type: String): String {
        return when (type) {
            "CONNECTION_CREATED" -> "🔗"
            "TOPIC_CREATED" -> "��"
            "CONNECTION_OFFLINE" -> "⚠️"
            "ERROR_OCCURRED" -> "❌"
            else -> "ℹ️"
        }
    }
}