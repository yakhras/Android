package com.yaxer.timetrack

import androidx.lifecycle.ViewModel

enum class StopwatchState { IDLE, RUNNING, PAUSED }

class StopwatchViewModel : ViewModel() {
    var state: StopwatchState = StopwatchState.IDLE
    var startTimeMillis: Long = 0
    var elapsedTimeMillis: Long = 0
    var lastLapTimeMillis: Long = 0
    val lapTimes: MutableList<LapTime> = mutableListOf()
}
