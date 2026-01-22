package com.yaxer.timetrack

import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class TimerFragment : Fragment() {

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

    private val viewModel: TimerViewModel by viewModels()

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        progressBar = view.findViewById(R.id.progressBar)
        retryButton = view.findViewById(R.id.retryButton)
        timerCard = view.findViewById(R.id.timerCard)
        pickerContainer = view.findViewById(R.id.pickerContainer)
        hoursPicker = view.findViewById(R.id.hoursPicker)
        minutesPicker = view.findViewById(R.id.minutesPicker)
        secondsPicker = view.findViewById(R.id.secondsPicker)
        timerDisplay = view.findViewById(R.id.timerDisplay)
        startButton = view.findViewById(R.id.startButton)
        runningButtons = view.findViewById(R.id.runningButtons)
        pauseResumeButton = view.findViewById(R.id.pauseResumeButton)
        stopButton = view.findViewById(R.id.stopButton)

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
        startButton.setOnClickListener { startTimer() }
        pauseResumeButton.setOnClickListener { togglePause() }
        stopButton.setOnClickListener { stopTimer() }

        // Show UI immediately (no API connection needed for timer)
        progressBar.visibility = View.GONE
        retryButton.visibility = View.GONE
        timerCard.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE

        // Restore state from ViewModel
        if (viewModel.timerState != TimerState.IDLE) {
            // Recalculate remaining time if timer was running
            if (viewModel.timerState == TimerState.RUNNING) {
                val now = SystemClock.elapsedRealtime()
                viewModel.remainingMillis = maxOf(0, viewModel.timerEndTimeMillis - now)
                if (viewModel.remainingMillis > 0) {
                    startCountDown()
                } else {
                    resetTimer()
                }
            }
            updateTimerDisplay()
        }
        updateTimerUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private fun startTimer() {
        val hours = hoursPicker.value
        val minutes = minutesPicker.value
        val seconds = secondsPicker.value

        viewModel.totalMillis = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

        if (viewModel.totalMillis == 0L) {
            Toast.makeText(requireContext(), "Please set a time", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.remainingMillis = viewModel.totalMillis
        viewModel.timerState = TimerState.RUNNING
        viewModel.timerEndTimeMillis = SystemClock.elapsedRealtime() + viewModel.remainingMillis

        updateTimerUI()
        startCountDown()
    }

    private fun togglePause() {
        when (viewModel.timerState) {
            TimerState.RUNNING -> pauseTimer()
            TimerState.PAUSED -> resumeTimer()
            else -> {}
        }
    }

    private fun pauseTimer() {
        viewModel.timerState = TimerState.PAUSED
        countDownTimer?.cancel()
        updateTimerUI()
    }

    private fun resumeTimer() {
        viewModel.timerState = TimerState.RUNNING
        viewModel.timerEndTimeMillis = SystemClock.elapsedRealtime() + viewModel.remainingMillis
        updateTimerUI()
        startCountDown()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        resetTimer()
    }

    private fun resetTimer() {
        viewModel.timerState = TimerState.IDLE
        viewModel.remainingMillis = 0
        viewModel.totalMillis = 0
        viewModel.timerEndTimeMillis = 0
        updateTimerUI()
    }

    private fun updateTimerUI() {
        when (viewModel.timerState) {
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
        countDownTimer = object : CountDownTimer(viewModel.remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                viewModel.remainingMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                viewModel.remainingMillis = 0
                updateTimerDisplay()
                onTimerComplete()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        val totalSeconds = viewModel.remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        timerDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun onTimerComplete() {
        Toast.makeText(requireContext(), "Timer complete!", Toast.LENGTH_LONG).show()
        resetTimer()
    }
}
