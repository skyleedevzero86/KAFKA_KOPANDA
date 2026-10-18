package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.valueobjects.names.ConnectionName
import com.sleekydz86.kopanda.domain.valueobjects.network.Host
import com.sleekydz86.kopanda.domain.valueobjects.network.Port
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@Testcontainers
class KafkaRepositoryImplTest {

    @Test
    fun `만든 토픽의 메트릭과 클러스터 정보를 반환한다`() = runBlocking {
        // given
        val host = "127.0.0.1"
        val port = 19092
        val connection = Connection(
            name = ConnectionName("local"),
            host = Host(host),
            port = Port(port)
        )
        val props = Properties()
        props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        AdminClient.create(props).use { admin ->
            admin.createTopics(
                listOf(
                    NewTopic("orders", 3, 1.toShort()),
                    NewTopic("payments", 2, 1.toShort())
                )
            ).all().get()
        }
        val repository = KafkaRepositoryImpl()

        // when
        val metrics = repository.getMetrics(connection)
        val cluster = repository.getClusterInfo(connection)

        // then
        assertEquals(1, metrics.brokerCount)
        assertEquals(2, metrics.topicCount)
        assertEquals(5, metrics.totalPartitions)
        assertNull(metrics.messagesPerSecond)
        assertNull(metrics.bytesInPerSec)
        assertNull(metrics.bytesOutPerSec)
        assertNotEquals("test-cluster", cluster.clusterId)
        assertEquals(2, cluster.totalTopics)
        assertEquals(5, cluster.totalPartitions)
        assertNull(cluster.version)
    }

    companion object {
        @Container
        @JvmStatic
        val kafka = ConfluentKafkaContainer("confluentinc/cp-kafka:7.9.0").apply {
            portBindings = listOf("19092:9092")
            withEnv(
                "KAFKA_LISTENERS",
                "PLAINTEXT://0.0.0.0:9092,BROKER://0.0.0.0:9093,CONTROLLER://127.0.0.1:9094"
            )
            withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@127.0.0.1:9094")
            withEnv(
                "KAFKA_ADVERTISED_LISTENERS",
                "PLAINTEXT://127.0.0.1:19092,BROKER://127.0.0.1:9093"
            )
        }
    }
}
