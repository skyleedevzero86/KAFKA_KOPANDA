package com.sleekydz86.kopanda.domain.valueobjects

import jakarta.persistence.Embeddable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.CollectionTable
import jakarta.persistence.JoinColumn

@Embeddable
data class TopicConfig(
    @Column(name = "partition_count")
    val partitionCount: Int,

    @Column(name = "replication_factor")
    val replicationFactor: Int,

    @ElementCollection
    @CollectionTable(name = "topic_configs", joinColumns = [JoinColumn(name = "topic_id")])
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value")
    val config: Map<String, String> = emptyMap()
) {
    init {
        require(partitionCount > 0) { "Partition count must be positive" }
        require(replicationFactor > 0) { "Replication factor must be positive" }
        require(replicationFactor <= 10) { "Replication factor cannot exceed 10" }
    }

    fun getConfigValue(key: String): String? = config[key]

    fun hasConfig(key: String): Boolean = config.containsKey(key)

    fun getRetentionMs(): Long? = config["retention.ms"]?.toLongOrNull()

    fun getCleanupPolicy(): String? = config["cleanup.policy"]

    fun getMaxMessageBytes(): Int? = config["max.message.bytes"]?.toIntOrNull()

    override fun toString(): String =
        "TopicConfig(partitions=$partitionCount, replication=$replicationFactor, configs=${config.size})"
}