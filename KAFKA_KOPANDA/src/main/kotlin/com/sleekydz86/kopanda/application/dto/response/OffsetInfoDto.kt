package com.sleekydz86.kopanda.application.dto.response

data class OffsetInfoDto(
    val topicName: String,
    val partitionNumber: Int,
    val currentOffset: Long,
    val committedOffset: Long,
    val endOffset: Long,
    val lag: Long,
    val consumerGroup: String?,
    val lastCommitTime: java.time.LocalDateTime?
)
