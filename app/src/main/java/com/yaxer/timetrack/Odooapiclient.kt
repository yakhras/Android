package com.yaxer.timetrack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

/**
 * Odoo API Client (Singleton)
 * Handles authentication and data fetching from Odoo server
 */
object OdooApiClient {

    private var userId: Int? = null

    val isAuthenticated: Boolean
        get() = userId != null

    /**
     * Authenticate with Odoo server
     * Returns: user ID if successful, null if failed
     */
    suspend fun authenticate(): Int? = withContext(Dispatchers.IO) {
        try {
            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/common")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val result = client.execute(
                "authenticate",
                listOf(
                    OdooConfig.DATABASE,
                    OdooConfig.USERNAME,
                    OdooConfig.API_KEY,
                    emptyMap<String, Any>()
                )
            ) as Int

            userId = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch all active projects
     */
    suspend fun fetchProjects(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext emptyList()

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val result = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.project",
                    "search_read",
                    listOf(listOf(listOf("active", "=", true))),
                    mapOf("fields" to listOf("id", "name"))
                )
            ) as Array<*>

            result.map { it as Map<String, Any> }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch time entries (optionally filtered by date)
     */
    suspend fun fetchTimeEntries(todayOnly: Boolean = false): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext emptyList()

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            // Build domain filter
            val domain = if (todayOnly) {
                val today = java.time.LocalDate.now().toString()
                listOf(listOf("date", "=", today))
            } else {
                emptyList<List<Any>>()
            }

            val result = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "search_read",
                    listOf(domain),
                    mapOf(
                        "fields" to listOf("id", "date", "project_id", "start_time", "end_time", "duration_minutes", "description", "is_running"),
                        "order" to "start_time desc"
                    )
                )
            ) as Array<*>

            result.map { it as Map<String, Any> }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Create a new project
     * Returns: new project ID if successful, null if failed
     */
    suspend fun createProject(name: String): Int? = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext null

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val result = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.project",
                    "create",
                    listOf(mapOf(
                        "name" to name,
                        "active" to true
                    ))
                )
            ) as Int

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create a new time entry with full time data
     * Returns: new entry ID if successful, null if failed
     */
    suspend fun createEntryWithTime(
        projectId: Int,
        startTime: java.time.LocalDateTime,
        endTime: java.time.LocalDateTime,
        durationMinutes: Int,
        description: String? = null
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext null

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val values = mutableMapOf<String, Any>(
                "project_id" to projectId,
                "date" to startTime.toLocalDate().toString(),
                "start_time" to startTime.format(formatter),
                "end_time" to endTime.format(formatter),
                "duration_minutes" to durationMinutes
            )
            if (!description.isNullOrEmpty()) {
                values["description"] = description
            }

            val result = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "create",
                    listOf(values)
                )
            ) as Int

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create a new time entry
     * Returns: new entry ID if successful, null if failed
     */
    suspend fun createEntry(
        projectId: Int,
        description: String? = null
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext null

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val values = mutableMapOf<String, Any>(
                "project_id" to projectId,
                "date" to java.time.LocalDate.now().toString()
            )
            if (!description.isNullOrEmpty()) {
                values["description"] = description
            }

            val result = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "create",
                    listOf(values)
                )
            ) as Int

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Start timer on an entry using write method
     */
    suspend fun startTimer(entryId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext false

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            // Use write to update the entry directly
            val now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val today = java.time.LocalDate.now().toString()

            client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "write",
                    listOf(
                        listOf(entryId),
                        mapOf(
                            "start_time" to now,
                            "end_time" to false,
                            "date" to today
                        )
                    )
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Stop timer on an entry with pause adjustment
     */
    suspend fun stopTimerWithPause(entryId: Int, pausedSeconds: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext false

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            // First get the entry to calculate duration
            val entries = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "read",
                    listOf(listOf(entryId)),
                    mapOf("fields" to listOf("start_time"))
                )
            ) as Array<*>

            if (entries.isEmpty()) return@withContext false

            val entry = entries[0] as Map<*, *>
            val startTimeStr = entry["start_time"]?.toString()

            val now = java.time.LocalDateTime.now()
            val nowStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            // Calculate duration minus paused time
            var durationMinutes = 0
            if (startTimeStr != null && startTimeStr != "false") {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val startTime = java.time.LocalDateTime.parse(startTimeStr, formatter)
                val totalSeconds = java.time.Duration.between(startTime, now).seconds - pausedSeconds
                durationMinutes = (totalSeconds / 60).toInt()
            }

            // Update the entry
            client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "write",
                    listOf(
                        listOf(entryId),
                        mapOf(
                            "end_time" to nowStr,
                            "duration_minutes" to durationMinutes
                        )
                    )
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Stop timer on an entry using write method
     */
    suspend fun stopTimer(entryId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext false

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            // First get the entry to calculate duration
            val entries = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "read",
                    listOf(listOf(entryId)),
                    mapOf("fields" to listOf("start_time"))
                )
            ) as Array<*>

            if (entries.isEmpty()) return@withContext false

            val entry = entries[0] as Map<*, *>
            val startTimeStr = entry["start_time"]?.toString()

            val now = java.time.LocalDateTime.now()
            val nowStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            // Calculate duration
            var durationMinutes = 0
            if (startTimeStr != null && startTimeStr != "false") {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val startTime = java.time.LocalDateTime.parse(startTimeStr, formatter)
                durationMinutes = java.time.Duration.between(startTime, now).toMinutes().toInt()
            }

            // Update the entry
            client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "write",
                    listOf(
                        listOf(entryId),
                        mapOf(
                            "end_time" to nowStr,
                            "duration_minutes" to durationMinutes
                        )
                    )
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create a new time entry (simplified version for repository)
     * Returns: new entry ID if successful, null if failed
     */
    suspend fun createTimeEntry(
        date: String,
        projectId: Int,
        startTime: String,
        endTime: String,
        durationMinutes: Int,
        description: String
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext null

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val values = mutableMapOf<String, Any>(
                "date" to date,
                "project_id" to projectId,
                "duration_minutes" to durationMinutes
            )

            if (startTime.isNotEmpty()) values["start_time"] = startTime
            if (endTime.isNotEmpty()) values["end_time"] = endTime
            if (description.isNotEmpty()) values["description"] = description

            val result = client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "create",
                    listOf(values)
                )
            ) as Int

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Update an existing time entry
     * Returns: true if successful, false if failed
     */
    suspend fun updateTimeEntry(
        id: Int,
        date: String,
        projectId: Int,
        startTime: String,
        endTime: String,
        durationMinutes: Int,
        description: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext false

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            val values = mutableMapOf<String, Any>(
                "date" to date,
                "project_id" to projectId,
                "duration_minutes" to durationMinutes
            )

            if (startTime.isNotEmpty()) values["start_time"] = startTime
            if (endTime.isNotEmpty()) values["end_time"] = endTime
            if (description.isNotEmpty()) values["description"] = description

            client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "write",
                    listOf(
                        listOf(id),
                        values
                    )
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a time entry
     * Returns: true if successful, false if failed
     */
    suspend fun deleteTimeEntry(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = userId ?: return@withContext false

            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/object")
            }
            val client = XmlRpcClient()
            client.setConfig(config)

            client.execute(
                "execute_kw",
                listOf(
                    OdooConfig.DATABASE,
                    uid,
                    OdooConfig.API_KEY,
                    "timetrack.entry",
                    "unlink",
                    listOf(listOf(id))
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
