package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.infrastructure.persistence.entities.ActivityEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityJpaRepository : JpaRepository<ActivityEntity, String> {

    @Query("SELECT a FROM ActivityEntity a ORDER BY a.timestamp DESC")
    fun findRecentActivities(pageable: Pageable): List<ActivityEntity>

    @Query("SELECT a FROM ActivityEntity a WHERE a.connectionId = :connectionId ORDER BY a.timestamp DESC")
    fun findByConnectionId(connectionId: String, pageable: Pageable): List<ActivityEntity>

    @Query("DELETE FROM ActivityEntity a WHERE a.timestamp < :cutoffDate")
    fun deleteOldActivities(cutoffDate: LocalDateTime)
}