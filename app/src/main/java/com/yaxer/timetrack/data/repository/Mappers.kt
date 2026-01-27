package com.yaxer.timetrack.data.repository

import com.yaxer.timetrack.data.local.ProjectEntity
import com.yaxer.timetrack.data.local.SyncStatus
import com.yaxer.timetrack.data.local.TimeEntryEntity

/**
 * Extension functions to convert Odoo API response maps to Room entities
 */

/**
 * Convert Odoo project API response to ProjectEntity
 */
fun Map<String, Any>.toProjectEntity(): ProjectEntity {
    return ProjectEntity(
        id = (this["id"] as Number).toInt(),
        name = this["name"]?.toString() ?: "",
        code = null,
        active = true,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

/**
 * Convert Odoo time entry API response to TimeEntryEntity
 *
 * Note: Odoo returns project_id as either:
 * - Array: [id, "name"] when populated
 * - Boolean false when empty
 */
fun Map<String, Any>.toTimeEntryEntity(): TimeEntryEntity {
    val projectIdField = this["project_id"]

    val (projectId, projectName) = when (projectIdField) {
        is Array<*> -> {
            val id = (projectIdField[0] as? Number)?.toInt() ?: 0
            val name = projectIdField.getOrNull(1)?.toString() ?: ""
            Pair(id, name)
        }
        is List<*> -> {
            val id = (projectIdField.getOrNull(0) as? Number)?.toInt() ?: 0
            val name = projectIdField.getOrNull(1)?.toString() ?: ""
            Pair(id, name)
        }
        is Number -> Pair(projectIdField.toInt(), "")
        else -> Pair(0, "")
    }

    return TimeEntryEntity(
        id = (this["id"] as Number).toInt(),
        projectId = projectId,
        projectName = projectName,
        description = this["description"]?.toString()?.takeIf { it != "false" } ?: "",
        date = this["date"]?.toString() ?: "",
        unitAmount = (this["duration_minutes"] as? Number)?.toFloat() ?: 0f,
        // Timer fields
        startTime = this["start_time"]?.toString()?.takeIf { it != "false" },
        endTime = this["end_time"]?.toString()?.takeIf { it != "false" },
        isRunning = this["is_running"] == true,
        // Sync fields
        syncStatus = SyncStatus.SYNCED,
        localId = null,
        lastSyncedAt = System.currentTimeMillis()
    )
}
