package com.sleekydz86.kopanda.application.ports.out

import com.sleekydz86.kopanda.domain.entities.ConnectionHistory

interface ConnectionHistoryRepository {
    suspend fun save(history: ConnectionHistory)
    suspend fun findByConnectionId(connectionId: String, limit: Int): List<ConnectionHistory>
}