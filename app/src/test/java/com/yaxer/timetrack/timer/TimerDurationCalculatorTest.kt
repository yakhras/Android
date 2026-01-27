package com.yaxer.timetrack.timer

import com.yaxer.timetrack.util.TimerDurationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for timer duration calculations.
 * Tests the core time calculations from TimerFragment.kt.
 */
class TimerDurationCalculatorTest {

    // ========== calculateElapsedMinutes Tests ==========

    @Test
    fun `calculateElapsedMinutes - no time elapsed returns 0`() {
        val totalMillis = 300_000L  // 5 minutes
        val remainingMillis = 300_000L

        val result = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)

        assertEquals(0, result)
    }

    @Test
    fun `calculateElapsedMinutes - 59 seconds elapsed returns 0`() {
        val totalMillis = 300_000L  // 5 minutes
        val remainingMillis = 241_000L  // 4:01 remaining

        val result = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)

        assertEquals(0, result)
    }

    @Test
    fun `calculateElapsedMinutes - 60 seconds elapsed returns 1`() {
        val totalMillis = 300_000L  // 5 minutes
        val remainingMillis = 240_000L  // 4:00 remaining

        val result = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)

        assertEquals(1, result)
    }

    @Test
    fun `calculateElapsedMinutes - full 5 minutes returns 5`() {
        val totalMillis = 300_000L  // 5 minutes
        val remainingMillis = 0L

        val result = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)

        assertEquals(5, result)
    }

    @Test
    fun `calculateElapsedMinutes - partial minute truncates correctly`() {
        val totalMillis = 300_000L  // 5 minutes
        val remainingMillis = 180_500L  // 3:00.5 remaining, 1:59.5 elapsed

        val result = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)

        assertEquals(1, result)  // Should truncate to 1, not round to 2
    }

    // ========== calculateDurationMinutes Tests ==========

    @Test
    fun `calculateDurationMinutes - max 23h59m59s returns 1439`() {
        val totalMillis = 86_399_000L  // 23:59:59

        val result = TimerDurationCalculator.calculateDurationMinutes(totalMillis)

        assertEquals(1439, result)
    }

    @Test
    fun `calculateDurationMinutes - 1 hour returns 60`() {
        val totalMillis = 3_600_000L  // 1 hour

        val result = TimerDurationCalculator.calculateDurationMinutes(totalMillis)

        assertEquals(60, result)
    }

    @Test
    fun `calculateDurationMinutes - 30 seconds returns 0`() {
        val totalMillis = 30_000L  // 30 seconds

        val result = TimerDurationCalculator.calculateDurationMinutes(totalMillis)

        assertEquals(0, result)
    }

    // ========== shouldShowDialog Tests ==========

    @Test
    fun `shouldShowDialog - 0 minutes returns false`() {
        val result = TimerDurationCalculator.shouldShowDialog(0)

        assertEquals(false, result)
    }

    @Test
    fun `shouldShowDialog - 1 minute returns true`() {
        val result = TimerDurationCalculator.shouldShowDialog(1)

        assertEquals(true, result)
    }

    @Test
    fun `shouldShowDialog - 100 minutes returns true`() {
        val result = TimerDurationCalculator.shouldShowDialog(100)

        assertEquals(true, result)
    }

    // ========== calculateTotalMillis Tests ==========

    @Test
    fun `calculateTotalMillis - 1 hour 30 minutes 45 seconds`() {
        val result = TimerDurationCalculator.calculateTotalMillis(1, 30, 45)

        val expected = (1 * 3600L + 30 * 60L + 45) * 1000L
        assertEquals(expected, result)
    }

    @Test
    fun `calculateTotalMillis - all zeros returns 0`() {
        val result = TimerDurationCalculator.calculateTotalMillis(0, 0, 0)

        assertEquals(0L, result)
    }

    @Test
    fun `calculateTotalMillis - max values 23h 59m 59s`() {
        val result = TimerDurationCalculator.calculateTotalMillis(23, 59, 59)

        val expected = (23 * 3600L + 59 * 60L + 59) * 1000L
        assertEquals(expected, result)
    }
}
