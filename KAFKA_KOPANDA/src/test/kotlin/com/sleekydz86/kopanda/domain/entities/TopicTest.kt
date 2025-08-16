package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.ids.BrokerId
import com.sleekydz86.kopanda.domain.events.TopicDeletedEvent
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class TopicTest {

    @Test
    fun `Topic을 생성할 수 있다`() {
        // given
        val name = "test-topic"
        val partitionCount = 3
        val replicationFactor = 2
        val config = mapOf("retention.ms" to "86400000")

        // when
        val topic = Topic.create(name, partitionCount, replicationFactor, config)

        // then
        assertAll(
            { assertThat(topic.name.value).isEqualTo(name) },
            { assertThat(topic.config.partitionCount).isEqualTo(partitionCount) },
            { assertThat(topic.config.replicationFactor).isEqualTo(replicationFactor) },
            { assertThat(topic.config.config).containsEntry("retention.ms", "86400000") },
            { assertThat(topic.isInternal).isFalse() },
            { assertThat(topic.partitions).isEmpty() }
        )
    }

    @Test
    fun `내부 토픽을 생성할 수 있다`() {
        // given
        val name = "__consumer_offsets"
        val partitionCount = 1
        val replicationFactor = 1

        // when
        val topic = Topic.create(name, partitionCount, replicationFactor)

        // then
        assertAll(
            { assertThat(topic.name.value).isEqualTo(name) },
            { assertThat(topic.isInternal).isTrue() }
        )
    }

    @Test
    fun `파티션을 추가할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val partition = createTestPartition(0)

        // when
        topic.addPartition(partition)

        // then
        assertAll(
            { assertThat(topic.partitions).hasSize(1) },
            { assertThat(topic.getPartitionCount()).isEqualTo(1) },
            { assertThat(topic.getPartition(0)).isEqualTo(partition) }
        )
    }

    @Test
    fun `파티션을 제거할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val partition0 = createTestPartition(0)
        val partition1 = createTestPartition(1)
        topic.addPartition(partition0)
        topic.addPartition(partition1)

        // when
        topic.removePartition(0)

        // then
        assertAll(
            { assertThat(topic.partitions).hasSize(1) },
            { assertThat(topic.getPartition(0)).isNull() },
            { assertThat(topic.getPartition(1)).isEqualTo(partition1) }
        )
    }

    @Test
    fun `특정 파티션을 조회할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val partition = createTestPartition(0)
        topic.addPartition(partition)

        // when
        val foundPartition = topic.getPartition(0)

        // then
        assertThat(foundPartition).isEqualTo(partition)
    }

    @Test
    fun `존재하지 않는 파티션을 조회하면 null을 반환한다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)

        // when
        val foundPartition = topic.getPartition(999)

        // then
        assertThat(foundPartition).isNull()
    }

    @Test
    fun `총 메시지 수를 계산할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val partition0 = createTestPartition(0, 100L)
        val partition1 = createTestPartition(1, 200L)
        topic.addPartition(partition0)
        topic.addPartition(partition1)

        // when
        val totalMessageCount = topic.getTotalMessageCount()

        // then
        assertThat(totalMessageCount).isEqualTo(300L)
    }

    @Test
    fun `토픽이 건강한지 확인할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val healthyPartition = createHealthyPartition(0)
        topic.addPartition(healthyPartition)

        // when
        val isHealthy = topic.isHealthy()

        // then
        assertThat(isHealthy).isTrue()
    }

    @Test
    fun `건강하지 않은 파티션이 있으면 토픽이 건강하지 않다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val healthyPartition = createHealthyPartition(0)
        val unhealthyPartition = createUnhealthyPartition(1)
        topic.addPartition(healthyPartition)
        topic.addPartition(unhealthyPartition)

        // when
        val isHealthy = topic.isHealthy()

        // then
        assertThat(isHealthy).isFalse()
    }

    @Test
    fun `건강하지 않은 파티션 목록을 조회할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)
        val healthyPartition = createHealthyPartition(0)
        val unhealthyPartition = createUnhealthyPartition(1)
        topic.addPartition(healthyPartition)
        topic.addPartition(unhealthyPartition)

        // when
        val unhealthyPartitions = topic.getUnhealthyPartitions()

        // then
        assertAll(
            { assertThat(unhealthyPartitions).hasSize(1) },
            { assertThat(unhealthyPartitions.first().partitionNumber.value).isEqualTo(1) }
        )
    }

    @Test
    fun `토픽을 삭제할 수 있다`() {
        // given
        val topic = Topic.create("test-topic", 3, 2)

        // when
        topic.delete()

        // then
        val events = topic.getDomainEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(TopicDeletedEvent::class.java)
    }

    private fun createTestPartition(partitionNumber: Int, messageCount: Long = 0L): Partition {
        return Partition(
            partitionNumber = PartitionNumber(partitionNumber),
            leader = BrokerId(0),
            replicas = listOf(BrokerId(0), BrokerId(1)),
            inSyncReplicas = listOf(BrokerId(0), BrokerId(1)),
            earliestOffset = 0L,
            latestOffset = messageCount,
            messageCount = messageCount
        )
    }

    private fun createHealthyPartition(partitionNumber: Int): Partition {
        return Partition(
            partitionNumber = PartitionNumber(partitionNumber),
            leader = BrokerId(0),
            replicas = listOf(BrokerId(0), BrokerId(1)),
            inSyncReplicas = listOf(BrokerId(0), BrokerId(1)),
            earliestOffset = 0L,
            latestOffset = 100L,
            messageCount = 100L
        )
    }

    private fun createUnhealthyPartition(partitionNumber: Int): Partition {
        return Partition(
            partitionNumber = PartitionNumber(partitionNumber),
            leader = BrokerId(0),
            replicas = listOf(BrokerId(0), BrokerId(1)),
            inSyncReplicas = listOf(BrokerId(0)), // ISR이 복제본보다 적음
            earliestOffset = 0L,
            latestOffset = 100L,
            messageCount = 100L
        )
    }
}
