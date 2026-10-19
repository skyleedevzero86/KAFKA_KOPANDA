package com.sleekydz86.kopanda.infrastructure.adapters.out

import com.sleekydz86.kopanda.application.dto.common.MessageSearchCriteria
import com.sleekydz86.kopanda.domain.entities.Connection
import com.sleekydz86.kopanda.domain.entities.Partition
import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.shared.domain.OffsetChangeConflictException
import com.sleekydz86.kopanda.domain.valueobjects.ids.BrokerId
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.names.ConnectionName
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.network.Host
import com.sleekydz86.kopanda.domain.valueobjects.network.Port
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.topic.TopicConfig
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.time.Duration
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        ensureTopics(host, port)
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

    @Test
    fun `orders 메시지 수와 오프셋과 토픽 건강을 반환한다`() = runBlocking {
        // given
        val host = "127.0.0.1"
        val port = 19092
        val connection = Connection(
            name = ConnectionName("local"),
            host = Host(host),
            port = Port(port)
        )
        ensureTopics(host, port)
        val producerProps = Properties()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        KafkaProducer<String, String>(producerProps).use { producer ->
            repeat(30) { index ->
                producer.send(ProducerRecord("orders", "key-$index", "value-$index")).get()
            }
        }
        val repository = KafkaRepositoryImpl()
        val topicName = TopicName("orders")

        // when
        val orders = repository.getTopics(connection).single { it.name.value == "orders" }
        val endOffsetSum = (0 until 3).sumOf { partition ->
            repository.getOffsetInfo(connection, topicName, partition).endOffset
        }
        val partition = repository.getPartitionDetails(connection, topicName, 0)
        val health = repository.getTopicHealth(connection, topicName)

        // then
        assertEquals(30L, orders.getTotalMessageCount())
        assertEquals(30L, endOffsetSum)
        assertTrue(partition.leader >= 0)
        assertEquals(1, partition.replicas.size)
        assertEquals(1, partition.inSyncReplicas.size)
        assertFalse(partition.isUnderReplicated)
        assertTrue(health.isHealthy)
        assertEquals(0, health.underReplicatedPartitions)
        assertEquals(3, health.totalPartitions)
    }

    @Test
    fun `billing 그룹의 랙과 없는 그룹 예외를 반환한다`() { runBlocking {
        // given
        val host = "127.0.0.1"
        val port = 19092
        val topic = "invoices"
        val connection = Connection(
            name = ConnectionName("local"),
            host = Host(host),
            port = Port(port)
        )
        val adminProps = Properties()
        adminProps[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        AdminClient.create(adminProps).use { admin ->
            val existing = admin.listTopics().names().get()
            if (topic !in existing) {
                admin.createTopics(listOf(NewTopic(topic, 3, 1.toShort()))).all().get()
            }
        }
        val producerProps = Properties()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        KafkaProducer<String, String>(producerProps).use { producer ->
            repeat(30) { index ->
                producer.send(ProducerRecord(topic, "key-$index", "value-$index")).get()
            }
        }
        val consumerProps = Properties()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = "billing"
        consumerProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        KafkaConsumer<String, String>(consumerProps).use { consumer ->
            val partitions = (0 until 3).map { TopicPartition(topic, it) }
            consumer.assign(partitions)
            consumer.seekToBeginning(partitions)
            var read = 0
            val deadline = System.currentTimeMillis() + 20_000
            while (read < 10 && System.currentTimeMillis() < deadline) {
                read += consumer.poll(Duration.ofMillis(500)).count()
            }
            assertEquals(10, read)
            consumer.commitSync()
        }
        val repository = KafkaRepositoryImpl()

        try {
            // when
            val metrics = repository.getConsumerGroupMetrics(connection, "billing")

            // then
            assertEquals(20L, metrics.totalLag)
            assertEquals(10L, metrics.partitions.sumOf { it.committedOffset })
            assertEquals(30L, metrics.partitions.sumOf { it.endOffset })
            assertEquals(1, metrics.topicCount)
            assertFails {
                repository.getConsumerGroupMetrics(connection, "missing-group")
            }
        } finally {
            AdminClient.create(adminProps).use { admin ->
                admin.deleteTopics(listOf(topic)).all().get()
            }
        }
    } }

    @Test
    fun `audit 그룹 오프셋을 바꾸고 범위와 실행 중 소비자를 거절한다`() {
        runBlocking {
            // given
            val host = "127.0.0.1"
            val port = 19092
            val topic = "audit-log"
            val connection = Connection(
                name = ConnectionName("local"),
                host = Host(host),
                port = Port(port)
            )
            val topicName = TopicName(topic)
            val adminProps = Properties()
            adminProps[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
            AdminClient.create(adminProps).use { admin ->
                val existing = admin.listTopics().names().get()
                if (topic !in existing) {
                    admin.createTopics(listOf(NewTopic(topic, 3, 1.toShort()))).all().get()
                }
            }
            val producerProps = Properties()
            producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
            producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            KafkaProducer<String, String>(producerProps).use { producer ->
                repeat(3) { partition ->
                    repeat(10) { index ->
                        producer.send(ProducerRecord(topic, partition, "key-$partition-$index", "value-$index")).get()
                    }
                }
            }
            val consumerProps = Properties()
            consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
            consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = "audit"
            consumerProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
            consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
            consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            KafkaConsumer<String, String>(consumerProps).use { consumer ->
                val partitions = (0 until 3).map { TopicPartition(topic, it) }
                consumer.assign(partitions)
                consumer.seekToBeginning(partitions)
                var read = 0
                val deadline = System.currentTimeMillis() + 20_000
                while (read < 10 && System.currentTimeMillis() < deadline) {
                    read += consumer.poll(Duration.ofMillis(500)).count()
                }
                assertEquals(10, read)
                consumer.commitSync()
            }
            val repository = KafkaRepositoryImpl()

            try {
                // when
                val changed = repository.setOffset(connection, topicName, 1, "audit", 5)
                val committed = repository.getConsumerGroupMetrics(connection, "audit")
                    .partitions.single { it.partition == 1 }.committedOffset

                // then
                assertTrue(changed)
                assertEquals(5L, committed)
                assertFailsWith<IllegalArgumentException> {
                    repository.setOffset(connection, topicName, 1, "audit", 999)
                }
                KafkaConsumer<String, String>(consumerProps).use { consumer ->
                    consumer.subscribe(listOf(topic))
                    val deadline = System.currentTimeMillis() + 20_000
                    while (consumer.assignment().isEmpty() && System.currentTimeMillis() < deadline) {
                        consumer.poll(Duration.ofMillis(500))
                    }
                    assertTrue(consumer.assignment().isNotEmpty())
                    assertFailsWith<OffsetChangeConflictException> {
                        repository.setOffset(connection, topicName, 1, "audit", 5)
                    }
                }
            } finally {
                AdminClient.create(adminProps).use { admin ->
                    admin.deleteTopics(listOf(topic)).all().get()
                }
            }
        }
    }

    @Test
    fun `지정 오프셋부터 limit개를 읽고 끝에서는 남은 메시지만 바로 반환한다`() = runBlocking {
        // given
        val host = "127.0.0.1"
        val port = 19092
        val topicName = "read-window"
        val connection = Connection(
            name = ConnectionName("local"),
            host = Host(host),
            port = Port(port)
        )
        val adminProps = adminProps(host, port)
        createTopic(adminProps, topicName, 1)
        produce(host, port, topicName, 30) { index -> "value-$index" }
        val repository = KafkaRepositoryImpl()
        val topic = browseTopic(topicName)
        val partition = browsePartition()

        try {
            // when
            val window = repository.getMessages(connection, topic, partition, Offset(10), 10)
            val started = System.nanoTime()
            val tail = repository.getMessages(connection, topic, partition, Offset(25), 10)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            val groups = consumerGroups(adminProps)

            // then
            assertEquals((10L..19L).toList(), window.map { it.offset.value })
            assertEquals((25L..29L).toList(), tail.map { it.offset.value })
            assertTrue(elapsedMs < 8_000)
            assertFalse(groups.contains("kopanda-consumer"))
        } finally {
            deleteTopic(adminProps, topicName)
        }
    }

    @Test
    fun `값 필터를 통과한 메시지를 limit까지 채운다`() = runBlocking {
        // given
        val host = "127.0.0.1"
        val port = 19092
        val topicName = "read-search"
        val connection = Connection(
            name = ConnectionName("local"),
            host = Host(host),
            port = Port(port)
        )
        val adminProps = adminProps(host, port)
        createTopic(adminProps, topicName, 1)
        produce(host, port, topicName, 30) { index ->
            if (index % 2 == 0) "keep-$index" else "other-$index"
        }
        val repository = KafkaRepositoryImpl()

        try {
            // when
            val found = repository.searchMessages(
                connection,
                MessageSearchCriteria(topic = topicName, partition = 0, value = "keep", limit = 10)
            )
            val groups = consumerGroups(adminProps)

            // then
            assertEquals((0L..18L step 2).toList(), found.map { it.offset.value })
            assertTrue(found.all { it.value.value.contains("keep") })
            assertFalse(groups.contains("kopanda-consumer"))
        } finally {
            deleteTopic(adminProps, topicName)
        }
    }

    private fun ensureTopics(host: String, port: Int) {
        val props = Properties()
        props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        AdminClient.create(props).use { admin ->
            val existing = admin.listTopics().names().get()
            val missing = listOf(
                NewTopic("orders", 3, 1.toShort()),
                NewTopic("payments", 2, 1.toShort())
            ).filter { it.name() !in existing }
            if (missing.isNotEmpty()) {
                admin.createTopics(missing).all().get()
            }
        }
    }

    private fun adminProps(host: String, port: Int): Properties {
        val props = Properties()
        props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        return props
    }

    private fun createTopic(adminProps: Properties, topic: String, partitions: Int) {
        AdminClient.create(adminProps).use { admin ->
            val existing = admin.listTopics().names().get()
            if (topic !in existing) {
                admin.createTopics(listOf(NewTopic(topic, partitions, 1.toShort()))).all().get()
            }
        }
    }

    private fun deleteTopic(adminProps: Properties, topic: String) {
        AdminClient.create(adminProps).use { admin ->
            admin.deleteTopics(listOf(topic)).all().get()
        }
    }

    private fun produce(host: String, port: Int, topic: String, count: Int, valueOf: (Int) -> String) {
        val producerProps = Properties()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "$host:$port"
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        KafkaProducer<String, String>(producerProps).use { producer ->
            repeat(count) { index ->
                producer.send(ProducerRecord(topic, 0, "key-$index", valueOf(index))).get()
            }
        }
    }

    private fun consumerGroups(adminProps: Properties): List<String> {
        return AdminClient.create(adminProps).use { admin ->
            admin.listConsumerGroups().all().get().map { it.groupId() }
        }
    }

    private fun browseTopic(name: String): Topic {
        return Topic(name = TopicName(name), config = TopicConfig(partitionCount = 1, replicationFactor = 1))
    }

    private fun browsePartition(): Partition {
        val broker = BrokerId(1)
        return Partition(
            partitionNumber = PartitionNumber(0),
            leader = broker,
            replicas = listOf(broker),
            inSyncReplicas = listOf(broker)
        )
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
