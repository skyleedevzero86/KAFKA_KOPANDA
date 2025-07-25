package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.domain.valueobjects.message.MessageKey
import com.sleekydz86.kopanda.domain.valueobjects.message.MessageValue
import com.sleekydz86.kopanda.domain.valueobjects.message.Offset
import com.sleekydz86.kopanda.domain.valueobjects.topic.PartitionNumber
import com.sleekydz86.kopanda.domain.valueobjects.message.Timestamp
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
class Message(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "offset_value"))
    )
    val offset: Offset,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "message_key"))
    )
    val key: MessageKey?,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "message_value"))
    )
    val value: MessageValue,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "timestamp_value"))
    )
    val timestamp: Timestamp,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value", column = Column(name = "partition_number"))
    )
    val partitionNumber: PartitionNumber,

    @ElementCollection
    @CollectionTable(name = "message_headers", joinColumns = [JoinColumn(name = "message_id")])
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    val headers: Map<String, String> = emptyMap(),

    @Column(name = "consumed")
    var consumed: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
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

    fun markAsConsumed() {
        consumed = true
    }

    fun isUnconsumed(): Boolean = !consumed

    fun getFormattedTimestamp(): String = timestamp.toLocalDateTime().toString()

    fun getFormattedOffset(): String = offset.value.toString()

    fun hasKey(): Boolean = key != null

    fun hasHeaders(): Boolean = headers.isNotEmpty()

    companion object {
        fun create(
            offset: Long,
            key: String?,
            value: String,
            timestamp: Long,
            partitionNumber: Int,
            headers: Map<String, String> = emptyMap()
        ): Message {
            return Message(
                offset = Offset(offset),
                key = key?.let { MessageKey(it) },
                value = MessageValue(value),
                timestamp = Timestamp(timestamp),
                partitionNumber = PartitionNumber(partitionNumber),
                headers = headers
            )
        }
    }
}