package com.sleekydz86.kopanda.application.dto.response

data class ConsumerMemberDto(
    val memberId: String,
    val clientId: String,
    val clientHost: String,
    val partitions: List<String>
)