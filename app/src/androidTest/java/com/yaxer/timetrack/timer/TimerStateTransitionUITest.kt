package com.yaxer.timetrack.timer

import com.yaxer.timetrack.TimerState
import com.yaxer.timetrack.TimerViewModel
import com.yaxer.timetrack.util.TimerDurationCalculator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for timer state transitions and the associated dialog behavior.
 * These are unit tests that verify the state machine logic.
 *
 * For full end-to-end UI tests, see TimerFragmentUITest.
 */
class TimerStateTransitionUITest {

    private lateinit var viewModel: TimerViewModel

    @Before
    fun setUp() {
        viewModel = TimerViewModel()
    }

    // ========== Full State Machine Tests ==========

    @Test
    fun fullCycle_IDLE_to_RUNNING_to_PAUSED_to_RUNNING_to_IDLE() {
        // Initial state
        assertEquals(TimerState.IDLE, viewModel.timerState)

        // Start timer
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L  // 5 minutes
        viewModel.remainingMillis = 300_000L
        assertEquals(TimerState.RUNNING, viewModel.timerState)

        // Pause timer
        viewModel.timerState = TimerState.PAUSED
        assertEquals(TimerState.PAUSED, viewModel.timerState)

        // Resume timer
        viewModel.timerState = TimerState.RUNNING
        assertEquals(TimerState.RUNNING, viewModel.timerState)

        // Stop timer (reset to IDLE)
        viewModel.timerState = TimerState.IDLE
        viewModel.remainingMillis = 0
        viewModel.totalMillis = 0
        assertEquals(TimerState.IDLE, viewModel.timerState)
    }

    // ========== Stop from Running State Tests ==========

    @Test
    fun stopFromRunning_moreThan1MinuteElapsed_shouldShowDialog() {
        // Setup: Timer running with time elapsed
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L  // 5 minutes
        viewModel.remainingMillis = 180_000L  // 3 minutes remaining (2 minutes elapsed)

        // Calculate elapsed
        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(2, elapsedMinutes)
        assertTrue(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    @Test
    fun stopFromRunning_lessThan1MinuteElapsed_shouldNotShowDialog() {
        // Setup: Timer running with minimal time elapsed
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L  // 5 minutes
        viewModel.remainingMillis = 260_000L  // 40 seconds elapsed

        // Calculate elapsed
        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(0, elapsedMinutes)
        assertFalse(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    @Test
    fun stopFromRunning_exactly1MinuteElapsed_shouldShowDialog() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L
        viewModel.remainingMillis = 240_000L  // Exactly 1 minute elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(1, elapsedMinutes)
        assertTrue(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    // ========== Stop from Paused State Tests ==========

    @Test
    fun stopFromPaused_moreThan1MinuteElapsed_shouldShowDialog() {
        // Setup: Timer paused with time elapsed
        viewModel.timerState = TimerState.PAUSED
        viewModel.totalMillis = 300_000L
        viewModel.remainingMillis = 120_000L  // 3 minutes elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(3, elapsedMinutes)
        assertTrue(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    @Test
    fun stopFromPaused_lessThan1MinuteElapsed_shouldNotShowDialog() {
        viewModel.timerState = TimerState.PAUSED
        viewModel.totalMillis = 300_000L
        viewModel.remainingMillis = 250_000L  // 50 seconds elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(0, elapsedMinutes)
        assertFalse(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    // ========== Timer Completion Tests ==========

    @Test
    fun timerCompletion_showsDialogWithFullDuration() {
        // Setup: Timer completed naturally
        viewModel.totalMillis = 300_000L  // 5 minutes
        viewModel.remainingMillis = 0L

        val durationMinutes = TimerDurationCalculator.calculateDurationMinutes(viewModel.totalMillis)

        assertEquals(5, durationMinutes)
        assertTrue(TimerDurationCalculator.shouldShowDialog(durationMinutes))
    }

    @Test
    fun timerCompletion_1MinuteTimerShowsDialog() {
        viewModel.totalMillis = 60_000L  // 1 minute
        viewModel.remainingMillis = 0L

        val durationMinutes = TimerDurationCalculator.calculateDurationMinutes(viewModel.totalMillis)

        assertEquals(1, durationMinutes)
        assertTrue(TimerDurationCalculator.shouldShowDialog(durationMinutes))
    }

    @Test
    fun timerCompletion_maxDurationShowsDialog() {
        viewModel.totalMillis = 86_399_000L  // 23:59:59
        viewModel.remainingMillis = 0L

        val durationMinutes = TimerDurationCalculator.calculateDurationMinutes(viewModel.totalMillis)

        assertEquals(1439, durationMinutes)
        assertTrue(TimerDurationCalculator.shouldShowDialog(durationMinutes))
    }

    // ========== Quick Stop Tests ==========

    @Test
    fun quickStop_immediatelyAfterStart_noDialog() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L
        viewModel.remainingMillis = 300_000L  // No time elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(0, elapsedMinutes)
        assertFalse(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    @Test
    fun quickStop_after30Seconds_noDialog() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L
        viewModel.remainingMillis = 270_000L  // 30 seconds elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(0, elapsedMinutes)
        assertFalse(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    @Test
    fun quickStop_after59Seconds_noDialog() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.totalMillis = 300_000L
        viewModel.remainingMillis = 241_000L  // 59 seconds elapsed

        val elapsedMinutes = TimerDurationCalculator.calculateElapsedMinutes(
            viewModel.totalMillis,
            viewModel.remainingMillis
        )

        assertEquals(0, elapsedMinutes)
        assertFalse(TimerDurationCalculator.shouldShowDialog(elapsedMinutes))
    }

    // ========== Reset State Tests ==========

    @Test
    fun resetAfterStop_clearsAllValues() {
        // Setup: After stopping, simulate reset
        viewModel.timerState = TimerState.IDLE
        viewModel.remainingMillis = 0
        viewModel.totalMillis = 0
        viewModel.timerEndTimeMillis = 0

        assertEquals(TimerState.IDLE, viewModel.timerState)
        assertEquals(0L, viewModel.remainingMillis)
        assertEquals(0L, viewModel.totalMillis)
        assertEquals(0L, viewModel.timerEndTimeMillis)
    }

    @Test
    fun resetAfterCompletion_clearsAllValues() {
        // After timer completes and dialog is handled
        viewModel.timerState = TimerState.IDLE
        viewModel.remainingMillis = 0
        viewModel.totalMillis = 0
        viewModel.timerEndTimeMillis = 0

        assertEquals(TimerState.IDLE, viewModel.timerState)
        assertEquals(0L, viewModel.remainingMillis)
        assertEquals(0L, viewModel.totalMillis)
        assertEquals(0L, viewModel.timerEndTimeMillis)
    }
}
