package com.sleekydz86.kopanda.application.dto.request

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class SendMessageRequest @JsonCreator constructor(
    @JsonProperty("key") val key: String?,
    @JsonProperty("value") val value: String,
    @JsonProperty("partition") val partition: Int?,
    @JsonProperty("headers") val headers: Map<String, String> = emptyMap()
)