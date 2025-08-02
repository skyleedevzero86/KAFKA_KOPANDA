package com.sleekydz86.kopanda.domain.entities

import com.sleekydz86.kopanda.application.dto.enums.ActivityType
import com.sleekydz86.kopanda.domain.valueobjects.ids.ActivityId
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityMessage
import com.sleekydz86.kopanda.domain.valueobjects.names.ActivityTitle
import com.sleekydz86.kopanda.shared.domain.AggregateRoot
import java.time.LocalDateTime

class Activity(
    val type: ActivityType,
    val title: ActivityTitle,
    val message: ActivityMessage,
    val connectionId: String? = null,
    val topicName: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : AggregateRoot() {

    private var id: ActivityId = ActivityId.generate()

    fun getId(): ActivityId = id

    fun setId(activityId: ActivityId) {
        this.id = activityId
    }

    companion object {
        fun createConnectionCreated(connectionName: String, connectionId: String): Activity {
            return Activity(
                type = ActivityType.CONNECTION_CREATED,
                title = ActivityTitle("새 연결이 생성되었습니다"),
                message = ActivityMessage("새 연결이 생성되었습니다: $connectionName"),
                connectionId = connectionId
            )
        }

        fun createTopicCreated(topicName: String): Activity {
            return Activity(
                type = ActivityType.TOPIC_CREATED,
                title = ActivityTitle("토픽이 생성되었습니다"),
                message = ActivityMessage("토픽 \"$topicName\"이 생성되었습니다"),
                topicName = topicName
            )
        }

        fun createConnectionOffline(connectionName: String, connectionId: String): Activity {
            return Activity(
                type = ActivityType.CONNECTION_OFFLINE,
                title = ActivityTitle("연결이 오프라인 상태가 되었습니다"),
                message = ActivityMessage("연결 \"$connectionName\"이 오프라인 상태가 되었습니다"),
                connectionId = connectionId
            )
        }

        fun createTopicDeleted(topicName: String): Activity {
            return Activity(
                type = ActivityType.TOPIC_DELETED,
                title = ActivityTitle("토픽이 삭제되었습니다"),
                message = ActivityMessage("토픽 \"$topicName\"이 삭제되었습니다"),
                topicName = topicName
            )
        }
    }
}