package com.sleekydz86.kopanda.application.dto.response

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)