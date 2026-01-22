package com.yaxer.timetrack

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: Button
    private lateinit var timerCard: CardView
    private lateinit var pickerContainer: LinearLayout
    private lateinit var hoursPicker: NumberPicker
    private lateinit var minutesPicker: NumberPicker
    private lateinit var secondsPicker: NumberPicker
    private lateinit var timerDisplay: TextView
    private lateinit var startButton: Button
    private lateinit var runningButtons: LinearLayout
    private lateinit var pauseResumeButton: Button
    private lateinit var stopButton: Button
    private lateinit var navButtons: LinearLayout
    private lateinit var projectsNav: TextView
    private lateinit var entriesNav: TextView
    private lateinit var timerNav: TextView
    private lateinit var stopwatchNav: TextView

    private enum class TimerState { IDLE, RUNNING, PAUSED }
    private var timerState = TimerState.IDLE

    private var countDownTimer: CountDownTimer? = null
    private var remainingMillis: Long = 0
    private var totalMillis: Long = 0

    private var projects: List<Map<String, Any>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views
        progressBar = findViewById(R.id.progressBar)
        retryButton = findViewById(R.id.retryButton)
        timerCard = findViewById(R.id.timerCard)
        pickerContainer = findViewById(R.id.pickerContainer)
        hoursPicker = findViewById(R.id.hoursPicker)
        minutesPicker = findViewById(R.id.minutesPicker)
        secondsPicker = findViewById(R.id.secondsPicker)
        timerDisplay = findViewById(R.id.timerDisplay)
        startButton = findViewById(R.id.startButton)
        runningButtons = findViewById(R.id.runningButtons)
        pauseResumeButton = findViewById(R.id.pauseResumeButton)
        stopButton = findViewById(R.id.stopButton)
        navButtons = findViewById(R.id.navButtons)
        projectsNav = findViewById(R.id.projectsButton)
        entriesNav = findViewById(R.id.entriesButton)
        timerNav = findViewById(R.id.timerButton)
        stopwatchNav = findViewById(R.id.stopwatchNav)

        // Setup NumberPickers
        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        hoursPicker.wrapSelectorWheel = true

        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        minutesPicker.wrapSelectorWheel = true

        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59
        secondsPicker.wrapSelectorWheel = true

        // Set button clicks
        retryButton.setOnClickListener { connect() }
        startButton.setOnClickListener { startTimer() }
        pauseResumeButton.setOnClickListener { togglePause() }
        stopButton.setOnClickListener { stopTimer() }
        projectsNav.setOnClickListener { startActivity(Intent(this, ProjectsActivity::class.java)) }
        entriesNav.setOnClickListener { startActivity(Intent(this, EntriesActivity::class.java)) }
        timerNav.setOnClickListener { /* Already on timer screen */ }
        stopwatchNav.setOnClickListener { startActivity(Intent(this, StopwatchActivity::class.java)) }

        // Auto-connect on startup
        connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun connect() {
        progressBar.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        timerCard.visibility = View.GONE
        navButtons.visibility = View.GONE
        startButton.visibility = View.GONE

        lifecycleScope.launch {
            val userId = OdooApiClient.authenticate()

            if (userId != null) {
                projects = OdooApiClient.fetchProjects()
                onConnected()
            } else {
                onConnectionFailed()
            }
        }
    }

    private fun onConnected() {
        progressBar.visibility = View.GONE
        retryButton.visibility = View.GONE
        timerCard.visibility = View.VISIBLE
        navButtons.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE
    }

    private fun onConnectionFailed() {
        progressBar.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        timerCard.visibility = View.GONE
        navButtons.visibility = View.GONE
        startButton.visibility = View.GONE
    }

    private fun startTimer() {
        val hours = hoursPicker.value
        val minutes = minutesPicker.value
        val seconds = secondsPicker.value

        totalMillis = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

        if (totalMillis == 0L) {
            Toast.makeText(this, "Please set a time", Toast.LENGTH_SHORT).show()
            return
        }

        remainingMillis = totalMillis
        timerState = TimerState.RUNNING

        updateTimerUI()
        startCountDown()
    }

    private fun togglePause() {
        when (timerState) {
            TimerState.RUNNING -> pauseTimer()
            TimerState.PAUSED -> resumeTimer()
            else -> {}
        }
    }

    private fun pauseTimer() {
        timerState = TimerState.PAUSED
        countDownTimer?.cancel()
        updateTimerUI()
    }

    private fun resumeTimer() {
        timerState = TimerState.RUNNING
        updateTimerUI()
        startCountDown()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        resetTimer()
    }

    private fun resetTimer() {
        timerState = TimerState.IDLE
        remainingMillis = 0
        totalMillis = 0
        updateTimerUI()
    }

    private fun updateTimerUI() {
        when (timerState) {
            TimerState.IDLE -> {
                pickerContainer.visibility = View.VISIBLE
                timerDisplay.visibility = View.GONE
                startButton.visibility = View.VISIBLE
                runningButtons.visibility = View.GONE
            }
            TimerState.RUNNING -> {
                pickerContainer.visibility = View.GONE
                timerDisplay.visibility = View.VISIBLE
                startButton.visibility = View.GONE
                runningButtons.visibility = View.VISIBLE
                pauseResumeButton.text = "Pause"
                pauseResumeButton.setBackgroundColor(0xFFFFA000.toInt()) // Orange
                stopButton.setBackgroundColor(0xFFE53935.toInt()) // Red
            }
            TimerState.PAUSED -> {
                pickerContainer.visibility = View.GONE
                timerDisplay.visibility = View.VISIBLE
                startButton.visibility = View.GONE
                runningButtons.visibility = View.VISIBLE
                pauseResumeButton.text = "Resume"
                pauseResumeButton.setBackgroundColor(0xFF43A047.toInt()) // Green
                stopButton.setBackgroundColor(0xFFE53935.toInt()) // Red
            }
        }
    }

    private fun startCountDown() {
        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                remainingMillis = 0
                updateTimerDisplay()
                onTimerComplete()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        timerDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun onTimerComplete() {
        Toast.makeText(this, "Timer complete!", Toast.LENGTH_LONG).show()
        resetTimer()
    }
}
