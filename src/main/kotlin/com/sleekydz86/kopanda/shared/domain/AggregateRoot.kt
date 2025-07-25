package com.sleekydz86.kopanda.shared.domain

import java.util.*

abstract class AggregateRoot {
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()

    protected fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getDomainEvents(): List<DomainEvent> = domainEvents.toList()

    fun clearDomainEvents() {
        domainEvents.clear()
    }
}