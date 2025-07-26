package com.sleekydz86.kopanda.infrastructure.persistence.entities

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "connection_history")
class ConnectionHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val connectionId: String,

    @Column(nullable = false)
    val eventType: String,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @ElementCollection
    @CollectionTable(name = "connection_history_details", joinColumns = [JoinColumn(name = "history_id")])
    @MapKeyColumn(name = "detail_key")
    @Column(name = "detail_value")
    val details: Map<String, String> = emptyMap()
)