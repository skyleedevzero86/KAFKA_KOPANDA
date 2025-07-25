package com.sleekydz86.kopanda.application.dto

data class ConsumerGroupDto(
    val groupId: String,
    val state: String,
    val memberCount: Int,
    val topicCount: Int,
    val offsets: Map<String, Long>
)