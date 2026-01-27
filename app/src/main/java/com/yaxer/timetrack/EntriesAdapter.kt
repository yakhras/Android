package com.yaxer.timetrack

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yaxer.timetrack.data.local.TimeEntryEntity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EntriesAdapter(
    private val entries: List<TimeEntryEntity>,
    private val onStartTimer: (entryId: Int) -> Unit,
    private val onPauseTimer: (entryId: Int) -> Unit,
    private val onResumeTimer: (entryId: Int) -> Unit,
    private val onStopTimer: (entryId: Int, pausedSeconds: Long) -> Unit
) : RecyclerView.Adapter<EntriesAdapter.ViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())
    private val runningTimers = mutableMapOf<Int, Runnable>()

    // Track paused state locally (entryId -> pauseStartTime)
    private val pausedEntries = mutableMapOf<Int, LocalDateTime>()
    // Track total paused time per entry
    private val totalPausedSeconds = mutableMapOf<Int, Long>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val entryProject: TextView = view.findViewById(R.id.entryProject)
        val entryStatus: TextView = view.findViewById(R.id.entryStatus)
        val entryDate: TextView = view.findViewById(R.id.entryDate)
        val entryTime: TextView = view.findViewById(R.id.entryTime)
        val entryDuration: TextView = view.findViewById(R.id.entryDuration)
        val entryDescription: TextView = view.findViewById(R.id.entryDescription)
        val startButton: Button = view.findViewById(R.id.startButton)
        val runningButtonsContainer: LinearLayout = view.findViewById(R.id.runningButtonsContainer)
        val pauseResumeButton: Button = view.findViewById(R.id.pauseResumeButton)
        val stopButton: Button = view.findViewById(R.id.stopButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val entryId = entry.id

        // Project name - direct access from entity
        holder.entryProject.text = entry.projectName

        // Check states
        val isRunning = entry.isRunning
        val isPaused = pausedEntries.containsKey(entryId)

        // Status badge
        when {
            isPaused -> {
                holder.entryStatus.visibility = View.VISIBLE
                holder.entryStatus.text = "PAUSED"
                holder.entryStatus.setBackgroundColor(0xFFFFA000.toInt()) // Orange
            }
            isRunning -> {
                holder.entryStatus.visibility = View.VISIBLE
                holder.entryStatus.text = "RUNNING"
                holder.entryStatus.setBackgroundColor(0xFF43A047.toInt()) // Green
            }
            else -> {
                holder.entryStatus.visibility = View.GONE
            }
        }

        // Button visibility
        if (isRunning || isPaused) {
            holder.startButton.visibility = View.GONE
            holder.runningButtonsContainer.visibility = View.VISIBLE

            if (isPaused) {
                holder.pauseResumeButton.text = "Resume"
                holder.pauseResumeButton.setBackgroundColor(0xFF43A047.toInt()) // Green
            } else {
                holder.pauseResumeButton.text = "Pause"
                holder.pauseResumeButton.setBackgroundColor(0xFFFFA000.toInt()) // Orange
            }
            holder.stopButton.setBackgroundColor(0xFFE53935.toInt()) // Red
        } else {
            holder.startButton.visibility = View.VISIBLE
            holder.runningButtonsContainer.visibility = View.GONE
            holder.startButton.setBackgroundColor(0xFF43A047.toInt()) // Green
        }

        // Button clicks
        holder.startButton.setOnClickListener {
            onStartTimer(entryId)
        }

        holder.pauseResumeButton.setOnClickListener {
            if (isPaused) {
                // Resume
                pausedEntries[entryId]?.let { pauseStart ->
                    val pauseDuration = Duration.between(pauseStart, LocalDateTime.now()).seconds
                    totalPausedSeconds[entryId] = (totalPausedSeconds[entryId] ?: 0) + pauseDuration
                }
                pausedEntries.remove(entryId)
                onResumeTimer(entryId)
                notifyItemChanged(position)
                // Restart live timer
                val startTime = entry.startTime
                if (startTime != null) {
                    startLiveTimer(holder, entryId, startTime)
                }
            } else {
                // Pause
                pausedEntries[entryId] = LocalDateTime.now()
                stopLiveTimer(entryId)
                onPauseTimer(entryId)
                notifyItemChanged(position)
            }
        }

        holder.stopButton.setOnClickListener {
            // Calculate final paused time if currently paused
            if (isPaused) {
                pausedEntries[entryId]?.let { pauseStart ->
                    val pauseDuration = Duration.between(pauseStart, LocalDateTime.now()).seconds
                    totalPausedSeconds[entryId] = (totalPausedSeconds[entryId] ?: 0) + pauseDuration
                }
                pausedEntries.remove(entryId)
            }
            val paused = totalPausedSeconds[entryId] ?: 0
            totalPausedSeconds.remove(entryId)
            stopLiveTimer(entryId)
            onStopTimer(entryId, paused)
        }

        // Date
        holder.entryDate.text = "Date: ${entry.date}"

        // Time
        val startTimeStr = formatTime(entry.startTime)
        val endTimeStr = when {
            isPaused -> "Paused"
            isRunning -> "In progress"
            else -> formatTime(entry.endTime)
        }
        holder.entryTime.text = "Time: $startTimeStr - $endTimeStr"

        // Duration - live update for running entries
        if (isRunning && !isPaused && entry.startTime != null) {
            startLiveTimer(holder, entryId, entry.startTime)
        } else {
            stopLiveTimer(entryId)
            val duration = entry.unitAmount.toInt()
            if (isPaused && entry.startTime != null) {
                // Show current duration minus paused time
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                try {
                    val start = LocalDateTime.parse(entry.startTime, formatter)
                    val pauseStart = pausedEntries[entryId] ?: LocalDateTime.now()
                    val totalSeconds = Duration.between(start, pauseStart).seconds - (totalPausedSeconds[entryId] ?: 0)
                    val minutes = (totalSeconds / 60).toInt()
                    holder.entryDuration.text = formatDuration(minutes) + " (paused)"
                } catch (e: Exception) {
                    holder.entryDuration.text = formatDuration(duration)
                }
            } else {
                holder.entryDuration.text = formatDuration(duration)
            }
        }

        // Description
        if (entry.description.isNotEmpty()) {
            holder.entryDescription.visibility = View.VISIBLE
            holder.entryDescription.text = entry.description
        } else {
            holder.entryDescription.visibility = View.GONE
        }
    }

    private fun startLiveTimer(holder: ViewHolder, entryId: Int, startTimeStr: String) {
        stopLiveTimer(entryId)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val startTime = try {
            LocalDateTime.parse(startTimeStr, formatter)
        } catch (e: Exception) {
            return
        }

        val runnable = object : Runnable {
            override fun run() {
                val now = LocalDateTime.now()
                val totalSeconds = Duration.between(startTime, now).seconds - (totalPausedSeconds[entryId] ?: 0)
                val minutes = (totalSeconds / 60).toInt()
                holder.entryDuration.text = formatDuration(minutes) + " (live)"
                handler.postDelayed(this, 1000)
            }
        }

        runningTimers[entryId] = runnable
        handler.post(runnable)
    }

    private fun stopLiveTimer(entryId: Int) {
        runningTimers[entryId]?.let { handler.removeCallbacks(it) }
        runningTimers.remove(entryId)
    }

    private fun formatTime(time: String?): String {
        if (time == null) return "N/A"
        return if (time.contains(" ")) {
            time.split(" ").getOrNull(1)?.substring(0, 5) ?: time
        } else {
            time
        }
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            "Duration: ${hours}h ${mins}m"
        } else {
            "Duration: ${mins}m"
        }
    }

    override fun getItemCount() = entries.size

    fun cleanup() {
        runningTimers.values.forEach { handler.removeCallbacks(it) }
        runningTimers.clear()
        pausedEntries.clear()
        totalPausedSeconds.clear()
    }
}
