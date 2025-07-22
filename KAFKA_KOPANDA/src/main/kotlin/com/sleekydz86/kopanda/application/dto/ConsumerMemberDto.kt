package com.sleekydz86.kopanda.application.dto

data class ConsumerMemberDto(
    val memberId: String,
    val clientId: String,
    val clientHost: String,
    val partitions: List<String>
)