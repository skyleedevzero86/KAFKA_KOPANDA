package com.sleekydz86.kopanda.infrastructure.persistence.entities

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "connection_history")
class ConnectionHistoryEntity(
    @Id
    val id: String,
    
    @Column(name = "connection_id", nullable = false)
    val connectionId: String,
    
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    
    @Column(name = "description", nullable = false, length = 1000)
    val description: String,
    
    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime,
    
    @Column(name = "details", columnDefinition = "TEXT")
    val details: String = "{}"
) {
    constructor() : this("", "", "", "", LocalDateTime.now(), "{}")
}