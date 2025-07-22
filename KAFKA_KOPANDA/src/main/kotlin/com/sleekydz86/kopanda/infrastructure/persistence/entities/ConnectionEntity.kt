package com.sleekydz86.kopanda.infrastructure.persistence.entities

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.valueobjects.ConnectionId
import com.sleekydz86.kopanda.domain.valueobjects.ConnectionName
import com.sleekydz86.kopanda.domain.valueobjects.Host
import com.sleekydz86.kopanda.domain.valueobjects.Port
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "connections")
class ConnectionEntity(
    @EmbeddedId
    val id: ConnectionId,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "connection_name"))
    )
    val name: ConnectionName,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "host"))
    )
    val host: Host,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "port"))
    )
    val port: Port,

    @Column(name = "ssl_enabled")
    val sslEnabled: Boolean,

    @Column(name = "sasl_enabled")
    val saslEnabled: Boolean,

    @Column(name = "username")
    val username: String?,

    @Column(name = "password")
    val password: String?,

    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime,

    @Column(name = "last_connected")
    var lastConnected: LocalDateTime?
) {
    fun toDomain(): Connection {
        return Connection(
            name = name,
            host = host,
            port = port,
            sslEnabled = sslEnabled,
            saslEnabled = saslEnabled,
            username = username,
            password = password,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastConnected = lastConnected
        )
    }

    companion object {
        fun fromDomain(connection: Connection): ConnectionEntity {
            return ConnectionEntity(
                id = connection.getId(),
                name = connection.name,
                host = connection.host,
                port = connection.port,
                sslEnabled = connection.sslEnabled,
                saslEnabled = connection.saslEnabled,
                username = connection.username,
                password = connection.password,
                createdAt = connection.createdAt,
                updatedAt = connection.updatedAt,
                lastConnected = connection.lastConnected
            )
        }
    }
}