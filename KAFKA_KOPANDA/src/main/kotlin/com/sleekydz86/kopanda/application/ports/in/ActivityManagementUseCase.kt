package com.sleekydz86.kopanda.application.ports.`in`

import com.sleekydz86.kopanda.application.dto.response.ActivityDto

interface ActivityManagementUseCase {
    suspend fun getRecentActivities(limit: Int = 10): List<ActivityDto>
    suspend fun getActivitiesByConnection(connectionId: String, limit: Int = 10): List<ActivityDto>
    suspend fun logConnectionCreated(connectionName: String, connectionId: String)
    suspend fun logTopicCreated(topicName: String)
    suspend fun logConnectionOffline(connectionName: String, connectionId: String)
    suspend fun logError(message: String, connectionId: String? = null)
}