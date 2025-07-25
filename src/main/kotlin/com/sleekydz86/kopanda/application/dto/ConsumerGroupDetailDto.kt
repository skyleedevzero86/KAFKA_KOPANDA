package com.sleekydz86.kopanda.application.dto

data class ConsumerGroupDetailDto(
    val groupId: String,
    val state: String,
    val members: List<ConsumerMemberDto>,
    val offsets: Map<String, Long>
)