package com.sleekydz86.kopanda.domain.events

import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.shared.domain.DomainEvent

data class TopicCreatedEvent(val topic: Topic) : DomainEvent