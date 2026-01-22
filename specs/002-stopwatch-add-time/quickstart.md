# Quickstart: Add Time to Entry from Stopwatch

**Feature**: Add Time to Entry from Stopwatch (with Laps)
**Status**: Ready for Development
**Complexity**: Medium (UI + business logic)
**Timeline**: ~2-3 weeks (P1: ~1 week, P2: ~1 week, testing: ~1 week)

## Setup

### Prerequisites

- Android Studio 2022.1+
- Kotlin 1.9+
- Project has: Room, Lifecycle, Coroutines (assumed)
- Target API 26+ (existing project setting)

### Architecture Overview

```
StopwatchViewModel (observes reset button)
        │
        ├─ calls: StopwatchAddTimeService
        │
        └─ AddStopwatchTimeFragment
            │
            ├─ observes: AddStopwatchTimeViewModel.state
            │
            ├─ Layout:
            │  ├─ RecyclerView (lap list with checkboxes) [P2]
            │  ├─ Entry selection dialog
            │  └─ Confirmation buttons
            │
            └─ Actions:
               ├─ toggleLapSelection()
               ├─ assignLapsToEntry()
               └─ confirmAndAddTimeToEntries()
```

---

## Phase 1: Simple Add (MVP)

### Goal
User can add total stopwatch time to an entry in one tap.

### Implementation

#### 1. Create AddStopwatchTimeViewModel

**File**: `app/src/main/java/com/yaxer/timetrack/ui/stopwatch/AddStopwatchTimeViewModel.kt`

```kotlin
class AddStopwatchTimeViewModel(
    private val timeEntryRepository: TimeEntryRepository,
    private val stopwatchRepository: StopwatchRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AddStopwatchTimeState?>(null)
    val state: StateFlow<AddStopwatchTimeState?> = _state.asStateFlow()

    fun initializeForStopwatch(stopwatch: Stopwatch) {
        viewModelScope.launch {
            try {
                val entries = timeEntryRepository.getAllEntries()
                _state.value = AddStopwatchTimeState(
                    stopwatch = stopwatch,
                    availableLaps = stopwatch.laps,
                    availableEntries = entries,
                    selectedLaps = emptySet(),
                    lapToEntryAssignments = emptyMap()
                )
            } catch (e: Exception) {
                _state.value = AddStopwatchTimeState(
                    stopwatch = stopwatch,
                    availableLaps = stopwatch.laps,
                    availableEntries = emptyList(),
                    error = e.message
                )
            }
        }
    }

    fun addTimeToEntry(entryId: String) {
        viewModelScope.launch {
            try {
                val currentState = _state.value ?: return@launch
                val stopwatch = currentState.stopwatch

                // Update entry
                val entry = timeEntryRepository.getEntry(entryId)
                val newDuration = entry.durationMs + stopwatch.elapsedTimeMs
                timeEntryRepository.updateEntryDuration(entryId, newDuration)

                // Reset stopwatch
                stopwatchRepository.resetStopwatch(stopwatch.id)

                // Success - caller closes UI
            } catch (e: Exception) {
                _state.value = _state.value?.copy(error = e.message)
            }
        }
    }
}
```

#### 2. Create AddStopwatchTimeFragment

**File**: `app/src/main/java/com/yaxer/timetrack/ui/stopwatch/AddStopwatchTimeFragment.kt`

```kotlin
class AddStopwatchTimeFragment : Fragment() {

    private val viewModel: AddStopwatchTimeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stopwatch: Stopwatch? = arguments?.getParcelable("stopwatch")
        stopwatch?.let { viewModel.initializeForStopwatch(it) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when {
                    state == null -> showLoading()
                    state.error != null -> showError(state.error)
                    state.availableLaps.isEmpty() -> showSimpleAddUI(state)
                    else -> showLapAssignmentUI(state)
                }
            }
        }
    }

    private fun showSimpleAddUI(state: AddStopwatchTimeState) {
        // Show: "Add {time} to entry?"
        // Buttons for each entry
        // "Add 5:30 to:"
        //   [Project A]
        //   [Project B]
        //   [Cancel]

        binding.entryButtonsContainer.removeAllViews()
        state.availableEntries.forEach { entry ->
            val button = MaterialButton(requireContext()).apply {
                text = entry.name
                setOnClickListener {
                    viewModel.addTimeToEntry(entry.id)
                    dismiss()
                }
            }
            binding.entryButtonsContainer.addView(button)
        }

        binding.timeDisplay.text = formatTime(state.stopwatch.elapsedTimeMs)
    }

    private fun showLapAssignmentUI(state: StopwatchAddTimeState) {
        // P2: Show lap list with checkboxes
        // Delegate to Phase 2 implementation
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showError(error: String) {
        binding.errorMessage.text = error
        binding.errorMessage.visibility = View.VISIBLE
    }

    private fun dismiss() {
        parentFragmentManager.popBackStack()
    }

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}
```

#### 3. Trigger from StopwatchViewModel

**File**: Modify existing `StopwatchViewModel.kt`

```kotlin
class StopwatchViewModel(...) : ViewModel() {
    // ... existing code

    fun resetStopwatch() {
        viewModelScope.launch {
            val stopwatch = stopwatchRepository.getCurrentStopwatch()

            if (stopwatch.elapsedTimeMs == 0L) {
                // Nothing to add, just reset
                stopwatchRepository.resetStopwatch(stopwatch.id)
                return@launch
            }

            // Show add-to-entry UI
            navigateToAddTimeFragment(stopwatch)
        }
    }

    private fun navigateToAddTimeFragment(stopwatch: Stopwatch) {
        // Use Navigation component to show AddStopwatchTimeFragment
        val bundle = Bundle().apply {
            putParcelable("stopwatch", stopwatch)
        }
        findNavController().navigate(
            R.id.action_stopwatch_to_addStopwatchTime,
            bundle
        )
    }
}
```

---

## Phase 2: Lap Assignment (Advanced)

### Goal
User can assign individual laps to different (or same) entries.

### Additional Components

#### 1. LapAssignmentAdapter

**File**: `app/src/main/java/com/yaxer/timetrack/ui/stopwatch/LapAssignmentAdapter.kt`

```kotlin
class LapAssignmentAdapter(
    private val laps: List<Lap>,
    private val onLapSelected: (String) -> Unit
) : RecyclerView.Adapter<LapAssignmentAdapter.ViewHolder>() {

    private val selectedLaps = mutableSetOf<String>()

    inner class ViewHolder(val binding: LapItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lap: Lap) {
            binding.lapCheckbox.text = "${lap.displayName}: ${formatTime(lap.durationMs)}"
            binding.lapCheckbox.isChecked = selectedLaps.contains(lap.id)
            binding.lapCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedLaps.add(lap.id) else selectedLaps.remove(lap.id)
                onLapSelected(lap.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LapItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(laps[position])
    }

    override fun getItemCount() = laps.size
}
```

#### 2. Entry Selection Dialog

**File**: `app/src/main/java/com/yaxer/timetrack/ui/stopwatch/EntrySelectionDialog.kt`

```kotlin
class EntrySelectionDialog(
    private val entries: List<TimeEntry>,
    private val onEntrySelected: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Assign to Entry:")
            .setSingleChoiceItems(
                entries.map { it.name }.toTypedArray(),
                0
            ) { dialog, which ->
                onEntrySelected(entries[which].id)
                dismiss()
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .create()
    }
}
```

#### 3. Extended ViewModel for P2

```kotlin
// In AddStopwatchTimeViewModel

fun toggleLapSelection(lapId: String) {
    val currentState = _state.value ?: return
    val newSelected = currentState.selectedLaps.toMutableSet()

    if (newSelected.contains(lapId)) {
        newSelected.remove(lapId)
    } else {
        newSelected.add(lapId)
    }

    _state.value = currentState.copy(selectedLaps = newSelected)
}

fun assignSelectedLapsToEntry(entryId: String) {
    val currentState = _state.value ?: return
    val newAssignments = currentState.lapToEntryAssignments.toMutableMap()

    currentState.selectedLaps.forEach { lapId ->
        newAssignments[lapId] = entryId
    }

    _state.value = currentState.copy(
        lapToEntryAssignments = newAssignments,
        selectedLaps = emptySet()  // Clear selection after assign
    )
}

fun confirmAndAddLapTimes() {
    viewModelScope.launch {
        try {
            val state = _state.value ?: return@launch

            // Group assignments by entry
            val assignmentsByEntry = state.lapToEntryAssignments
                .groupBy { it.value }  // Group by entryId
                .mapValues { (_, assignments) ->
                    assignments.keys.sumOf { lapId ->
                        state.availableLaps.find { it.id == lapId }?.durationMs ?: 0L
                    }
                }

            // Update all entries (in transaction)
            assignmentsByEntry.forEach { (entryId, timeToAdd) ->
                val entry = timeEntryRepository.getEntry(entryId)
                val newDuration = entry.durationMs + timeToAdd
                timeEntryRepository.updateEntryDuration(entryId, newDuration)
            }

            // Reset stopwatch
            stopwatchRepository.resetStopwatch(state.stopwatch.id)

            // Success - caller closes UI
        } catch (e: Exception) {
            _state.value = _state.value?.copy(error = e.message)
        }
    }
}
```

---

## Testing

### Unit Tests

**File**: `app/src/test/java/com/yaxer/timetrack/ui/stopwatch/AddStopwatchTimeViewModelTest.kt`

```kotlin
class AddStopwatchTimeViewModelTest {

    @Test
    fun addTimeToEntry_WithValidEntry_UpdatesDuration() = runTest {
        // Given
        val stopwatch = mockStopwatch(elapsedMs = 5000)
        val entry = mockEntry(durationMs = 10000)

        // When
        viewModel.initializeForStopwatch(stopwatch)
        viewModel.addTimeToEntry(entry.id)

        // Then - entry duration increased
        verify(repository).updateEntryDuration(entry.id, 15000)
    }

    @Test
    fun toggleLapSelection_TogglesLapInState() = runTest {
        // Given
        viewModel.initializeForStopwatch(mockStopwatch(laps = listOf(mockLap("lap1"))))

        // When
        viewModel.toggleLapSelection("lap1")

        // Then - selectedLaps updated
        viewModel.state.test {
            assertThat(awaitItem().selectedLaps).contains("lap1")
        }
    }

    @Test
    fun confirmAssignment_WithMultipleLaps_AccumulatesCorrectly() = runTest {
        // Given
        val laps = listOf(mockLap("lap1", 2000), mockLap("lap2", 3000))

        // When
        viewModel.assignSelectedLapsToEntry("entry1")
        viewModel.confirmAndAddLapTimes()

        // Then - entry gets 5000ms total
        verify(repository).updateEntryDuration("entry1", 5000)
    }
}
```

### UI Tests

```kotlin
class AddStopwatchTimeFragmentTest {

    @Test
    fun simpleAddUI_ShowsEntryButtons() {
        // Given
        launchFragment(mockStopwatch)

        // When
        onView(withId(R.id.entryButtonsContainer))
            .check(matches(isDisplayed()))

        // Then - all entries shown as buttons
        onView(withText("Project A"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickingEntryButton_AddsTimeAndCloses() {
        // Given
        launchFragment(mockStopwatch)

        // When
        onView(withText("Project A")).perform(click())

        // Then - entry updated
        verify(repository).updateEntryDuration("projectA", 5000)
        // And fragment closed
    }
}
```

---

## Validation Checklist

✅ **P1: Simple Add**
- [ ] Fragment displays when reset with no laps
- [ ] Shows entry list
- [ ] Tapping entry adds time correctly
- [ ] Entry duration updated
- [ ] Stopwatch reset
- [ ] UI closes

✅ **P2: Lap Assignment**
- [ ] Fragment displays when reset with laps
- [ ] Shows lap list with checkboxes
- [ ] Checkbox toggling works
- [ ] User can assign laps to entries
- [ ] Multiple laps to same entry supported
- [ ] Confirm adds all times
- [ ] All entries updated atomically

✅ **General**
- [ ] No lint warnings
- [ ] 60%+ test coverage
- [ ] Error messages clear
- [ ] UI responsive with many laps
- [ ] Handles cancellation gracefully

---

## Files to Create/Modify

| File | Type | Phase |
|------|------|-------|
| AddStopwatchTimeViewModel.kt | New | P1/P2 |
| AddStopwatchTimeFragment.kt | New | P1/P2 |
| LapAssignmentAdapter.kt | New | P2 |
| EntrySelectionDialog.kt | New | P2 |
| StopwatchViewModel.kt | Modify | P1 |
| Navigation graphs | Modify | P1 |
| Test files (3) | New | P1/P2 |

---

## Timeline

- **P1 (Simple Add)**: ~1 week
  - ViewModel with add logic
  - Simple fragment UI
  - Basic testing

- **P2 (Lap Assignment)**: ~4-5 days
  - RecyclerView adapter
  - Multi-select logic
  - Entry assignment dialog
  - ViewModel extensions
  - UI tests

- **Polish & Testing**: ~3-5 days
  - Edge cases
  - Transaction testing
  - Performance validation
  - Final QA

**Total**: 2-3 weeks for full feature
