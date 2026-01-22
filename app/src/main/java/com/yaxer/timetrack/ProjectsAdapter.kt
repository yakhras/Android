package com.yaxer.timetrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectsAdapter(
    private val projects: List<Map<String, Any>>
) : RecyclerView.Adapter<ProjectsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val projectName: TextView = view.findViewById(R.id.projectName)
        val projectId: TextView = view.findViewById(R.id.projectId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        val id = project["id"]
        val name = project["name"]?.toString() ?: "Unknown"

        holder.projectName.text = name
        holder.projectId.text = "ID: $id"
    }

    override fun getItemCount() = projects.size
}
