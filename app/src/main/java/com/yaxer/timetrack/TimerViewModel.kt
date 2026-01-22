package com.yaxer.timetrack

import androidx.lifecycle.ViewModel

enum class TimerState { IDLE, RUNNING, PAUSED }

class TimerViewModel : ViewModel() {
    var timerState: TimerState = TimerState.IDLE
    var remainingMillis: Long = 0
    var totalMillis: Long = 0
    var timerEndTimeMillis: Long = 0  // SystemClock.elapsedRealtime() when timer will end
}
