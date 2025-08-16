package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.events.ConnectionCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ConnectionTest {

    @Test
    fun `Connection을 생성할 수 있다`() {
        // given
        val name = "test-connection"
        val host = "localhost"
        val port = 9092

        // when
        val connection = Connection.create(name, host, port)

        // then
        assertAll(
            { assertThat(connection.name.value).isEqualTo(name) },
            { assertThat(connection.host.value).isEqualTo(host) },
            { assertThat(connection.port.value).isEqualTo(port) },
            { assertThat(connection.sslEnabled).isFalse() },
            { assertThat(connection.saslEnabled).isFalse() },
            { assertThat(connection.isConnected()).isFalse() },
            { assertThat(connection.isDeleted).isFalse() }
        )
    }

    @Test
    fun `SSL과 SASL이 활성화된 Connection을 생성할 수 있다`() {
        // given
        val name = "secure-connection"
        val host = "kafka.example.com"
        val port = 9093
        val username = "admin"
        val password = "password123"

        // when
        val connection = Connection.create(
            name = name,
            host = host,
            port = port,
            sslEnabled = true,
            saslEnabled = true,
            username = username,
            password = password
        )

        // then
        assertAll(
            { assertThat(connection.sslEnabled).isTrue() },
            { assertThat(connection.saslEnabled).isTrue() },
            { assertThat(connection.username).isEqualTo(username) },
            { assertThat(connection.password).isEqualTo(password) },
            { assertThat(connection.requiresAuthentication()).isTrue() }
        )
    }

    @Test
    fun `Connection 정보를 업데이트할 수 있다`() {
        // given
        val connection = Connection.create("old-name", "old-host", 9092)
        val newName = "new-name"
        val newHost = "new-host"
        val newPort = 9093

        // when
        connection.updateConnectionInfo(
            name = newName,
            host = newHost,
            port = newPort,
            sslEnabled = true
        )

        // then
        assertAll(
            { assertThat(connection.name.value).isEqualTo(newName) },
            { assertThat(connection.host.value).isEqualTo(newHost) },
            { assertThat(connection.port.value).isEqualTo(newPort) },
            { assertThat(connection.sslEnabled).isTrue() }
        )
    }

    @Test
    fun `Connection을 연결 상태로 표시할 수 있다`() {
        // given
        val connection = Connection.create("test", "localhost", 9092)

        // when
        connection.markAsConnected()

        // then
        assertAll(
            { assertThat(connection.isConnected()).isTrue() },
            { assertThat(connection.status).isEqualTo(Connection.ConnectionStatus.CONNECTED) },
            { assertThat(connection.lastConnected).isNotNull() }
        )
    }

    @Test
    fun `Connection을 연결 해제 상태로 표시할 수 있다`() {
        // given
        val connection = Connection.create("test", "localhost", 9092)
        connection.markAsConnected()

        // when
        connection.markAsDisconnected()

        // then
        assertAll(
            { assertThat(connection.isConnected()).isFalse() },
            { assertThat(connection.status).isEqualTo(Connection.ConnectionStatus.DISCONNECTED) }
        )
    }

    @Test
    fun `Connection을 오류 상태로 표시할 수 있다`() {
        // given
        val connection = Connection.create("test", "localhost", 9092)
        val errorMessage = "Connection timeout"

        // when
        connection.markAsError(errorMessage)

        // then
        assertAll(
            { assertThat(connection.isConnected()).isFalse() },
            { assertThat(connection.status).isEqualTo(Connection.ConnectionStatus.ERROR) }
        )
    }

    @Test
    fun `Connection을 삭제할 수 있다`() {
        // given
        val connection = Connection.create("test", "localhost", 9092)

        // when
        connection.delete()

        // then
        assertAll(
            { assertThat(connection.isDeleted).isTrue() },
            { assertThat(connection.updatedAt).isAfter(connection.createdAt) }
        )
    }

    @Test
    fun `Connection 문자열을 올바르게 생성한다`() {
        // given
        val connection = Connection.create("test", "localhost", 9092)

        // when
        val connectionString = connection.getConnectionString()

        // then
        assertThat(connectionString).isEqualTo("localhost:9092")
    }

    @Test
    fun `인증이 필요한지 확인할 수 있다`() {
        // given
        val connectionWithAuth = Connection.create(
            "auth-connection",
            "localhost",
            9092,
            saslEnabled = true,
            username = "user"
        )
        val connectionWithoutAuth = Connection.create("no-auth", "localhost", 9092)

        // when & then
        assertAll(
            { assertThat(connectionWithAuth.requiresAuthentication()).isTrue() },
            { assertThat(connectionWithoutAuth.requiresAuthentication()).isFalse() }
        )
    }

    @Test
    fun `도메인 이벤트가 올바르게 추가된다`() {
        // given
        val connection = Connection.create("test", "localhost", 9092)

        // when
        val events = connection.getDomainEvents()

        // then
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(ConnectionCreatedEvent::class.java)
    }
}
