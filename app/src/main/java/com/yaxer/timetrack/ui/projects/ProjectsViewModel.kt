package com.yaxer.timetrack.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaxer.timetrack.data.local.ProjectEntity
import com.yaxer.timetrack.data.repository.TimeTrackRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Projects screen.
 * Provides reactive state for UI and handles user actions.
 */
class ProjectsViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    /**
     * Observable list of projects from local cache.
     * Automatically updates when database changes.
     */
    val projects: StateFlow<List<ProjectEntity>> = repository.getProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    /**
     * Loading state for showing progress indicators.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    /**
     * Error messages to show to the user.
     * Uses SharedFlow to avoid re-showing old errors on configuration change.
     */
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _isOnline = MutableStateFlow(repository.isOnline())
    /**
     * Online status for UI indicators.
     */
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Observe online status changes
        viewModelScope.launch {
            repository.observeOnlineStatus().collect { online ->
                _isOnline.value = online
            }
        }
    }

    /**
     * Refresh projects from the server.
     * Updates the local cache which triggers UI update via Flow.
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshProjects()
            } catch (e: Exception) {
                _error.emit("Failed to refresh projects: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new project.
     * @param name Project name
     */
    fun createProject(name: String) {
        if (name.isBlank()) {
            viewModelScope.launch {
                _error.emit("Project name cannot be empty")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.createProject(name)
                if (id == null) {
                    _error.emit("Failed to create project. Are you online?")
                }
            } catch (e: Exception) {
                _error.emit("Failed to create project: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
