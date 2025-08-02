package com.sleekydz86.kopanda.infrastructure.persistence.adapters

import com.sleekydz86.kopanda.domain.entities.Activity
import com.sleekydz86.kopanda.application.ports.out.ActivityRepository
import com.sleekydz86.kopanda.infrastructure.persistence.entities.ActivityEntity
import com.sleekydz86.kopanda.infrastructure.persistence.repositories.ActivityJpaRepository
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Primary
class ActivityRepositoryAdapter(
    private val activityJpaRepository: ActivityJpaRepository
) : ActivityRepository {

    override suspend fun save(activity: Activity): Activity {
        val entity = ActivityEntity.Companion.fromDomain(activity)
        val savedEntity = activityJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun findRecent(limit: Int): List<Activity> {
        val pageable = PageRequest.of(0, limit)
        return activityJpaRepository.findRecentActivities(pageable)
            .map { it.toDomain() }
    }

    override suspend fun findByConnectionId(connectionId: String, limit: Int): List<Activity> {
        val pageable = PageRequest.of(0, limit)
        return activityJpaRepository.findByConnectionId(connectionId, pageable)
            .map { it.toDomain() }
    }

    override suspend fun deleteOldActivities(daysToKeep: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(daysToKeep.toLong())
        activityJpaRepository.deleteOldActivities(cutoffDate)
    }
}