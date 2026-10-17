package com.sleekydz86.kopanda.domain.valueobjects.topic

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
        require(partitionCount > 0) { "파티션 수는 1 이상이어야 합니다" }
        require(replicationFactor > 0) { "복제 계수는 1 이상이어야 합니다" }
        require(replicationFactor <= 10) { "복제 계수는 10을 넘을 수 없습니다" }
    }

    fun getConfigValue(key: String): String? = config[key]

    fun hasConfig(key: String): Boolean = config.containsKey(key)

    fun getRetentionMs(): Long? = config["retention.ms"]?.toLongOrNull()

    fun getCleanupPolicy(): String? = config["cleanup.policy"]

    fun getMaxMessageBytes(): Int? = config["max.message.bytes"]?.toIntOrNull()

    override fun toString(): String =
        "TopicConfig(partitions=$partitionCount, replication=$replicationFactor, configs=${config.size})"
}