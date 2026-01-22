package com.yaxer.timetrack

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class EntriesActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddEntry: FloatingActionButton
    private lateinit var projectsNav: TextView
    private lateinit var entriesNav: TextView
    private lateinit var stopwatchNav: TextView
    private lateinit var timerNav: TextView

    private var projects: List<Map<String, Any>> = emptyList()
    private var adapter: EntriesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entries)

        statusText = findViewById(R.id.statusText)
        recyclerView = findViewById(R.id.entriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        fabAddEntry = findViewById(R.id.fabAddEntry)

        // Navigation
        projectsNav = findViewById(R.id.projectsNav)
        entriesNav = findViewById(R.id.entriesNav)
        stopwatchNav = findViewById(R.id.stopwatchNav)
        timerNav = findViewById(R.id.timerNav)

        projectsNav.setOnClickListener { startActivity(Intent(this, ProjectsActivity::class.java)) }
        entriesNav.setOnClickListener { /* Already on entries screen */ }
        stopwatchNav.setOnClickListener { startActivity(Intent(this, StopwatchActivity::class.java)) }
        timerNav.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        fabAddEntry.setOnClickListener {
            showCreateEntryDialog()
        }

        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.cleanup()
    }

    private fun loadData() {
        statusText.text = "Loading..."

        lifecycleScope.launch {
            projects = OdooApiClient.fetchProjects()
            loadEntries()
        }
    }

    private fun loadEntries() {
        lifecycleScope.launch {
            val entries = OdooApiClient.fetchTimeEntries()

            if (entries.isEmpty()) {
                statusText.text = "No time entries found"
                adapter?.cleanup()
                adapter = null
                recyclerView.adapter = null
            } else {
                statusText.text = "${entries.size} entries"
                adapter?.cleanup()
                adapter = EntriesAdapter(
                    entries = entries,
                    onStartTimer = { entryId -> handleStartTimer(entryId) },
                    onPauseTimer = { entryId -> handlePauseTimer(entryId) },
                    onResumeTimer = { entryId -> handleResumeTimer(entryId) },
                    onStopTimer = { entryId, pausedSeconds -> handleStopTimer(entryId, pausedSeconds) }
                )
                recyclerView.adapter = adapter
            }
        }
    }

    private fun handleStartTimer(entryId: Int) {
        lifecycleScope.launch {
            statusText.text = "Starting timer..."
            val success = OdooApiClient.startTimer(entryId)

            if (success) {
                Toast.makeText(this@EntriesActivity, "Timer started!", Toast.LENGTH_SHORT).show()
                loadEntries()
            } else {
                Toast.makeText(this@EntriesActivity, "Failed to start timer", Toast.LENGTH_SHORT).show()
                statusText.text = "Error"
            }
        }
    }

    private fun handlePauseTimer(entryId: Int) {
        // Pause is handled locally in the adapter
        // Just update the status text
        statusText.text = "Timer paused"
    }

    private fun handleResumeTimer(entryId: Int) {
        // Resume is handled locally in the adapter
        statusText.text = "Timer resumed"
    }

    private fun handleStopTimer(entryId: Int, pausedSeconds: Long) {
        lifecycleScope.launch {
            statusText.text = "Stopping timer..."
            val success = OdooApiClient.stopTimerWithPause(entryId, pausedSeconds)

            if (success) {
                Toast.makeText(this@EntriesActivity, "Timer stopped!", Toast.LENGTH_SHORT).show()
                loadEntries()
            } else {
                Toast.makeText(this@EntriesActivity, "Failed to stop timer", Toast.LENGTH_SHORT).show()
                statusText.text = "Error"
            }
        }
    }

    private fun showCreateEntryDialog() {
        if (projects.isEmpty()) {
            Toast.makeText(this, "No projects available. Create a project first.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_entry, null)
        val projectSpinner = dialogView.findViewById<Spinner>(R.id.projectSpinner)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)

        val projectNames = projects.map { it["name"]?.toString() ?: "Unknown" }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, projectNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setTitle("New Time Entry")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val selectedIndex = projectSpinner.selectedItemPosition
                val projectId = (projects[selectedIndex]["id"] as Number).toInt()
                val description = descriptionEditText.text.toString().trim()

                createEntry(projectId, description.ifEmpty { null })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createEntry(projectId: Int, description: String?) {
        statusText.text = "Creating entry..."

        lifecycleScope.launch {
            val entryId = OdooApiClient.createEntry(projectId, description)

            if (entryId != null) {
                Toast.makeText(this@EntriesActivity, "Entry created!", Toast.LENGTH_SHORT).show()
                loadEntries()
            } else {
                Toast.makeText(this@EntriesActivity, "Failed to create entry", Toast.LENGTH_SHORT).show()
                statusText.text = "Error creating entry"
            }
        }
    }
}
