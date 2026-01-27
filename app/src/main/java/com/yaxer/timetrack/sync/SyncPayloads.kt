package com.yaxer.timetrack.sync

/**
 * Payload for creating a project offline.
 */
data class ProjectPayload(
    val name: String
)

/**
 * Payload for creating a time entry offline.
 */
data class TimeEntryPayload(
    val projectId: Int,
    val description: String?
)

/**
 * Payload for timer operations (start/stop).
 */
data class TimerPayload(
    val entryId: Int,
    val action: String,  // "start" or "stop"
    val pausedSeconds: Long = 0
)

/**
 * Payload for creating a time entry with full time data (offline timer).
 */
data class TimeEntryWithTimePayload(
    val projectId: Int,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val description: String?
)

