package com.sleekydz86.kopanda.infrastructure.persistence.entities

import com.sleekydz86.kopanda.domain.entities.Activity
import com.sleekydz86.kopanda.domain.valueobjects.ids.ActivityId
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityMessage
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityTitle
import com.sleekydz86.kopanda.domain.valueobjects.common.ActivityType

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "activities")
class ActivityEntity(
    @Id
    @Column(name = "activity_id", nullable = false, length = 36)
    val id: String,

    @Column(name = "activity_type", nullable = false, length = 50)
    val type: String,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "message", nullable = false, length = 500)
    val message: String,

    @Column(name = "connection_id", length = 36)
    val connectionId: String? = null,

    @Column(name = "topic_name", length = 249)
    val topicName: String? = null,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = "",
        type = "ERROR_OCCURRED",
        title = "",
        message = "",
        connectionId = null,
        topicName = null,
        timestamp = LocalDateTime.now()
    )

    fun toDomain(): Activity {
        val activity = Activity(
            type = ActivityType(this.type),
            title = ActivityTitle(title),
            message = ActivityMessage(message),
            connectionId = connectionId,
            topicName = topicName,
            timestamp = timestamp
        )

        activity.setId(ActivityId(this.id))
        return activity
    }

    companion object {
        fun fromDomain(activity: Activity): ActivityEntity {
            return ActivityEntity(
                id = activity.getId().value,
                type = activity.type.value,
                title = activity.title.value,
                message = activity.message.value,
                connectionId = activity.connectionId,
                topicName = activity.topicName,
                timestamp = activity.timestamp
            )
        }
    }
}