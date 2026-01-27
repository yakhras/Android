package com.yaxer.timetrack

import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.yaxer.timetrack.data.local.ProjectEntity
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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

    private var projects: List<ProjectEntity> = emptyList()
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

        // Fetch projects in background for when timer completes
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireActivity().application as TimeTrackApplication
            // Try to refresh from API if online (background)
            try { app.repository.refreshProjects() } catch (_: Exception) {}
            // Always load from local cache
            projects = app.repository.getProjectsSync()
        }

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
        val elapsedMinutes = ((viewModel.totalMillis - viewModel.remainingMillis) / 60_000).toInt()
        if (elapsedMinutes > 0) {
            showAddEntryDialog(elapsedMinutes)
        }
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
        val durationMinutes = (viewModel.totalMillis / 60_000).toInt()
        Toast.makeText(requireContext(), "Timer complete!", Toast.LENGTH_LONG).show()
        showAddEntryDialog(durationMinutes)
        resetTimer()
    }

    private fun showAddEntryDialog(durationMinutes: Int) {
        if (projects.isEmpty()) {
            showNoProjectsDialog(durationMinutes)
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_timer_entry, null)
        val durationText = dialogView.findViewById<TextView>(R.id.durationText)
        val projectSpinner = dialogView.findViewById<Spinner>(R.id.projectSpinner)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)

        // Format duration display
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        durationText.text = if (hours > 0) {
            String.format("Duration: %d hr %d min", hours, mins)
        } else {
            String.format("Duration: %d min", mins)
        }

        // Setup project spinner
        val projectNames = projects.map { it.name }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, projectNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add Time Entry")
            .setView(dialogView)
            .setPositiveButton("Add Entry") { _, _ ->
                val selectedIndex = projectSpinner.selectedItemPosition
                val projectId = projects[selectedIndex].id
                val description = descriptionEditText.text.toString().trim()

                createTimerEntry(projectId, durationMinutes, description.ifEmpty { null })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createTimerEntry(projectId: Int, durationMinutes: Int, description: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireActivity().application as TimeTrackApplication
            val endTime = LocalDateTime.now()
            val startTime = endTime.minusMinutes(durationMinutes.toLong())

            val entryId = app.repository.createTimeEntryWithTime(
                projectId = projectId,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                description = description
            )

            if (entryId != null) {
                val syncStatus = if (entryId < 0) " (will sync when online)" else ""
                Toast.makeText(requireContext(), "Time entry created!$syncStatus", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to create entry", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNoProjectsDialog(durationMinutes: Int) {
        val editText = EditText(requireContext()).apply {
            hint = "Project name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("No Projects Available")
            .setMessage("Create a project to save your time entry (${formatDuration(durationMinutes)}):")
            .setView(editText)
            .setPositiveButton("Create & Save") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createProjectAndEntry(name, durationMinutes)
                } else {
                    Toast.makeText(requireContext(), "Please enter a project name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Discard Time", null)
            .show()
    }

    private fun createProjectAndEntry(projectName: String, durationMinutes: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireActivity().application as TimeTrackApplication
            val projectId = app.repository.createProject(projectName)
            if (projectId != null) {
                val syncStatus = if (projectId < 0) " (will sync when online)" else ""
                Toast.makeText(requireContext(), "Project created!$syncStatus", Toast.LENGTH_SHORT).show()

                // Refresh local cache
                projects = app.repository.getProjectsSync()

                // Create the time entry
                createTimerEntry(projectId, durationMinutes, null)
            } else {
                Toast.makeText(requireContext(), "Failed to create project", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        return if (hours > 0) {
            String.format("%d hr %d min", hours, mins)
        } else {
            String.format("%d min", mins)
        }
    }
}
