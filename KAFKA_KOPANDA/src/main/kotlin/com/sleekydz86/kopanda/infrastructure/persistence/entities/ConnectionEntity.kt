package com.sleekydz86.kopanda.infrastructure.persistence.entities

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.valueobjects.*
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "connections")
class ConnectionEntity(
    @Id
    @Column(name = "connection_id", nullable = false, length = 36)
    val id: String,

    @Column(name = "connection_name", nullable = false, length = 100)
    val name: String,

    @Column(name = "host", nullable = false, length = 255)
    val host: String,

    @Column(name = "port", nullable = false)
    val port: Int,

    @Column(name = "ssl_enabled", nullable = false)
    val sslEnabled: Boolean = false,

    @Column(name = "sasl_enabled", nullable = false)
    val saslEnabled: Boolean = false,

    @Column(name = "username", length = 100)
    val username: String? = null,

    @Column(name = "password", length = 255)
    val password: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_connected")
    var lastConnected: LocalDateTime? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
) {
    constructor() : this(
        id = "",
        name = "",
        host = "",
        port = 0,
        sslEnabled = false,
        saslEnabled = false,
        username = null,
        password = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        lastConnected = null,
        isDeleted = false
    )

    fun toDomain(): Connection {
        val connection = Connection(
            name = ConnectionName(name),
            host = Host(host),
            port = Port(port),
            sslEnabled = sslEnabled,
            saslEnabled = saslEnabled,
            username = username,
            password = password,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastConnected = lastConnected,
            isDeleted = isDeleted
        )

        connection.setId(ConnectionId(this.id))

        return connection
    }

    companion object {
        fun fromDomain(connection: Connection): ConnectionEntity {
            return ConnectionEntity(
                id = connection.getId().value,
                name = connection.name.value,
                host = connection.host.value,
                port = connection.port.value,
                sslEnabled = connection.sslEnabled,
                saslEnabled = connection.saslEnabled,
                username = connection.username,
                password = connection.password,
                createdAt = connection.createdAt,
                updatedAt = connection.updatedAt,
                lastConnected = connection.lastConnected,
                isDeleted = connection.isDeleted
            )
        }
    }
}