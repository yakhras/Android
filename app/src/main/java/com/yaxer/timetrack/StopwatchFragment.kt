package com.yaxer.timetrack

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StopwatchFragment : Fragment() {

    private lateinit var stopwatchDisplay: TextView
    private lateinit var millisecondsDisplay: TextView
    private lateinit var startStopButton: Button
    private lateinit var lapResetButton: Button
    private lateinit var lapTimesRecyclerView: RecyclerView

    private val viewModel: StopwatchViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private lateinit var lapTimesAdapter: LapTimesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        stopwatchDisplay = view.findViewById(R.id.stopwatchDisplay)
        millisecondsDisplay = view.findViewById(R.id.millisecondsDisplay)
        startStopButton = view.findViewById(R.id.startStopButton)
        lapResetButton = view.findViewById(R.id.lapResetButton)
        lapTimesRecyclerView = view.findViewById(R.id.lapTimesRecyclerView)

        // Setup lap times RecyclerView
        lapTimesAdapter = LapTimesAdapter(viewModel.lapTimes)
        lapTimesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        lapTimesRecyclerView.adapter = lapTimesAdapter

        // Set button clicks
        startStopButton.setOnClickListener { toggleStartStop() }
        lapResetButton.setOnClickListener { lapOrReset() }

        // Restore display from ViewModel state
        if (viewModel.state != StopwatchState.IDLE) {
            val elapsed = if (viewModel.state == StopwatchState.RUNNING) {
                viewModel.elapsedTimeMillis + (SystemClock.elapsedRealtime() - viewModel.startTimeMillis)
            } else {
                viewModel.elapsedTimeMillis
            }
            updateDisplay(elapsed)
        }

        // Restart timer if was running
        if (viewModel.state == StopwatchState.RUNNING) {
            startTimer()
        }

        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun toggleStartStop() {
        when (viewModel.state) {
            StopwatchState.IDLE -> start()
            StopwatchState.RUNNING -> pause()
            StopwatchState.PAUSED -> resume()
        }
    }

    private fun lapOrReset() {
        when (viewModel.state) {
            StopwatchState.IDLE -> { /* Nothing to reset */ }
            StopwatchState.RUNNING -> recordLap()
            StopwatchState.PAUSED -> reset()
        }
    }

    private fun start() {
        viewModel.state = StopwatchState.RUNNING
        viewModel.startTimeMillis = SystemClock.elapsedRealtime()
        viewModel.lastLapTimeMillis = 0
        startTimer()
        updateUI()
    }

    private fun pause() {
        viewModel.state = StopwatchState.PAUSED
        viewModel.elapsedTimeMillis += SystemClock.elapsedRealtime() - viewModel.startTimeMillis
        timerRunnable?.let { handler.removeCallbacks(it) }
        updateUI()
    }

    private fun resume() {
        viewModel.state = StopwatchState.RUNNING
        viewModel.startTimeMillis = SystemClock.elapsedRealtime()
        startTimer()
        updateUI()
    }

    private fun reset() {
        viewModel.state = StopwatchState.IDLE
        viewModel.elapsedTimeMillis = 0
        viewModel.lastLapTimeMillis = 0
        viewModel.lapTimes.clear()
        lapTimesAdapter.notifyDataSetChanged()
        updateDisplay(0)
        updateUI()
    }

    private fun recordLap() {
        val currentTotal = viewModel.elapsedTimeMillis + (SystemClock.elapsedRealtime() - viewModel.startTimeMillis)
        val lapDuration = currentTotal - viewModel.lastLapTimeMillis
        viewModel.lastLapTimeMillis = currentTotal

        val lapTime = LapTime(
            lapNumber = viewModel.lapTimes.size + 1,
            lapDuration = lapDuration,
            totalTime = currentTotal
        )
        viewModel.lapTimes.add(0, lapTime)
        lapTimesAdapter.notifyItemInserted(0)
        lapTimesRecyclerView.scrollToPosition(0)
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                val currentElapsed = viewModel.elapsedTimeMillis + (SystemClock.elapsedRealtime() - viewModel.startTimeMillis)
                updateDisplay(currentElapsed)
                handler.postDelayed(this, 10)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun updateDisplay(millis: Long) {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        val centiseconds = (millis % 1000) / 10

        stopwatchDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        millisecondsDisplay.text = String.format(".%02d", centiseconds)
    }

    private fun updateUI() {
        when (viewModel.state) {
            StopwatchState.IDLE -> {
                startStopButton.text = "Start"
                startStopButton.setBackgroundColor(0xFF43A047.toInt()) // Green
                lapResetButton.text = "Lap"
                lapResetButton.isEnabled = false
                lapResetButton.alpha = 0.5f
            }
            StopwatchState.RUNNING -> {
                startStopButton.text = "Stop"
                startStopButton.setBackgroundColor(0xFFE53935.toInt()) // Red
                lapResetButton.text = "Lap"
                lapResetButton.isEnabled = true
                lapResetButton.alpha = 1.0f
            }
            StopwatchState.PAUSED -> {
                startStopButton.text = "Resume"
                startStopButton.setBackgroundColor(0xFF43A047.toInt()) // Green
                lapResetButton.text = "Reset"
                lapResetButton.isEnabled = true
                lapResetButton.alpha = 1.0f
            }
        }
    }
}
