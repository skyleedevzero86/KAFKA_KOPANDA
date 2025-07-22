package com.sleekydz86.kopanda.domain.events

import com.sleekydz86.kopanda.domain.entities.Topic
import com.sleekydz86.kopanda.shared.domain.DomainEvent

data class TopicDeletedEvent(val topic: Topic) : DomainEvent