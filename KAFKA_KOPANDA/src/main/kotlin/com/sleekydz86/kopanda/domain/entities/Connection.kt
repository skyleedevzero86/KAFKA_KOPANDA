package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.names.ConnectionName
import com.sleekydz86.kopanda.domain.valueobjects.network.Host
import com.sleekydz86.kopanda.domain.valueobjects.network.Port
import com.sleekydz86.kopanda.domain.valueobjects.ids.ConnectionId
import com.sleekydz86.kopanda.domain.events.ConnectionCreatedEvent
import com.sleekydz86.kopanda.domain.events.ConnectionUpdatedEvent
import com.sleekydz86.kopanda.domain.events.ConnectionDeletedEvent
import com.sleekydz86.kopanda.shared.domain.AggregateRoot
import java.time.LocalDateTime

class Connection(
    var name: ConnectionName,
    var host: Host,
    var port: Port,
    var sslEnabled: Boolean = false,
    var saslEnabled: Boolean = false,
    var username: String? = null,
    var password: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var lastConnected: LocalDateTime? = null,
    var isDeleted: Boolean = false
) : AggregateRoot() {

    private var id: ConnectionId = ConnectionId.generate()

    fun getId(): ConnectionId = id

    fun setId(connectionId: ConnectionId) {
        this.id = connectionId
    }

    fun updateConnectionInfo(
        name: String? = null,
        host: String? = null,
        port: Int? = null,
        sslEnabled: Boolean? = null,
        saslEnabled: Boolean? = null,
        username: String? = null,
        password: String? = null
    ) {
        name?.let { this.name = ConnectionName(it) }
        host?.let { this.host = Host(it) }
        port?.let { this.port = Port(it) }
        sslEnabled?.let { this.sslEnabled = it }
        saslEnabled?.let { this.saslEnabled = it }
        username?.let { this.username = it }
        password?.let { this.password = it }

        this.updatedAt = LocalDateTime.now()
        addDomainEvent(ConnectionUpdatedEvent(this))
    }

    fun markAsConnected() {
        lastConnected = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    fun isConnected(): Boolean = lastConnected != null

    fun getConnectionString(): String = "${host.value}:${port.value}"

    fun requiresAuthentication(): Boolean = saslEnabled && !username.isNullOrBlank()

    fun delete() {
        isDeleted = true
        updatedAt = LocalDateTime.now()
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