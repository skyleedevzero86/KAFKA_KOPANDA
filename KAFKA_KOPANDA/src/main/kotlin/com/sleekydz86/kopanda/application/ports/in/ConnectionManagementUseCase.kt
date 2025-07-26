package com.sleekydz86.kopanda.application.ports.`in`

import com.sleekydz86.kopanda.application.dto.*
import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.common.ConnectionTestResult
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto
import com.sleekydz86.kopanda.application.dto.response.ConnectionHealthDto
import com.sleekydz86.kopanda.application.dto.response.PingResultDto
import com.sleekydz86.kopanda.application.dto.response.ConnectionHistoryDto
import com.sleekydz86.kopanda.application.dto.response.ConnectionMetricsDto

interface ConnectionManagementUseCase {
    suspend fun getConnections(): List<ConnectionDto>

    suspend fun getConnection(id: String): ConnectionDto

    suspend fun createConnection(request: CreateConnectionRequest): ConnectionDto

    suspend fun updateConnection(id: String, request: UpdateConnectionRequest): ConnectionDto

    suspend fun deleteConnection(id: String)

    suspend fun testConnection(request: CreateConnectionRequest): ConnectionTestResult

    suspend fun getConnectionStatus(id: String): ConnectionStatus

    suspend fun refreshAllConnectionStatuses()

    suspend fun getConnectionHealth(id: String): ConnectionHealthDto

    suspend fun getAllConnectionsHealth(): List<ConnectionHealthDto>

    suspend fun getConnectionMetrics(id: String): ConnectionMetricsDto

    suspend fun pingConnection(id: String): PingResultDto

    suspend fun getConnectionHistory(id: String, limit: Int): List<ConnectionHistoryDto>
}