package com.sleekydz86.kopanda.application.dto.response

data class PartitionLagDto(
    val topic: String,
    val partition: Int,
    val currentOffset: Long,
    val committedOffset: Long,
    val endOffset: Long,
    val lag: Long,
    val consumerId: String?
)