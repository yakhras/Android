package com.yaxer.timetrack

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StopwatchActivity : AppCompatActivity() {

    private lateinit var stopwatchDisplay: TextView
    private lateinit var millisecondsDisplay: TextView
    private lateinit var startStopButton: Button
    private lateinit var lapResetButton: Button
    private lateinit var lapTimesRecyclerView: RecyclerView

    private lateinit var projectsNav: TextView
    private lateinit var entriesNav: TextView
    private lateinit var timerNav: TextView
    private lateinit var stopwatchNav: TextView

    private enum class StopwatchState { IDLE, RUNNING, PAUSED }
    private var state = StopwatchState.IDLE

    private var startTimeMillis: Long = 0
    private var elapsedTimeMillis: Long = 0
    private var lastLapTimeMillis: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private val lapTimes = mutableListOf<LapTime>()
    private lateinit var lapTimesAdapter: LapTimesAdapter

    data class LapTime(val lapNumber: Int, val lapDuration: Long, val totalTime: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stopwatch)

        // Find views
        stopwatchDisplay = findViewById(R.id.stopwatchDisplay)
        millisecondsDisplay = findViewById(R.id.millisecondsDisplay)
        startStopButton = findViewById(R.id.startStopButton)
        lapResetButton = findViewById(R.id.lapResetButton)
        lapTimesRecyclerView = findViewById(R.id.lapTimesRecyclerView)

        projectsNav = findViewById(R.id.projectsNav)
        entriesNav = findViewById(R.id.entriesNav)
        timerNav = findViewById(R.id.timerNav)
        stopwatchNav = findViewById(R.id.stopwatchNav)

        // Setup lap times RecyclerView
        lapTimesAdapter = LapTimesAdapter(lapTimes)
        lapTimesRecyclerView.layoutManager = LinearLayoutManager(this)
        lapTimesRecyclerView.adapter = lapTimesAdapter

        // Set button clicks
        startStopButton.setOnClickListener { toggleStartStop() }
        lapResetButton.setOnClickListener { lapOrReset() }

        // Navigation
        projectsNav.setOnClickListener { startActivity(Intent(this, ProjectsActivity::class.java)) }
        entriesNav.setOnClickListener { startActivity(Intent(this, EntriesActivity::class.java)) }
        timerNav.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        stopwatchNav.setOnClickListener { /* Already on stopwatch screen */ }

        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun toggleStartStop() {
        when (state) {
            StopwatchState.IDLE -> start()
            StopwatchState.RUNNING -> pause()
            StopwatchState.PAUSED -> resume()
        }
    }

    private fun lapOrReset() {
        when (state) {
            StopwatchState.IDLE -> { /* Nothing to reset */ }
            StopwatchState.RUNNING -> recordLap()
            StopwatchState.PAUSED -> reset()
        }
    }

    private fun start() {
        state = StopwatchState.RUNNING
        startTimeMillis = SystemClock.elapsedRealtime()
        lastLapTimeMillis = 0
        startTimer()
        updateUI()
    }

    private fun pause() {
        state = StopwatchState.PAUSED
        elapsedTimeMillis += SystemClock.elapsedRealtime() - startTimeMillis
        timerRunnable?.let { handler.removeCallbacks(it) }
        updateUI()
    }

    private fun resume() {
        state = StopwatchState.RUNNING
        startTimeMillis = SystemClock.elapsedRealtime()
        startTimer()
        updateUI()
    }

    private fun reset() {
        state = StopwatchState.IDLE
        elapsedTimeMillis = 0
        lastLapTimeMillis = 0
        lapTimes.clear()
        lapTimesAdapter.notifyDataSetChanged()
        updateDisplay(0)
        updateUI()
    }

    private fun recordLap() {
        val currentTotal = elapsedTimeMillis + (SystemClock.elapsedRealtime() - startTimeMillis)
        val lapDuration = currentTotal - lastLapTimeMillis
        lastLapTimeMillis = currentTotal

        val lapTime = LapTime(
            lapNumber = lapTimes.size + 1,
            lapDuration = lapDuration,
            totalTime = currentTotal
        )
        lapTimes.add(0, lapTime)
        lapTimesAdapter.notifyItemInserted(0)
        lapTimesRecyclerView.scrollToPosition(0)
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                val currentElapsed = elapsedTimeMillis + (SystemClock.elapsedRealtime() - startTimeMillis)
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
        when (state) {
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

    // Lap Times Adapter
    private class LapTimesAdapter(private val lapTimes: List<LapTime>) :
        RecyclerView.Adapter<LapTimesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val lapNumber: TextView = view.findViewById(R.id.lapNumber)
            val lapDuration: TextView = view.findViewById(R.id.lapDuration)
            val totalTime: TextView = view.findViewById(R.id.totalTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lap_time, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val lap = lapTimes[position]
            holder.lapNumber.text = "Lap ${lap.lapNumber}"
            holder.lapDuration.text = formatTime(lap.lapDuration)
            holder.totalTime.text = formatTime(lap.totalTime)
        }

        override fun getItemCount() = lapTimes.size

        private fun formatTime(millis: Long): String {
            val minutes = (millis % 3600000) / 60000
            val seconds = (millis % 60000) / 1000
            val centiseconds = (millis % 1000) / 10
            return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
        }
    }
}
