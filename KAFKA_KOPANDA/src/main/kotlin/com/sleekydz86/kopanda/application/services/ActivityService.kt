package com.sleekydz86.kopanda.application.services

import com.sleekydz86.kopanda.application.dto.response.ActivityDto
import com.sleekydz86.kopanda.application.dto.enums.ActivityType
import com.sleekydz86.kopanda.application.ports.`in`.ActivityManagementUseCase
import com.sleekydz86.kopanda.application.ports.out.ActivityRepository
import com.sleekydz86.kopanda.domain.entities.Activity
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityMessage
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityTitle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
            type = ActivityType.ERROR_OCCURRED,
            title = ActivityTitle("오류 발생"),
            message = ActivityMessage(message),
            connectionId = connectionId
        )
        activityRepository.save(activity)
    }

    private fun Activity.toActivityDto(): ActivityDto {
        return ActivityDto(
            id = this.getId().value,
            type = this.type,
            title = this.title.value,
            message = this.message.value,
            connectionId = this.connectionId,
            topicName = this.topicName,
            timestamp = this.timestamp,
            icon = getIconForType(this.type)
        )
    }

    private fun getIconForType(type: ActivityType): String {
        return when (type) {
            ActivityType.CONNECTION_CREATED -> "🔗"
            ActivityType.TOPIC_CREATED -> "📝"
            ActivityType.CONNECTION_OFFLINE -> "⚠️"
            ActivityType.ERROR_OCCURRED -> "❌"
            ActivityType.CONNECTION_UPDATED -> "🔄"
            ActivityType.CONNECTION_DELETED -> "🗑️"
            ActivityType.MESSAGE_SENT -> "📤"
            ActivityType.TOPIC_DELETED -> "️"
        }
    }
}