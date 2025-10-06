package com.sleekydz86.kopanda.infrastructure.persistence.entities

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "alerts")
class AlertEntity(
    @Id
    val id: String,
    
    @Column(name = "type", nullable = false)
    val type: String,
    
    @Column(name = "severity", nullable = false)
    val severity: String,
    
    @Column(name = "title", nullable = false)
    val title: String,
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    val message: String,
    
    @Column(name = "connection_id")
    val connectionId: String?,
    
    @Column(name = "topic_name")
    val topicName: String?,
    
    @Column(name = "partition_number")
    val partitionNumber: Int?,
    
    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime,
    
    @Column(name = "is_acknowledged", nullable = false)
    val isAcknowledged: Boolean,
    
    @Column(name = "acknowledged_at")
    val acknowledgedAt: LocalDateTime?,
    
    @Column(name = "acknowledged_by")
    val acknowledgedBy: String?
)