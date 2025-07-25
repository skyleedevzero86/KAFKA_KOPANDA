package com.sleekydz86.kopanda.domain.entities

<<<<<<< HEAD
import com.sleekydz86.kopanda.domain.valueobjects.ids.BrokerId
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.message.OffsetRange
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
=======
import com.sleekydz86.kopanda.domain.valueobjects.BrokerId
import com.sleekydz86.kopanda.domain.valueobjects.Offset
import com.sleekydz86.kopanda.domain.valueobjects.OffsetRange
import com.sleekydz86.kopanda.domain.valueobjects.PartitionNumber
>>>>>>> origin/main
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "partitions")
class Partition(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "partition_number"))
    )
    val partitionNumber: PartitionNumber,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "leader_broker_id"))
    )
    val leader: BrokerId,

    @ElementCollection
    @CollectionTable(name = "partition_replicas", joinColumns = [JoinColumn(name = "partition_id")])
    @Column(name = "broker_id")
    val replicas: List<BrokerId>,

    @ElementCollection
    @CollectionTable(name = "partition_isr", joinColumns = [JoinColumn(name = "partition_id")])
    @Column(name = "broker_id")
    val inSyncReplicas: List<BrokerId>,

    @Column(name = "earliest_offset")
    var earliestOffset: Long = 0,

    @Column(name = "latest_offset")
    var latestOffset: Long = 0,

    @Column(name = "message_count")
    var messageCount: Long = 0,

    @Column(name = "last_updated")
    var lastUpdated: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private var topic: Topic? = null

    fun setTopic(topic: Topic) {
        this.topic = topic
    }

    fun getTopic(): Topic? = topic

    fun getOffsetRange(): OffsetRange {
        return OffsetRange(
            earliest = Offset(earliestOffset),
            latest = Offset(latestOffset)
        )
    }

    fun isHealthy(): Boolean {
        return inSyncReplicas.contains(leader) && inSyncReplicas.size >= replicas.size / 2 + 1
    }

    fun updateMessageCount(count: Long) {
        messageCount = count
        lastUpdated = LocalDateTime.now()
    }

    fun updateOffsetRange(earliest: Long, latest: Long) {
        earliestOffset = earliest
        latestOffset = latest
        lastUpdated = LocalDateTime.now()
    }

    fun getAvailableMessages(): Long = latestOffset - earliestOffset

    fun isUnderReplicated(): Boolean = inSyncReplicas.size < replicas.size

    fun getReplicationFactor(): Int = replicas.size
}