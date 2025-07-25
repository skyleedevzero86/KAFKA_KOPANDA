package com.sleekydz86.kopanda.application.ports.`in`

import com.sleekydz86.kopanda.application.dto.*
import com.sleekydz86.kopanda.application.dto.common.ConnectionStatus
import com.sleekydz86.kopanda.application.dto.common.ConnectionTestResult
import com.sleekydz86.kopanda.application.dto.request.CreateConnectionRequest
import com.sleekydz86.kopanda.application.dto.request.UpdateConnectionRequest
import com.sleekydz86.kopanda.application.dto.response.ConnectionDto

interface ConnectionManagementUseCase {
    suspend fun getConnections(): List<ConnectionDto>

    suspend fun getConnection(id: String): ConnectionDto

    suspend fun createConnection(request: CreateConnectionRequest): ConnectionDto

    suspend fun updateConnection(id: String, request: UpdateConnectionRequest): ConnectionDto

    suspend fun deleteConnection(id: String)

    suspend fun testConnection(request: CreateConnectionRequest): ConnectionTestResult

    suspend fun getConnectionStatus(id: String): ConnectionStatus

    suspend fun refreshAllConnectionStatuses()
}