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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yaxer.timetrack.data.local.ProjectEntity
import com.yaxer.timetrack.ui.ViewModelFactory
import com.yaxer.timetrack.ui.entries.EntriesViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EntriesFragment : Fragment() {

    private val viewModel: EntriesViewModel by viewModels {
        ViewModelFactory((requireActivity().application as TimeTrackApplication).repository)
    }

    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddEntry: FloatingActionButton

    private var projectEntities: List<ProjectEntity> = emptyList()
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

        // Observe entries and loading state combined
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.entries, viewModel.isLoading) { entries, isLoading ->
                Pair(entries, isLoading)
            }.collect { (entries, isLoading) ->
                statusText.text = when {
                    isLoading -> "Loading..."
                    entries.isEmpty() -> "No time entries found"
                    else -> "${entries.size} entries"
                }
                if (!isLoading) {
                    if (entries.isEmpty()) {
                        adapter?.cleanup()
                        adapter = null
                        recyclerView.adapter = null
                    } else {
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
        }

        // Observe projects for create dialog
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.projects.collect { projects ->
                projectEntities = projects
            }
        }

        // Observe errors
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        // Initial refresh
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter?.cleanup()
    }

    private fun handleStartTimer(entryId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            statusText.text = "Starting timer..."
            val success = viewModel.startTimer(entryId)

            if (success) {
                Toast.makeText(requireContext(), "Timer started!", Toast.LENGTH_SHORT).show()
                viewModel.refreshTodayOnly()
            } else {
                Toast.makeText(requireContext(), "Failed to start timer", Toast.LENGTH_SHORT).show()
                statusText.text = "Error"
            }
        }
    }

    private fun handlePauseTimer(entryId: Int) {
        // Pause is handled locally in the adapter
        statusText.text = "Timer paused"
    }

    private fun handleResumeTimer(entryId: Int) {
        // Resume is handled locally in the adapter
        statusText.text = "Timer resumed"
    }

    private fun handleStopTimer(entryId: Int, pausedSeconds: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            statusText.text = "Stopping timer..."
            val success = viewModel.stopTimer(entryId, pausedSeconds)

            if (success) {
                Toast.makeText(requireContext(), "Timer stopped!", Toast.LENGTH_SHORT).show()
                viewModel.refreshTodayOnly()
            } else {
                Toast.makeText(requireContext(), "Failed to stop timer", Toast.LENGTH_SHORT).show()
                statusText.text = "Error"
            }
        }
    }

    private fun showCreateEntryDialog() {
        if (projectEntities.isEmpty()) {
            Toast.makeText(requireContext(), "No projects available. Create a project first.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_entry, null)
        val projectSpinner = dialogView.findViewById<Spinner>(R.id.projectSpinner)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)

        val projectNames = projectEntities.map { it.name }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, projectNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("New Time Entry")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val selectedIndex = projectSpinner.selectedItemPosition
                val projectId = projectEntities[selectedIndex].id
                val description = descriptionEditText.text.toString().trim()

                viewModel.createEntry(projectId, description.ifEmpty { null })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
