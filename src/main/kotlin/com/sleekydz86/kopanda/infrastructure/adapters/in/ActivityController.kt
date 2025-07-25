package com.sleekydz86.kopanda.infrastructure.adapters.`in`

import com.sleekydz86.kopanda.application.dto.ActivityDto
import com.sleekydz86.kopanda.application.ports.`in`.ActivityManagementUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/activities")
class ActivityController(
    private val activityManagementUseCase: ActivityManagementUseCase
) {

    @GetMapping
    suspend fun getRecentActivities(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ActivityDto>> {
        val activities = activityManagementUseCase.getRecentActivities(limit)
        return ResponseEntity.ok(activities)
    }

    @GetMapping("/connection/{connectionId}")
    suspend fun getActivitiesByConnection(
        @PathVariable connectionId: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ActivityDto>> {
        val activities = activityManagementUseCase.getActivitiesByConnection(connectionId, limit)
        return ResponseEntity.ok(activities)
    }
}