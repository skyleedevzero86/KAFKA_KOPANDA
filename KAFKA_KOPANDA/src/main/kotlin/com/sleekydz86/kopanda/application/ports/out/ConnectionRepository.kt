package com.sleekydz86.kopanda.application.ports.out

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.valueobjects.ConnectionId

interface ConnectionRepository {
    suspend fun findAll(): List<Connection>

    suspend fun findById(id: ConnectionId): Connection?

    suspend fun save(connection: Connection): Connection

    suspend fun delete(id: ConnectionId)

    suspend fun existsById(id: ConnectionId): Boolean

    suspend fun findByName(name: String): Connection?
}