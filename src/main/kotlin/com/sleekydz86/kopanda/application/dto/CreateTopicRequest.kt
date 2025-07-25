package com.sleekydz86.kopanda.application.dto

data class CreateTopicRequest(
    val name: String,
    val partitions: Int,
    val replicationFactor: Int,
    val config: Map<String, String> = emptyMap()
)