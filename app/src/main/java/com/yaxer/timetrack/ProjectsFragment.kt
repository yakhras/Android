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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {

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

        loadProjects()
    }

    private fun loadProjects() {
        statusText.text = "Loading projects..."

        viewLifecycleOwner.lifecycleScope.launch {
            val projects = OdooApiClient.fetchProjects()

            if (projects.isEmpty()) {
                statusText.text = "No projects found"
            } else {
                statusText.text = "${projects.size} projects"
                recyclerView.adapter = ProjectsAdapter(projects)
            }
        }
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
                    createProject(name)
                } else {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createProject(name: String) {
        statusText.text = "Creating project..."

        viewLifecycleOwner.lifecycleScope.launch {
            val projectId = OdooApiClient.createProject(name)

            if (projectId != null) {
                Toast.makeText(requireContext(), "Project created!", Toast.LENGTH_SHORT).show()
                loadProjects()
            } else {
                Toast.makeText(requireContext(), "Failed to create project", Toast.LENGTH_SHORT).show()
                statusText.text = "Error creating project"
            }
        }
    }
}
