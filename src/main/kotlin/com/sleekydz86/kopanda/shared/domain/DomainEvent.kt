package com.sleekydz86.kopanda.shared.domain

import java.util.*

interface DomainEvent {
    val occurredOn: Date
        get() = Date()
}