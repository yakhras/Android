package com.yaxer.timetrack

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ProjectsActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddProject: FloatingActionButton
    private lateinit var projectsNav: TextView
    private lateinit var entriesNav: TextView
    private lateinit var stopwatchNav: TextView
    private lateinit var timerNav: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)

        statusText = findViewById(R.id.statusText)
        recyclerView = findViewById(R.id.projectsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        fabAddProject = findViewById(R.id.fabAddProject)

        // Navigation
        projectsNav = findViewById(R.id.projectsNav)
        entriesNav = findViewById(R.id.entriesNav)
        stopwatchNav = findViewById(R.id.stopwatchNav)
        timerNav = findViewById(R.id.timerNav)

        projectsNav.setOnClickListener { /* Already on projects screen */ }
        entriesNav.setOnClickListener { startActivity(Intent(this, EntriesActivity::class.java)) }
        stopwatchNav.setOnClickListener { startActivity(Intent(this, StopwatchActivity::class.java)) }
        timerNav.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        fabAddProject.setOnClickListener {
            showCreateProjectDialog()
        }

        loadProjects()
    }

    private fun loadProjects() {
        statusText.text = "Loading projects..."

        lifecycleScope.launch {
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
        val editText = EditText(this).apply {
            hint = "Project name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("New Project")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createProject(name)
                } else {
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createProject(name: String) {
        statusText.text = "Creating project..."

        lifecycleScope.launch {
            val projectId = OdooApiClient.createProject(name)

            if (projectId != null) {
                Toast.makeText(this@ProjectsActivity, "Project created!", Toast.LENGTH_SHORT).show()
                loadProjects()
            } else {
                Toast.makeText(this@ProjectsActivity, "Failed to create project", Toast.LENGTH_SHORT).show()
                statusText.text = "Error creating project"
            }
        }
    }
}
