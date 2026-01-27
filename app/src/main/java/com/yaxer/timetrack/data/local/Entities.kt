package com.yaxer.timetrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync status for tracking offline/online state of records
 */
enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    SYNC_FAILED
}

/**
 * Operation type for sync queue
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Project entity - caches projects from Odoo
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val code: String?,
    val active: Boolean,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Time entry entity - caches time entries from Odoo
 */
@Entity(tableName = "time_entries")
data class TimeEntryEntity(
    @PrimaryKey
    val id: Int,
    val projectId: Int,
    val projectName: String,
    val description: String,
    val date: String,
    val unitAmount: Float,
    // Timer fields
    val startTime: String? = null,      // "YYYY-MM-DD HH:mm:ss"
    val endTime: String? = null,
    val isRunning: Boolean = false,
    // Sync fields
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localId: String? = null, // For entries created offline
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Sync queue entity - tracks pending offline operations
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // "project" or "time_entry"
    val entityId: String, // Can be Int ID or local UUID
    val operation: SyncOperation,
    val payload: String, // JSON payload for create/update operations
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)

/**
 * Running timer entity - persists timer state across app restarts
 */
@Entity(tableName = "running_timers")
data class RunningTimerEntity(
    @PrimaryKey
    val id: Int = 1, // Single timer, always ID 1
    val projectId: Int,
    val projectName: String,
    val description: String,
    val startTime: Long,
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false
)
