package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.ConnectionName
import com.sleekydz86.kopanda.domain.valueobjects.Host
import com.sleekydz86.kopanda.domain.valueobjects.Port
import com.sleekydz86.kopanda.domain.valueobjects.ConnectionId
import com.sleekydz86.kopanda.domain.events.ConnectionCreatedEvent
import com.sleekydz86.kopanda.domain.events.ConnectionUpdatedEvent
import com.sleekydz86.kopanda.domain.events.ConnectionDeletedEvent
import com.sleekydz86.kopanda.shared.domain.AggregateRoot
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "connections")
class Connection(
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
    val sslEnabled: Boolean = false,

    @Column(name = "sasl_enabled")
    val saslEnabled: Boolean = false,

    @Column(name = "username")
    val username: String? = null,

    @Column(name = "password")
    val password: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_connected")
    var lastConnected: LocalDateTime? = null
) : AggregateRoot() {

    @EmbeddedId
    private val id: ConnectionId = ConnectionId.generate()

    fun getId(): ConnectionId = id

    fun updateConnectionInfo(
        name: String? = null,
        host: String? = null,
        port: Int? = null,
        sslEnabled: Boolean? = null,
        saslEnabled: Boolean? = null,
        username: String? = null,
        password: String? = null
    ) {

        val newName = name?.let { ConnectionName(it) } ?: this.name
        val newHost = host?.let { Host(it) } ?: this.host
        val newPort = port?.let { Port(it) } ?: this.port
        val newSslEnabled = sslEnabled ?: this.sslEnabled
        val newSaslEnabled = saslEnabled ?: this.saslEnabled
        val newUsername = username
        val newPassword = password

        val updatedConnection = Connection(
            name = newName,
            host = newHost,
            port = newPort,
            sslEnabled = newSslEnabled,
            saslEnabled = newSaslEnabled,
            username = newUsername,
            password = newPassword,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now(),
            lastConnected = this.lastConnected
        )

        addDomainEvent(ConnectionUpdatedEvent(updatedConnection))
    }

    fun markAsConnected() {
        lastConnected = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    fun isConnected(): Boolean = lastConnected != null

    fun getConnectionString(): String = "${host.value}:${port.value}"

    fun requiresAuthentication(): Boolean = saslEnabled && !username.isNullOrBlank()

    fun delete() {
        addDomainEvent(ConnectionDeletedEvent(this))
    }

    companion object {
        fun create(
            name: String,
            host: String,
            port: Int,
            sslEnabled: Boolean = false,
            saslEnabled: Boolean = false,
            username: String? = null,
            password: String? = null
        ): Connection {
            val connection = Connection(
                name = ConnectionName(name),
                host = Host(host),
                port = Port(port),
                sslEnabled = sslEnabled,
                saslEnabled = saslEnabled,
                username = username,
                password = password
            )

            connection.addDomainEvent(ConnectionCreatedEvent(connection))
            return connection
        }
    }
}