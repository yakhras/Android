package com.yaxer.timetrack.util

/**
 * Utility class for timer duration calculations.
 * Extracted from TimerFragment for testability.
 */
object TimerDurationCalculator {

    /**
     * Calculate elapsed minutes from timer state.
     * @param totalMillis Total timer duration in milliseconds
     * @param remainingMillis Remaining time in milliseconds
     * @return Elapsed time in whole minutes (integer division)
     */
    fun calculateElapsedMinutes(totalMillis: Long, remainingMillis: Long): Int {
        return ((totalMillis - remainingMillis) / 60_000).toInt()
    }

    /**
     * Calculate total duration in minutes.
     * Used when timer completes naturally.
     * @param totalMillis Total timer duration in milliseconds
     * @return Total duration in whole minutes
     */
    fun calculateDurationMinutes(totalMillis: Long): Int {
        return (totalMillis / 60_000).toInt()
    }

    /**
     * Determine if add entry dialog should be shown.
     * @param elapsedMinutes Number of minutes elapsed
     * @return true if at least 1 minute has elapsed
     */
    fun shouldShowDialog(elapsedMinutes: Int): Boolean {
        return elapsedMinutes > 0
    }

    /**
     * Calculate total milliseconds from picker values.
     * @param hours Hours value (0-23)
     * @param minutes Minutes value (0-59)
     * @param seconds Seconds value (0-59)
     * @return Total milliseconds
     */
    fun calculateTotalMillis(hours: Int, minutes: Int, seconds: Int): Long {
        return ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L
    }
}
