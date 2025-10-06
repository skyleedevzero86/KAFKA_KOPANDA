package com.sleekydz86.kopanda.application.ports.`in`

import com.sleekydz86.kopanda.application.dto.response.AlertDto

interface AlertManagementUseCase {
    fun getActiveAlerts(connectionId: String? = null): List<AlertDto>
    fun getAlertsBySeverity(severity: String): List<AlertDto>
    fun acknowledgeAlert(alertId: String, acknowledgedBy: String): AlertDto
    fun clearAllAlerts(connectionId: String? = null)
    suspend fun checkAndCreateAlerts(connectionId: String)
    fun getAlertById(id: String): AlertDto
}