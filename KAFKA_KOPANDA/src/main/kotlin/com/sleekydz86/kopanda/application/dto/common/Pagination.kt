package com.sleekydz86.kopanda.application.dto.common

data class Pagination(
    val page: Int,
    val pageSize: Int,
    val total: Long
) {
    val totalPages: Int
        get() = ((total + pageSize - 1) / pageSize).toInt()

    val hasNext: Boolean
        get() = page < totalPages

    val hasPrevious: Boolean
        get() = page > 1

    val offset: Long
        get() = (page - 1) * pageSize.toLong()
}