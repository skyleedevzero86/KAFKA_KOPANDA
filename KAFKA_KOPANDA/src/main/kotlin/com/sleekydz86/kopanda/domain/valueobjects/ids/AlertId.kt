package com.sleekydz86.kopanda.domain.valueobjects.ids

import java.util.*

data class AlertId(val value: String) {
    companion object {
        fun generate(): AlertId = AlertId(UUID.randomUUID().toString())
    }
}