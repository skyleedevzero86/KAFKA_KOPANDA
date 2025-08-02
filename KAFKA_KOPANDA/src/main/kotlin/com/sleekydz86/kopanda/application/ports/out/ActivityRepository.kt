package com.sleekydz86.kopanda.application.ports.out

import com.sleekydz86.kopanda.domain.entities.Activity

interface ActivityRepository {
    suspend fun save(activity: Activity): Activity
    suspend fun findRecent(limit: Int): List<Activity>
    suspend fun findByConnectionId(connectionId: String, limit: Int): List<Activity>
    suspend fun deleteOldActivities(daysToKeep: Int)
}