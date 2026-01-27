package com.yaxer.timetrack.util

/**
 * Utility class for formatting duration display strings.
 * Extracted from TimerFragment for testability.
 */
object DurationFormatter {

    /**
     * Format duration in minutes to a human-readable string.
     * @param durationMinutes Duration in whole minutes
     * @return Formatted string like "Duration: 1 min" or "Duration: 1 hr 30 min"
     */
    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        return if (hours > 0) {
            String.format("Duration: %d hr %d min", hours, mins)
        } else {
            String.format("Duration: %d min", mins)
        }
    }

    /**
     * Format time display for countdown timer (HH:MM:SS).
     * @param remainingMillis Remaining time in milliseconds
     * @return Formatted string like "01:30:45"
     */
    fun formatTimerDisplay(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
