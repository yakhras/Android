package com.yaxer.timetrack.data.repository

import com.google.gson.Gson
import com.yaxer.timetrack.OdooApiClient
import com.yaxer.timetrack.data.local.ProjectEntity
import com.yaxer.timetrack.data.local.SyncOperation
import com.yaxer.timetrack.data.local.SyncQueueEntity
import com.yaxer.timetrack.data.local.SyncStatus
import com.yaxer.timetrack.data.local.TimeEntryEntity
import com.yaxer.timetrack.data.local.TimeTrackDatabase
import com.yaxer.timetrack.data.network.NetworkMonitor
import com.yaxer.timetrack.sync.ProjectPayload
import com.yaxer.timetrack.sync.TimeEntryPayload
import com.yaxer.timetrack.sync.TimeEntryWithTimePayload
import com.yaxer.timetrack.sync.TimerPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository layer that acts as a single source of truth.
 *
 * Pattern: Cache-first with background refresh
 * - Read: Return Flow from local DB immediately
 * - Refresh: Fetch from API, update local DB
 * - Write: Save to local DB, attempt API call if online
 */
class TimeTrackRepository(
    private val database: TimeTrackDatabase,
    private val networkMonitor: NetworkMonitor
) {
    private val projectDao = database.projectDao()
    private val timeEntryDao = database.timeEntryDao()
    private val syncQueueDao = database.syncQueueDao()
    private val gson = Gson()
    private val syncMutex = Mutex()

    // ==================== Projects ====================

    /**
     * Observe cached projects as a Flow.
     * Returns immediately from local database.
     */
    fun getProjects(): Flow<List<ProjectEntity>> = projectDao.getAll()

    /**
     * Get cached projects synchronously.
     */
    suspend fun getProjectsSync(): List<ProjectEntity> = projectDao.getAllSync()

    /**
     * Refresh projects from Odoo API and update local cache.
     * Does nothing if offline.
     */
    suspend fun refreshProjects() {
        if (!networkMonitor.isOnlineNow()) return

        // Ensure authenticated
        if (!OdooApiClient.isAuthenticated) {
            OdooApiClient.authenticate() ?: return
        }

        // Fetch from API
        val apiProjects = OdooApiClient.fetchProjects()
        if (apiProjects.isEmpty()) return

        // Convert to entities and cache
        val entities = apiProjects.map { it.toProjectEntity() }
        projectDao.deleteAll()
        projectDao.insertAll(entities)
    }

    /**
     * Create a new project.
     * Returns: project ID if successful (positive for online, negative for offline).
     *
     * Online: Creates on server immediately, caches locally
     * Offline: Saves locally with negative ID, queues for sync
     */
    suspend fun createProject(name: String): Int? {
        // Try online first
        if (networkMonitor.isOnlineNow()) {
            if (!OdooApiClient.isAuthenticated) {
                OdooApiClient.authenticate()
            }
            if (OdooApiClient.isAuthenticated) {
                val id = OdooApiClient.createProject(name)
                if (id != null) {
                    projectDao.insert(ProjectEntity(id, name, null, true, SyncStatus.SYNCED))
                    return id
                }
            }
        }

        // Offline: Save locally with negative ID + queue for sync
        val localId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        projectDao.insert(ProjectEntity(
            id = localId,
            name = name,
            code = null,
            active = true,
            syncStatus = SyncStatus.PENDING_SYNC
        ))

        // Add to sync queue
        syncQueueDao.insert(SyncQueueEntity(
            entityType = "project",
            entityId = localId.toString(),
            operation = SyncOperation.CREATE,
            payload = gson.toJson(ProjectPayload(name))
        ))

        return localId
    }

    // ==================== Time Entries ====================

    /**
     * Observe cached time entries as a Flow.
     * Returns immediately from local database.
     */
    fun getTimeEntries(): Flow<List<TimeEntryEntity>> = timeEntryDao.getAll()

    /**
     * Get cached time entries synchronously.
     */
    suspend fun getTimeEntriesSync(): List<TimeEntryEntity> = timeEntryDao.getAllSync()

    /**
     * Observe cached time entries for a specific date.
     */
    fun getTimeEntriesByDate(date: String): Flow<List<TimeEntryEntity>> = timeEntryDao.getByDate(date)

    /**
     * Refresh time entries from Odoo API and update local cache.
     * Does nothing if offline.
     *
     * @param todayOnly If true, only fetches today's entries
     */
    suspend fun refreshTimeEntries(todayOnly: Boolean = false) {
        if (!networkMonitor.isOnlineNow()) return

        // Ensure authenticated
        if (!OdooApiClient.isAuthenticated) {
            OdooApiClient.authenticate() ?: return
        }

        // Fetch from API
        val apiEntries = OdooApiClient.fetchTimeEntries(todayOnly)
        if (apiEntries.isEmpty()) return

        // Convert to entities and cache
        val entities = apiEntries.map { it.toTimeEntryEntity() }

        if (todayOnly) {
            // Only replace today's entries
            val today = LocalDate.now().toString()
            val existingToday = timeEntryDao.getByDateSync(today)
            existingToday.forEach { timeEntryDao.deleteById(it.id) }
        } else {
            // Replace all entries
            timeEntryDao.deleteAll()
        }

        timeEntryDao.insertAll(entities)
    }

    /**
     * Create a new time entry.
     * Returns: entry ID if successful (positive for online, negative for offline).
     *
     * Online: Creates on server immediately, caches locally
     * Offline: Saves locally with negative ID, queues for sync
     */
    suspend fun createTimeEntry(
        projectId: Int,
        description: String? = null
    ): Int? {
        val project = projectDao.getById(projectId)
        val projectName = project?.name ?: ""

        // Try online first
        if (networkMonitor.isOnlineNow()) {
            if (!OdooApiClient.isAuthenticated) {
                OdooApiClient.authenticate()
            }
            if (OdooApiClient.isAuthenticated) {
                val id = OdooApiClient.createEntry(projectId, description)
                if (id != null) {
                    timeEntryDao.insert(TimeEntryEntity(
                        id = id,
                        projectId = projectId,
                        projectName = projectName,
                        description = description ?: "",
                        date = LocalDate.now().toString(),
                        unitAmount = 0f,
                        syncStatus = SyncStatus.SYNCED
                    ))
                    return id
                }
            }
        }

        // Offline: Save locally with negative ID + queue for sync
        val localId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        timeEntryDao.insert(TimeEntryEntity(
            id = localId,
            projectId = projectId,
            projectName = projectName,
            description = description ?: "",
            date = LocalDate.now().toString(),
            unitAmount = 0f,
            syncStatus = SyncStatus.PENDING_SYNC,
            localId = localId.toString()
        ))

        // Add to sync queue
        syncQueueDao.insert(SyncQueueEntity(
            entityType = "time_entry",
            entityId = localId.toString(),
            operation = SyncOperation.CREATE,
            payload = gson.toJson(TimeEntryPayload(projectId, description))
        ))

        return localId
    }

    /**
     * Create a time entry with full time data (for timer completion).
     * Returns: entry ID if successful (positive for online, negative for offline).
     *
     * Online: Creates on server immediately, caches locally
     * Offline: Saves locally with negative ID, queues for sync
     */
    suspend fun createTimeEntryWithTime(
        projectId: Int,
        startTime: java.time.LocalDateTime,
        endTime: java.time.LocalDateTime,
        durationMinutes: Int,
        description: String? = null
    ): Int? {
        val project = projectDao.getById(projectId)
        val projectName = project?.name ?: ""
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Try online first
        if (networkMonitor.isOnlineNow()) {
            if (!OdooApiClient.isAuthenticated) {
                OdooApiClient.authenticate()
            }
            if (OdooApiClient.isAuthenticated) {
                val id = OdooApiClient.createEntryWithTime(
                    projectId, startTime, endTime, durationMinutes, description
                )
                if (id != null) {
                    timeEntryDao.insert(TimeEntryEntity(
                        id = id,
                        projectId = projectId,
                        projectName = projectName,
                        description = description ?: "",
                        date = startTime.toLocalDate().toString(),
                        unitAmount = durationMinutes.toFloat() / 60f,
                        startTime = startTime.format(dateFormatter),
                        endTime = endTime.format(dateFormatter),
                        syncStatus = SyncStatus.SYNCED
                    ))
                    return id
                }
            }
        }

        // Offline: Save locally with negative ID + queue for sync
        val localId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        timeEntryDao.insert(TimeEntryEntity(
            id = localId,
            projectId = projectId,
            projectName = projectName,
            description = description ?: "",
            date = startTime.toLocalDate().toString(),
            unitAmount = durationMinutes.toFloat() / 60f,
            startTime = startTime.format(dateFormatter),
            endTime = endTime.format(dateFormatter),
            syncStatus = SyncStatus.PENDING_SYNC,
            localId = localId.toString()
        ))

        // Add to sync queue with full time data
        syncQueueDao.insert(SyncQueueEntity(
            entityType = "time_entry_with_time",
            entityId = localId.toString(),
            operation = SyncOperation.CREATE,
            payload = gson.toJson(TimeEntryWithTimePayload(
                projectId, startTime.toString(), endTime.toString(), durationMinutes, description
            ))
        ))

        return localId
    }

    // ==================== Timer Operations ====================

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Start timer on an entry.
     * Online: Call API immediately
     * Offline: Queue for sync, update local state
     */
    suspend fun startTimer(entryId: Int): Boolean {
        // Update local state immediately
        val entry = timeEntryDao.getById(entryId) ?: return false
        val now = LocalDateTime.now()
        timeEntryDao.update(entry.copy(
            isRunning = true,
            startTime = now.format(dateTimeFormatter)
        ))

        if (networkMonitor.isOnlineNow()) {
            if (!OdooApiClient.isAuthenticated) {
                OdooApiClient.authenticate()
            }
            if (OdooApiClient.isAuthenticated) {
                return OdooApiClient.startTimer(entryId)
            }
        }

        // Offline: Queue for sync
        syncQueueDao.insert(SyncQueueEntity(
            entityType = "timer_start",
            entityId = entryId.toString(),
            operation = SyncOperation.UPDATE,
            payload = gson.toJson(TimerPayload(entryId, "start", 0))
        ))
        return true
    }

    /**
     * Stop timer on an entry.
     * Online: Call API immediately
     * Offline: Queue for sync, update local state
     */
    suspend fun stopTimer(entryId: Int, pausedSeconds: Long): Boolean {
        // Update local state immediately
        val entry = timeEntryDao.getById(entryId) ?: return false
        val now = LocalDateTime.now()
        timeEntryDao.update(entry.copy(
            isRunning = false,
            endTime = now.format(dateTimeFormatter)
        ))

        if (networkMonitor.isOnlineNow()) {
            if (!OdooApiClient.isAuthenticated) {
                OdooApiClient.authenticate()
            }
            if (OdooApiClient.isAuthenticated) {
                return OdooApiClient.stopTimerWithPause(entryId, pausedSeconds)
            }
        }

        // Offline: Queue for sync
        syncQueueDao.insert(SyncQueueEntity(
            entityType = "timer_stop",
            entityId = entryId.toString(),
            operation = SyncOperation.UPDATE,
            payload = gson.toJson(TimerPayload(entryId, "stop", pausedSeconds))
        ))
        return true
    }

    // ==================== Sync Status ====================

    /**
     * Observe count of pending sync items.
     */
    fun getPendingSyncCount(): Flow<Int> = syncQueueDao.getCount()

    /**
     * Get count of pending sync items synchronously.
     */
    suspend fun getPendingSyncCountSync(): Int = syncQueueDao.getCountSync()

    /**
     * Check if device is currently online.
     */
    fun isOnline(): Boolean = networkMonitor.isOnlineNow()

    /**
     * Observe online status as a Flow.
     */
    fun observeOnlineStatus() = networkMonitor.isOnline

    // ==================== Sync Operations ====================

    /**
     * Process all pending sync queue items.
     * Called by SyncWorker when online.
     * Protected by mutex to prevent concurrent sync operations causing duplicates.
     */
    suspend fun syncPendingChanges() {
        syncMutex.withLock {
            if (!networkMonitor.isOnlineNow()) return@withLock

            // Ensure authenticated
            if (!OdooApiClient.isAuthenticated) {
                OdooApiClient.authenticate() ?: return@withLock
            }

            val pendingItems = syncQueueDao.getAllSync()

            for (item in pendingItems) {
                try {
                    val success = when (item.operation) {
                        SyncOperation.CREATE -> syncCreate(item)
                        SyncOperation.UPDATE -> syncUpdate(item)
                        SyncOperation.DELETE -> syncDelete(item)
                    }

                    if (success) {
                        syncQueueDao.deleteById(item.id)
                    } else {
                        // Increment retry count
                        syncQueueDao.update(item.copy(
                            retryCount = item.retryCount + 1,
                            lastError = "API call failed"
                        ))
                    }
                } catch (e: Exception) {
                    syncQueueDao.update(item.copy(
                        retryCount = item.retryCount + 1,
                        lastError = e.message
                    ))
                }
            }
        }
    }

    private suspend fun syncCreate(item: SyncQueueEntity): Boolean {
        return when (item.entityType) {
            "project" -> {
                val data = gson.fromJson(item.payload, ProjectPayload::class.java)
                val serverId = OdooApiClient.createProject(data.name)
                if (serverId != null) {
                    val localId = item.entityId.toIntOrNull()
                    // Delete local entry with negative ID
                    if (localId != null) {
                        projectDao.deleteById(localId)
                    }
                    // Insert with server ID
                    projectDao.insert(ProjectEntity(
                        id = serverId,
                        name = data.name,
                        code = null,
                        active = true,
                        syncStatus = SyncStatus.SYNCED
                    ))
                    true
                } else {
                    false
                }
            }
            "time_entry" -> {
                val data = gson.fromJson(item.payload, TimeEntryPayload::class.java)
                val serverId = OdooApiClient.createEntry(data.projectId, data.description)
                if (serverId != null) {
                    val localId = item.entityId.toIntOrNull()
                    // Delete local entry with negative ID
                    if (localId != null) {
                        timeEntryDao.deleteById(localId)
                    }
                    // Fetch the project name from cache
                    val project = projectDao.getById(data.projectId)
                    val projectName = project?.name ?: ""

                    // Insert with server ID
                    timeEntryDao.insert(TimeEntryEntity(
                        id = serverId,
                        projectId = data.projectId,
                        projectName = projectName,
                        description = data.description ?: "",
                        date = LocalDate.now().toString(),
                        unitAmount = 0f,
                        syncStatus = SyncStatus.SYNCED
                    ))
                    true
                } else {
                    false
                }
            }
            "time_entry_with_time" -> {
                val data = gson.fromJson(item.payload, TimeEntryWithTimePayload::class.java)
                val startTime = LocalDateTime.parse(data.startTime)
                val endTime = LocalDateTime.parse(data.endTime)
                val serverId = OdooApiClient.createEntryWithTime(
                    data.projectId, startTime, endTime, data.durationMinutes, data.description
                )
                if (serverId != null) {
                    val localId = item.entityId.toIntOrNull()
                    // Delete local entry with negative ID
                    if (localId != null) {
                        timeEntryDao.deleteById(localId)
                    }
                    // Fetch the project name from cache
                    val project = projectDao.getById(data.projectId)
                    val projectName = project?.name ?: ""

                    // Insert with server ID
                    timeEntryDao.insert(TimeEntryEntity(
                        id = serverId,
                        projectId = data.projectId,
                        projectName = projectName,
                        description = data.description ?: "",
                        date = startTime.toLocalDate().toString(),
                        unitAmount = data.durationMinutes.toFloat() / 60f,
                        syncStatus = SyncStatus.SYNCED
                    ))
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    private suspend fun syncUpdate(item: SyncQueueEntity): Boolean {
        return when (item.entityType) {
            "timer_start" -> {
                val data = gson.fromJson(item.payload, TimerPayload::class.java)
                OdooApiClient.startTimer(data.entryId)
            }
            "timer_stop" -> {
                val data = gson.fromJson(item.payload, TimerPayload::class.java)
                OdooApiClient.stopTimerWithPause(data.entryId, data.pausedSeconds)
            }
            else -> true
        }
    }

    private suspend fun syncDelete(item: SyncQueueEntity): Boolean {
        return when (item.entityType) {
            "time_entry" -> {
                val id = item.entityId.toIntOrNull() ?: return false
                OdooApiClient.deleteTimeEntry(id)
            }
            else -> false
        }
    }
}
