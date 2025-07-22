package com.sleekydz86.kopanda.infrastructure.persistence.repositories

import com.sleekydz86.kopanda.application.ports.out.ConnectionRepository
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.valueobjects.ConnectionId
import com.sleekydz86.kopanda.infrastructure.persistence.entities.ConnectionEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class ConnectionRepositoryImpl(
    private val connectionJpaRepository: ConnectionJpaRepository
) : ConnectionRepository {

    override suspend fun findAll(): List<Connection> {
        return connectionJpaRepository.findAll().map { it.toDomain() }
    }

    override suspend fun findById(id: ConnectionId): Connection? {
        return connectionJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override suspend fun save(connection: Connection): Connection {
        val entity = ConnectionEntity.fromDomain(connection)
        val savedEntity = connectionJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun delete(id: ConnectionId) {
        connectionJpaRepository.deleteById(id.value)
    }

    override suspend fun existsById(id: ConnectionId): Boolean {
        return connectionJpaRepository.existsById(id.value)
    }

    override suspend fun findByName(name: String): Connection? {
        return connectionJpaRepository.findByName(name)?.toDomain()
    }
}