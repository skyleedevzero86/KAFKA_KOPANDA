package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.infrastructure.persistence.entities.AlertEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface AlertJpaRepository : JpaRepository<AlertEntity, String> {
    
    fun findByConnectionIdAndIsAcknowledgedFalse(connectionId: String): List<AlertEntity>
    
    fun findByIsAcknowledgedFalse(): List<AlertEntity>
    
    fun findBySeverity(severity: String): List<AlertEntity>
    
    fun findByTypeAndConnectionIdAndTopicName(
        type: String,
        connectionId: String,
        topicName: String
    ): List<AlertEntity>
    
    @Modifying
    @Transactional
    fun deleteByConnectionId(connectionId: String)
    
    @Modifying
    @Transactional
    fun deleteByTimestampBefore(timestamp: LocalDateTime)
    
    @Query("SELECT a FROM AlertEntity a WHERE a.connectionId = :connectionId AND a.isAcknowledged = false")
    fun findActiveAlertsByConnection(@Param("connectionId") connectionId: String): List<AlertEntity>
    
    @Query("SELECT a FROM AlertEntity a WHERE a.isAcknowledged = false")
    fun findAllActiveAlerts(): List<AlertEntity>
}