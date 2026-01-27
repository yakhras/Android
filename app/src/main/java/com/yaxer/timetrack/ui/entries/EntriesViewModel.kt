package com.yaxer.timetrack.ui.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaxer.timetrack.data.local.ProjectEntity
import com.yaxer.timetrack.data.local.TimeEntryEntity
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
import java.time.LocalDateTime

/**
 * ViewModel for Time Entries screen.
 * Provides reactive state for UI and handles user actions.
 */
class EntriesViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    /**
     * Observable list of time entries from local cache.
     * Automatically updates when database changes.
     */
    val entries: StateFlow<List<TimeEntryEntity>> = repository.getTimeEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Observable list of projects for entry creation.
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
     * Refresh both projects and time entries from the server.
     * Updates the local cache which triggers UI update via Flow.
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Refresh projects first (needed for entry creation dropdown)
                repository.refreshProjects()
                // Then refresh entries
                repository.refreshTimeEntries()
            } catch (e: Exception) {
                _error.emit("Failed to refresh: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh only today's entries (lighter operation).
     */
    fun refreshTodayOnly() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshTimeEntries(todayOnly = true)
            } catch (e: Exception) {
                _error.emit("Failed to refresh: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new time entry (quick entry without time data).
     * @param projectId Project ID
     * @param description Optional description
     */
    fun createEntry(projectId: Int, description: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.createTimeEntry(projectId, description)
                if (id == null) {
                    _error.emit("Failed to create entry. Are you online?")
                }
            } catch (e: Exception) {
                _error.emit("Failed to create entry: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a time entry with full time data (for completed timer sessions).
     * @param projectId Project ID
     * @param startTime Start time of the session
     * @param endTime End time of the session
     * @param durationMinutes Total duration in minutes
     * @param description Optional description
     */
    fun createEntryWithTime(
        projectId: Int,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        durationMinutes: Int,
        description: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.createTimeEntryWithTime(
                    projectId, startTime, endTime, durationMinutes, description
                )
                if (id == null) {
                    _error.emit("Failed to save time entry. Are you online?")
                }
            } catch (e: Exception) {
                _error.emit("Failed to save time entry: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start timer on an entry.
     * Works offline by updating local state and queuing for sync.
     */
    suspend fun startTimer(entryId: Int): Boolean {
        return repository.startTimer(entryId)
    }

    /**
     * Stop timer on an entry.
     * Works offline by updating local state and queuing for sync.
     */
    suspend fun stopTimer(entryId: Int, pausedSeconds: Long): Boolean {
        return repository.stopTimer(entryId, pausedSeconds)
    }
}
