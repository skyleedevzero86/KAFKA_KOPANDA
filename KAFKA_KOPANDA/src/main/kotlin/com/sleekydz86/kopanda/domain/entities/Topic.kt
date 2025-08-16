package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.events.TopicCreatedEvent
import com.sleekydz86.kopanda.domain.events.TopicDeletedEvent
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.topic.TopicConfig
import com.sleekydz86.kopanda.domain.valueobjects.topic.TopicConfig
import com.sleekydz86.kopanda.domain.valueobjects.names.TopicName
import com.sleekydz86.kopanda.domain.valueobjects.*
import com.sleekydz86.kopanda.shared.domain.AggregateRoot
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "topics")
class Topic(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "topic_name"))
    )
    val name: TopicName,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "topic", fetch = FetchType.LAZY)
    val partitions: MutableList<Partition> = mutableListOf(),

    @Embedded
    val config: TopicConfig,

    @Column(name = "internal")
    val isInternal: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AggregateRoot() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun addPartition(partition: Partition) {
        partitions.add(partition)
        partition.setTopic(this)
        updatedAt = LocalDateTime.now()
    }

    fun removePartition(partitionNumber: Int) {
        partitions.removeIf { it.partitionNumber.value == partitionNumber }
        updatedAt = LocalDateTime.now()
    }

    fun getPartition(partitionNumber: Int): Partition? {
        return partitions.find { it.partitionNumber.value == partitionNumber }
    }

    fun getPartitionCount(): Int = partitions.size

    fun getTotalMessageCount(): Long = partitions.sumOf { it.messageCount }



    fun isHealthy(): Boolean {
        return partitions.isNotEmpty() && partitions.any { partition ->
            partition.leader != null && partition.inSyncReplicas.isNotEmpty()
        }
    }

    fun getUnhealthyPartitions(): List<Partition> = partitions.filter { !it.isHealthy() }

    companion object {
        fun create(
            name: String,
            partitionCount: Int,
            replicationFactor: Int,
            config: Map<String, String> = emptyMap()
        ): Topic {
            val topicName = TopicName(name)
            val topicConfig = TopicConfig(
                partitionCount = partitionCount,
                replicationFactor = replicationFactor,
                config = config
            )

            val topic = Topic(
                name = topicName,
                config = topicConfig
            )

            topic.addDomainEvent(TopicCreatedEvent(topic))
            return topic
        }
    }

    fun delete() {
        addDomainEvent(TopicDeletedEvent(this))
    }
}