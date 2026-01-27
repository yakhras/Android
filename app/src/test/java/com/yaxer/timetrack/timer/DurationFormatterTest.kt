package com.yaxer.timetrack.timer

import com.yaxer.timetrack.util.DurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for duration display formatting.
 * Tests the formatting logic for the add entry dialog.
 */
class DurationFormatterTest {

    // ========== formatDuration Tests - Minutes Only ==========

    @Test
    fun `formatDuration - 1 minute`() {
        val result = DurationFormatter.formatDuration(1)
        assertEquals("Duration: 1 min", result)
    }

    @Test
    fun `formatDuration - 30 minutes`() {
        val result = DurationFormatter.formatDuration(30)
        assertEquals("Duration: 30 min", result)
    }

    @Test
    fun `formatDuration - 59 minutes`() {
        val result = DurationFormatter.formatDuration(59)
        assertEquals("Duration: 59 min", result)
    }

    // ========== formatDuration Tests - Hours + Minutes ==========

    @Test
    fun `formatDuration - 60 minutes shows 1 hr 0 min`() {
        val result = DurationFormatter.formatDuration(60)
        assertEquals("Duration: 1 hr 0 min", result)
    }

    @Test
    fun `formatDuration - 61 minutes shows 1 hr 1 min`() {
        val result = DurationFormatter.formatDuration(61)
        assertEquals("Duration: 1 hr 1 min", result)
    }

    @Test
    fun `formatDuration - 90 minutes shows 1 hr 30 min`() {
        val result = DurationFormatter.formatDuration(90)
        assertEquals("Duration: 1 hr 30 min", result)
    }

    @Test
    fun `formatDuration - 120 minutes shows 2 hr 0 min`() {
        val result = DurationFormatter.formatDuration(120)
        assertEquals("Duration: 2 hr 0 min", result)
    }

    @Test
    fun `formatDuration - 1439 minutes shows 23 hr 59 min`() {
        val result = DurationFormatter.formatDuration(1439)
        assertEquals("Duration: 23 hr 59 min", result)
    }

    // ========== formatDuration Edge Cases ==========

    @Test
    fun `formatDuration - 0 minutes shows 0 min`() {
        val result = DurationFormatter.formatDuration(0)
        assertEquals("Duration: 0 min", result)
    }

    @Test
    fun `formatDuration - large hour value`() {
        val result = DurationFormatter.formatDuration(600)  // 10 hours
        assertEquals("Duration: 10 hr 0 min", result)
    }

    // ========== formatTimerDisplay Tests ==========

    @Test
    fun `formatTimerDisplay - zero time`() {
        val result = DurationFormatter.formatTimerDisplay(0)
        assertEquals("00:00:00", result)
    }

    @Test
    fun `formatTimerDisplay - 1 second`() {
        val result = DurationFormatter.formatTimerDisplay(1000)
        assertEquals("00:00:01", result)
    }

    @Test
    fun `formatTimerDisplay - 59 seconds`() {
        val result = DurationFormatter.formatTimerDisplay(59_000)
        assertEquals("00:00:59", result)
    }

    @Test
    fun `formatTimerDisplay - 1 minute`() {
        val result = DurationFormatter.formatTimerDisplay(60_000)
        assertEquals("00:01:00", result)
    }

    @Test
    fun `formatTimerDisplay - 1 minute 30 seconds`() {
        val result = DurationFormatter.formatTimerDisplay(90_000)
        assertEquals("00:01:30", result)
    }

    @Test
    fun `formatTimerDisplay - 1 hour`() {
        val result = DurationFormatter.formatTimerDisplay(3_600_000)
        assertEquals("01:00:00", result)
    }

    @Test
    fun `formatTimerDisplay - 1 hour 30 minutes 45 seconds`() {
        val millis = (1 * 3600 + 30 * 60 + 45) * 1000L
        val result = DurationFormatter.formatTimerDisplay(millis)
        assertEquals("01:30:45", result)
    }

    @Test
    fun `formatTimerDisplay - max time 23 59 59`() {
        val millis = (23 * 3600 + 59 * 60 + 59) * 1000L
        val result = DurationFormatter.formatTimerDisplay(millis)
        assertEquals("23:59:59", result)
    }

    @Test
    fun `formatTimerDisplay - partial second truncates`() {
        // 1.5 seconds should display as 1 second
        val result = DurationFormatter.formatTimerDisplay(1500)
        assertEquals("00:00:01", result)
    }

    @Test
    fun `formatTimerDisplay - formats with leading zeros`() {
        val millis = (5 * 3600 + 3 * 60 + 7) * 1000L  // 5:03:07
        val result = DurationFormatter.formatTimerDisplay(millis)
        assertEquals("05:03:07", result)
    }
}
