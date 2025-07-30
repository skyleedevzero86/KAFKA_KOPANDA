package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.application.ports.out.ConnectionHistoryRepository
import com.sleekydz86.kopanda.domain.entities.ConnectionHistory
import com.sleekydz86.kopanda.infrastructure.persistence.adapters.ConnectionHistoryRepositoryAdapter
import org.springframework.stereotype.Repository

@Repository
class ConnectionHistoryRepositoryImpl(
    private val connectionHistoryRepositoryAdapter: ConnectionHistoryRepositoryAdapter
) : ConnectionHistoryRepository {

    override suspend fun save(history: ConnectionHistory) {
        connectionHistoryRepositoryAdapter.save(history)
    }

    override suspend fun findByConnectionId(connectionId: String, limit: Int): List<ConnectionHistory> {
        return connectionHistoryRepositoryAdapter.findByConnectionId(connectionId, limit)
    }
} 