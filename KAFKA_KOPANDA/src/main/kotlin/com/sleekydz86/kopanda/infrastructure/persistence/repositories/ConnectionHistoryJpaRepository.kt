package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ConnectionHistoryJpaRepository : JpaRepository<ConnectionHistoryEntity, String> {
    
    @Query("SELECT ch FROM ConnectionHistoryEntity ch WHERE ch.connectionId = :connectionId ORDER BY ch.timestamp DESC")
    fun findByConnectionIdOrderByTimestampDesc(@Param("connectionId") connectionId: String, limit: Int): List<ConnectionHistoryEntity>
    
    fun findByConnectionId(connectionId: String): List<ConnectionHistoryEntity>
}