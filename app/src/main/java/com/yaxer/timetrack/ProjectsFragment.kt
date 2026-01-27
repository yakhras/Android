package com.yaxer.timetrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yaxer.timetrack.ui.ViewModelFactory
import com.yaxer.timetrack.ui.projects.ProjectsViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {

    private val viewModel: ProjectsViewModel by viewModels {
        ViewModelFactory((requireActivity().application as TimeTrackApplication).repository)
    }

    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddProject: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText = view.findViewById(R.id.statusText)
        recyclerView = view.findViewById(R.id.projectsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        fabAddProject = view.findViewById(R.id.fabAddProject)

        fabAddProject.setOnClickListener {
            showCreateProjectDialog()
        }

        // Observe projects and loading state combined
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.projects, viewModel.isLoading) { projects, isLoading ->
                Pair(projects, isLoading)
            }.collect { (projects, isLoading) ->
                statusText.text = when {
                    isLoading -> "Loading..."
                    projects.isEmpty() -> "No projects found"
                    else -> "${projects.size} projects"
                }
                if (!isLoading && projects.isNotEmpty()) {
                    recyclerView.adapter = ProjectsAdapter(projects)
                }
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

    private fun showCreateProjectDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Project name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("New Project")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createProject(name)
                } else {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
