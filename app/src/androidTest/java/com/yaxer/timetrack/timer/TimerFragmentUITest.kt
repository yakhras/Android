package com.yaxer.timetrack.timer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yaxer.timetrack.HostActivity
import com.yaxer.timetrack.R
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for TimerFragment.
 * Tests timer UI interactions and state changes.
 */
@RunWith(AndroidJUnit4::class)
class TimerFragmentUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(HostActivity::class.java)

    // ========== Initial State Tests ==========

    @Test
    fun timerCard_isVisible_onLaunch() {
        // Navigate to timer tab if needed (depends on app structure)
        // For now, assuming timer is the default or accessible
        onView(withId(R.id.timerCard))
            .check(matches(isDisplayed()))
    }

    @Test
    fun pickerContainer_isVisible_whenIdle() {
        onView(withId(R.id.pickerContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun startButton_isVisible_whenIdle() {
        onView(withId(R.id.startButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun runningButtons_notVisible_whenIdle() {
        onView(withId(R.id.runningButtons))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun timerDisplay_notVisible_whenIdle() {
        onView(withId(R.id.timerDisplay))
            .check(matches(not(isDisplayed())))
    }

    // ========== Number Picker Tests ==========

    @Test
    fun hoursPicker_isDisplayed() {
        onView(withId(R.id.hoursPicker))
            .check(matches(isDisplayed()))
    }

    @Test
    fun minutesPicker_isDisplayed() {
        onView(withId(R.id.minutesPicker))
            .check(matches(isDisplayed()))
    }

    @Test
    fun secondsPicker_isDisplayed() {
        onView(withId(R.id.secondsPicker))
            .check(matches(isDisplayed()))
    }

    // ========== Button Interaction Tests ==========

    @Test
    fun startButton_hasCorrectText() {
        onView(withId(R.id.startButton))
            .check(matches(withText("Start")))
    }

    @Test
    fun pauseResumeButton_exists() {
        // Check button exists (may not be visible initially)
        onView(withId(R.id.pauseResumeButton))
            .check(matches(isEnabled()))
    }

    @Test
    fun stopButton_exists() {
        // Check button exists (may not be visible initially)
        onView(withId(R.id.stopButton))
            .check(matches(isEnabled()))
    }

    // ========== Progress and Retry Tests ==========

    @Test
    fun progressBar_notVisible_afterInit() {
        // Progress bar should be hidden once UI is ready
        onView(withId(R.id.progressBar))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun retryButton_notVisible_onSuccess() {
        onView(withId(R.id.retryButton))
            .check(matches(not(isDisplayed())))
    }
}
