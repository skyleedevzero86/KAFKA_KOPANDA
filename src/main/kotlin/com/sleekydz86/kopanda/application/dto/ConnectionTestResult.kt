package com.sleekydz86.kopanda.application.dto

data class ConnectionTestResult(
    val success: Boolean,
    val message: String,
    val latency: Long? = null,
    val brokerInfo: BrokerInfo? = null
)