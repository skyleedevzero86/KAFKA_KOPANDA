package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.domain.entities.Connection
import org.apache.kafka.clients.admin.AdminClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch

class AdminClientRegistryTest {

    private lateinit var registry: AdminClientRegistry

    @BeforeEach
    fun setUp() {
        registry = AdminClientRegistry()
    }

    @AfterEach
    fun tearDown() {
        registry.closeAll()
    }

    @Test
    fun `같은 연결은 같은 AdminClient를 반환한다`() {
        // given
        val connection = connection(password = "pw-do-not-print")

        // when
        val first = registry.get(connection)
        val second = registry.get(connection)

        // then
        assertThat(second).isSameAs(first)
        assertThat(registry.toString()).doesNotContain("pw-do-not-print")
    }

    @Test
    fun `설정이 바뀌면 새 인스턴스를 만들고 이전 것을 닫는다`() {
        // given
        val connection = connection()
        val first = registry.get(connection)

        // when
        connection.updateConnectionInfo(port = 9093)
        val second = registry.get(connection)

        // then
        assertThat(second).isNotSameAs(first)
        assertClosed(first)
    }

    @Test
    fun `evict 하면 닫히고 다음 get은 새 인스턴스다`() {
        // given
        val connection = connection()
        val first = registry.get(connection)

        // when
        registry.evict(connection.getId().value)
        val second = registry.get(connection)

        // then
        assertClosed(first)
        assertThat(second).isNotSameAs(first)
    }

    @Test
    fun `동시에 여러 번 get 해도 인스턴스는 하나다`() {
        // given
        val connection = connection()
        val threads = 8
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val instances = Collections.synchronizedList(mutableListOf<AdminClient>())

        // when
        val workers = (1..threads).map {
            Thread {
                ready.countDown()
                start.await()
                instances.add(registry.get(connection))
            }
        }
        workers.forEach { it.start() }
        ready.await()
        start.countDown()
        workers.forEach { it.join() }

        // then
        assertThat(instances).hasSize(threads)
        assertThat(instances.distinct()).containsExactly(instances.first())
    }

    private fun assertClosed(admin: AdminClient) {
        assertThatThrownBy { admin.listTopics().names().get() }
            .satisfies({ thrown ->
                assertThat(generateSequence(thrown) { it.cause }.any { it is IllegalStateException }).isTrue()
            })
    }

    private fun connection(password: String? = null): Connection {
        return Connection.create(
            name = "registry",
            host = "localhost",
            port = 9092,
            password = password
        )
    }
}
