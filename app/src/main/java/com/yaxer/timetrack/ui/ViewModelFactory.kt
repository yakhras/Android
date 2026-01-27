package com.yaxer.timetrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yaxer.timetrack.data.repository.TimeTrackRepository
import com.yaxer.timetrack.ui.entries.EntriesViewModel
import com.yaxer.timetrack.ui.projects.ProjectsViewModel

/**
 * ViewModelFactory for creating ViewModels with repository dependency.
 * Simple manual DI solution (no Hilt/Dagger required).
 *
 * Usage in Fragment:
 * ```
 * private val viewModel: ProjectsViewModel by viewModels {
 *     ViewModelFactory((requireActivity().application as TimeTrackApplication).repository)
 * }
 * ```
 */
class ViewModelFactory(
    private val repository: TimeTrackRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ProjectsViewModel::class.java) -> {
                ProjectsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(EntriesViewModel::class.java) -> {
                EntriesViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
