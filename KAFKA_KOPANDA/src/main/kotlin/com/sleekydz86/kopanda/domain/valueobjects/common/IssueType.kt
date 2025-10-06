package com.sleekydz86.kopanda.domain.valueobjects.common

enum class IssueType {
    UNDER_REPLICATED,
    OFFLINE_PARTITION,
    LEADER_NOT_AVAILABLE,
    REPLICATION_FACTOR_MISMATCH,
    CONSUMER_LAG,
    CONNECTION_ERROR
}
