package com.yaxer.timetrack.timer

import com.yaxer.timetrack.util.TimerDurationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Parameterized boundary tests for timer dialog display logic.
 * Critical boundary: 59 seconds = no dialog, 60 seconds = dialog shown
 */
@RunWith(Parameterized::class)
class TimerBoundaryTest(
    private val testName: String,
    private val totalMillis: Long,
    private val remainingMillis: Long,
    private val expectedElapsedMinutes: Int,
    private val expectedShowDialog: Boolean
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            // testName, totalMillis, remainingMillis, expectedElapsedMinutes, expectedShowDialog
            arrayOf("No time elapsed", 300_000L, 300_000L, 0, false),
            arrayOf("1 second elapsed", 300_000L, 299_000L, 0, false),
            arrayOf("59 seconds elapsed", 300_000L, 241_000L, 0, false),
            arrayOf("59.9 seconds elapsed", 300_000L, 240_100L, 0, false),
            arrayOf("60 seconds elapsed (boundary)", 300_000L, 240_000L, 1, true),
            arrayOf("61 seconds elapsed", 300_000L, 239_000L, 1, true),
            arrayOf("119 seconds elapsed", 300_000L, 181_000L, 1, true),
            arrayOf("120 seconds elapsed", 300_000L, 180_000L, 2, true),
            arrayOf("Full duration (5 min)", 300_000L, 0L, 5, true),

            // Additional edge cases
            arrayOf("1 minute timer - just started", 60_000L, 60_000L, 0, false),
            arrayOf("1 minute timer - half way", 60_000L, 30_000L, 0, false),
            arrayOf("1 minute timer - completed", 60_000L, 0L, 1, true),
            arrayOf("30 second timer - completed", 30_000L, 0L, 0, false),
            arrayOf("Max timer (23:59:59) - completed", 86_399_000L, 0L, 1439, true),
            arrayOf("Max timer - 1 minute elapsed", 86_399_000L, 86_339_000L, 1, true)
        )
    }

    @Test
    fun `elapsed minutes calculation`() {
        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)
        assertEquals(
            "Elapsed minutes mismatch for: $testName",
            expectedElapsedMinutes,
            elapsedMinutes
        )
    }

    @Test
    fun `show dialog decision`() {
        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)
        val shouldShow = TimerDurationCalculator.shouldShowDialog(elapsedMinutes)
        assertEquals(
            "Show dialog mismatch for: $testName",
            expectedShowDialog,
            shouldShow
        )
    }
}

/**
 * Non-parameterized boundary tests for additional edge cases.
 */
class TimerBoundaryEdgeCasesTest {

    @Test
    fun `boundary at exactly 59999ms elapsed is 0 minutes`() {
        val totalMillis = 300_000L
        val remainingMillis = 240_001L  // 59.999 seconds elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)
        val shouldShow = TimerDurationCalculator.shouldShowDialog(elapsedMinutes)

        assertEquals(0, elapsedMinutes)
        assertEquals(false, shouldShow)
    }

    @Test
    fun `boundary at exactly 60000ms elapsed is 1 minute`() {
        val totalMillis = 300_000L
        val remainingMillis = 240_000L  // Exactly 60 seconds elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)
        val shouldShow = TimerDurationCalculator.shouldShowDialog(elapsedMinutes)

        assertEquals(1, elapsedMinutes)
        assertEquals(true, shouldShow)
    }

    @Test
    fun `multiple minute boundaries`() {
        val totalMillis = 600_000L  // 10 minutes

        // Test each minute boundary
        for (minutes in 0..10) {
            val remainingMillis = totalMillis - (minutes * 60_000L)
            val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)
            assertEquals("At $minutes minutes", minutes, elapsedMinutes)
        }
    }

    @Test
    fun `timer started but immediately stopped`() {
        val totalMillis = 300_000L
        val remainingMillis = 300_000L

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)
        val shouldShow = TimerDurationCalculator.shouldShowDialog(elapsedMinutes)

        assertEquals(0, elapsedMinutes)
        assertEquals(false, shouldShow)
    }

    @Test
    fun `timer with fractional second remaining at completion`() {
        // Timer finishes with small remainder due to timer precision
        val totalMillis = 300_000L
        val remainingMillis = 500L  // 0.5 seconds remaining

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(totalMillis, remainingMillis)

        // Should still count as 4 minutes (299.5 seconds / 60 = 4.99)
        assertEquals(4, elapsedMinutes)
    }
}
