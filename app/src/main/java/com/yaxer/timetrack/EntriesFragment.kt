package com.yaxer.timetrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class EntriesFragment : Fragment() {

    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddEntry: FloatingActionButton

    private var projects: List<Map<String, Any>> = emptyList()
    private var adapter: EntriesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText = view.findViewById(R.id.statusText)
        recyclerView = view.findViewById(R.id.entriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        fabAddEntry = view.findViewById(R.id.fabAddEntry)

        fabAddEntry.setOnClickListener {
            showCreateEntryDialog()
        }

        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter?.cleanup()
    }

    private fun loadData() {
        statusText.text = "Loading..."

        viewLifecycleOwner.lifecycleScope.launch {
            projects = OdooApiClient.fetchProjects()
            loadEntries()
        }
    }

    private fun loadEntries() {
        viewLifecycleOwner.lifecycleScope.launch {
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
        viewLifecycleOwner.lifecycleScope.launch {
            statusText.text = "Starting timer..."
            val success = OdooApiClient.startTimer(entryId)

            if (success) {
                Toast.makeText(requireContext(), "Timer started!", Toast.LENGTH_SHORT).show()
                loadEntries()
            } else {
                Toast.makeText(requireContext(), "Failed to start timer", Toast.LENGTH_SHORT).show()
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
        viewLifecycleOwner.lifecycleScope.launch {
            statusText.text = "Stopping timer..."
            val success = OdooApiClient.stopTimerWithPause(entryId, pausedSeconds)

            if (success) {
                Toast.makeText(requireContext(), "Timer stopped!", Toast.LENGTH_SHORT).show()
                loadEntries()
            } else {
                Toast.makeText(requireContext(), "Failed to stop timer", Toast.LENGTH_SHORT).show()
                statusText.text = "Error"
            }
        }
    }

    private fun showCreateEntryDialog() {
        if (projects.isEmpty()) {
            Toast.makeText(requireContext(), "No projects available. Create a project first.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_entry, null)
        val projectSpinner = dialogView.findViewById<Spinner>(R.id.projectSpinner)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)

        val projectNames = projects.map { it["name"]?.toString() ?: "Unknown" }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, projectNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
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

        viewLifecycleOwner.lifecycleScope.launch {
            val entryId = OdooApiClient.createEntry(projectId, description)

            if (entryId != null) {
                Toast.makeText(requireContext(), "Entry created!", Toast.LENGTH_SHORT).show()
                loadEntries()
            } else {
                Toast.makeText(requireContext(), "Failed to create entry", Toast.LENGTH_SHORT).show()
                statusText.text = "Error creating entry"
            }
        }
    }
}
