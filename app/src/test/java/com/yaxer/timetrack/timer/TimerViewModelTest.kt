package com.yaxer.timetrack.timer

import com.yaxer.timetrack.TimerState
import com.yaxer.timetrack.TimerViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TimerViewModel state management.
 * Tests state transitions and value storage.
 */
class TimerViewModelTest {

    private lateinit var viewModel: TimerViewModel

    @Before
    fun setUp() {
        viewModel = TimerViewModel()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state is IDLE`() {
        assertEquals(TimerState.IDLE, viewModel.timerState)
    }

    @Test
    fun `initial remainingMillis is 0`() {
        assertEquals(0L, viewModel.remainingMillis)
    }

    @Test
    fun `initial totalMillis is 0`() {
        assertEquals(0L, viewModel.totalMillis)
    }

    @Test
    fun `initial timerEndTimeMillis is 0`() {
        assertEquals(0L, viewModel.timerEndTimeMillis)
    }

    // ========== State Transition Tests ==========

    @Test
    fun `state can transition from IDLE to RUNNING`() {
        viewModel.timerState = TimerState.RUNNING

        assertEquals(TimerState.RUNNING, viewModel.timerState)
    }

    @Test
    fun `state can transition from RUNNING to PAUSED`() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.timerState = TimerState.PAUSED

        assertEquals(TimerState.PAUSED, viewModel.timerState)
    }

    @Test
    fun `state can transition from PAUSED to RUNNING`() {
        viewModel.timerState = TimerState.PAUSED
        viewModel.timerState = TimerState.RUNNING

        assertEquals(TimerState.RUNNING, viewModel.timerState)
    }

    @Test
    fun `state can transition from RUNNING to IDLE`() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.timerState = TimerState.IDLE

        assertEquals(TimerState.IDLE, viewModel.timerState)
    }

    @Test
    fun `state can transition from PAUSED to IDLE`() {
        viewModel.timerState = TimerState.PAUSED
        viewModel.timerState = TimerState.IDLE

        assertEquals(TimerState.IDLE, viewModel.timerState)
    }

    @Test
    fun `full state cycle - IDLE to RUNNING to PAUSED to RUNNING to IDLE`() {
        assertEquals(TimerState.IDLE, viewModel.timerState)

        viewModel.timerState = TimerState.RUNNING
        assertEquals(TimerState.RUNNING, viewModel.timerState)

        viewModel.timerState = TimerState.PAUSED
        assertEquals(TimerState.PAUSED, viewModel.timerState)

        viewModel.timerState = TimerState.RUNNING
        assertEquals(TimerState.RUNNING, viewModel.timerState)

        viewModel.timerState = TimerState.IDLE
        assertEquals(TimerState.IDLE, viewModel.timerState)
    }

    // ========== Value Storage Tests ==========

    @Test
    fun `remainingMillis stores positive value`() {
        viewModel.remainingMillis = 300_000L

        assertEquals(300_000L, viewModel.remainingMillis)
    }

    @Test
    fun `totalMillis stores positive value`() {
        viewModel.totalMillis = 600_000L

        assertEquals(600_000L, viewModel.totalMillis)
    }

    @Test
    fun `timerEndTimeMillis stores positive value`() {
        viewModel.timerEndTimeMillis = 1234567890L

        assertEquals(1234567890L, viewModel.timerEndTimeMillis)
    }

    @Test
    fun `values can be updated independently`() {
        viewModel.remainingMillis = 100_000L
        viewModel.totalMillis = 200_000L
        viewModel.timerEndTimeMillis = 300_000L

        assertEquals(100_000L, viewModel.remainingMillis)
        assertEquals(200_000L, viewModel.totalMillis)
        assertEquals(300_000L, viewModel.timerEndTimeMillis)
    }

    // ========== Reset Simulation Tests ==========

    @Test
    fun `reset clears all values`() {
        // Set up running state
        viewModel.timerState = TimerState.RUNNING
        viewModel.remainingMillis = 150_000L
        viewModel.totalMillis = 300_000L
        viewModel.timerEndTimeMillis = 999_999L

        // Simulate reset
        viewModel.timerState = TimerState.IDLE
        viewModel.remainingMillis = 0L
        viewModel.totalMillis = 0L
        viewModel.timerEndTimeMillis = 0L

        assertEquals(TimerState.IDLE, viewModel.timerState)
        assertEquals(0L, viewModel.remainingMillis)
        assertEquals(0L, viewModel.totalMillis)
        assertEquals(0L, viewModel.timerEndTimeMillis)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `large millisecond values are handled correctly`() {
        val maxTimerMillis = 86_399_000L  // 23:59:59
        viewModel.totalMillis = maxTimerMillis
        viewModel.remainingMillis = maxTimerMillis

        assertEquals(maxTimerMillis, viewModel.totalMillis)
        assertEquals(maxTimerMillis, viewModel.remainingMillis)
    }
}
