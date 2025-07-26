package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ConnectionHistoryJpaRepository : JpaRepository<ConnectionHistoryEntity, Long> {
    @Query("SELECT h FROM ConnectionHistoryEntity h WHERE h.connectionId = :connectionId ORDER BY h.timestamp DESC")
    fun findByConnectionId(connectionId: String): List<ConnectionHistoryEntity>
}