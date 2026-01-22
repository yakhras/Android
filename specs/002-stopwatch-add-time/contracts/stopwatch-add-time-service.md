# Contract: StopwatchAddTimeService Interface

**Purpose**: Define the business logic and state management for stopwatch add-time feature
**Feature**: Add Time to Entry from Stopwatch
**Type**: Kotlin Interface/ViewModel Contract

## Interface Definition

```kotlin
/**
 * Manages the workflow of adding stopwatch time to time entries.
 * Handles both P1 (total time) and P2 (lap-by-lap assignment).
 */
interface StopwatchAddTimeService {

    /**
     * Initialize the add-time workflow when user presses reset.
     *
     * @param stopwatch The stopwatch being reset (with elapsed time and laps)
     * @return Result with initial UI state (lap list or simple total view)
     */
    suspend fun initializeAddTimeFlow(stopwatch: Stopwatch): Result<StopwatchAddTimeState>

    /**
     * Get all available entries user can assign time to.
     *
     * @return List of TimeEntry objects
     */
    suspend fun getAvailableEntries(): Result<List<TimeEntry>>

    /**
     * Select or deselect a lap for assignment.
     *
     * P1 (no laps): Not called
     * P2 (has laps): Called for each lap user clicks
     *
     * @param lapId The lap ID to toggle
     * @return Updated StopwatchAddTimeState with new selectedLaps
     */
    fun toggleLapSelection(lapId: String): StopwatchAddTimeState

    /**
     * Assign one or more selected laps to a specific entry.
     *
     * @param selectedLapIds Laps to assign (can be 1 or all)
     * @param entryId Target entry
     * @return Updated state with lap-to-entry assignment recorded
     */
    fun assignLapsToEntry(
        selectedLapIds: List<String>,
        entryId: String
    ): StopwatchAddTimeState

    /**
     * Clear assignment for specific lap (user changes mind).
     *
     * @param lapId The lap to unassign
     * @return Updated state
     */
    fun clearLapAssignment(lapId: String): StopwatchAddTimeState

    /**
     * Confirm and persist all lap assignments to entries.
     * ATOMIC OPERATION: All succeed or all fail (no partial updates).
     *
     * @return Result.success if all entries updated
     *         Result.failure if any entry update fails
     */
    suspend fun confirmAndAddTimeToEntries(): Result<Unit>

    /**
     * Cancel the add-time workflow without persisting changes.
     *
     * @return Result indicating cleanup complete
     */
    suspend fun cancelAddTimeFlow(): Result<Unit>

    /**
     * Get current UI state (for observation by Fragment/UI).
     *
     * @return StateFlow<StopwatchAddTimeState> for reactive updates
     */
    fun getState(): StateFlow<StopwatchAddTimeState>

    /**
     * For P1 simple add: directly add total time to entry.
     *
     * @param stopwatch Stopwatch with elapsed time (no laps used)
     * @param entryId Target entry
     * @return Result with updated entry
     */
    suspend fun addTotalStopwatchTimeToEntry(
        stopwatch: Stopwatch,
        entryId: String
    ): Result<Unit>
}
```

---

## Usage Scenarios

### P1 Scenario: No Laps (Simple Add)

```
USER: Presses Reset on stopwatch with no laps
  │
  └─> Fragment calls: service.initializeAddTimeFlow(stopwatch)
      │
      └─> Returns: StopwatchAddTimeState(
            stopwatch=stopwatch,
            availableLaps=[],  // Empty
            availableEntries=[Entry1, Entry2, ...]
          )
      │
      └─> UI shows: "Add 5:30 to which entry?"
          Buttons: [Entry1] [Entry2] ... [Cancel]
      │
      └─> User clicks Entry1
          │
          └─> service.addTotalStopwatchTimeToEntry(stopwatch, entry1.id)
              │
              └─> Result.success()
                  Entry1.durationMs += stopwatch.elapsedTimeMs
                  Stopwatch reset
                  UI closes
```

### P2 Scenario: With Laps (Multi-Assign)

```
USER: Presses Reset on stopwatch with 3 laps
  │
  └─> Fragment calls: service.initializeAddTimeFlow(stopwatch)
      │
      └─> Returns: StopwatchAddTimeState(
            stopwatch=stopwatch,
            availableLaps=[Lap1(2m), Lap2(3m), Lap3(1.5m)],
            selectedLaps=[]
          )
      │
      └─> UI shows: Lap list with checkboxes
          - [ ] Lap 1 (2:00)
          - [ ] Lap 2 (3:00)
          - [ ] Lap 3 (1:30)
          [Total: 0]  [Assign to Entry]  [Cancel]
      │
      └─> User clicks Lap1 and Lap2 checkboxes
          │
          └─> service.toggleLapSelection("lap1")
              service.toggleLapSelection("lap2")
              │
              └─> UI updates:
                  - [x] Lap 1 (2:00)
                  - [x] Lap 2 (3:00)
                  - [ ] Lap 3 (1:30)
                  [Total: 5:00]  [Assign to Entry]  [Cancel]
      │
      └─> User clicks "Assign to Entry"
          │
          └─> Show entry selection dialog
              │
              └─> User selects "Project A"
                  │
                  └─> service.assignLapsToEntry(["lap1", "lap2"], "projectA")
                      │
                      └─> UI updates: "5:00 → Project A" ✓
                          Lap 1 and 2 show assignment badge
      │
      └─> User repeats for Lap3 (assigns to "Project B")
          │
          └─> service.assignLapsToEntry(["lap3"], "projectB")
      │
      └─> User clicks "Confirm"
          │
          └─> service.confirmAndAddTimeToEntries()
              │
              └─> Transaction:
                  Project A.durationMs += 5:00
                  Project B.durationMs += 1:30
                  Stopwatch.reset()
                  │
              └─> Result.success()
                  UI closes, both entries updated
```

---

## State Contract

The StateFlow emitted by `getState()` transitions as follows:

```kotlin
data class StopwatchAddTimeState(
    val stopwatch: Stopwatch,
    val availableLaps: List<Lap>,
    val selectedLaps: Set<String>,
    val availableEntries: List<TimeEntry>,
    val lapToEntryAssignments: Map<String, String>,  // lapId -> entryId
    val isLoading: Boolean = false,
    val error: String? = null
)

// Transitions:
// INITIAL: isLoading=true, error=null
// READY: isLoading=false, selectedLaps=empty, lapToEntryAssignments=empty
// USER SELECTS: selectedLaps updated (reactive)
// USER ASSIGNS: lapToEntryAssignments updated (reactive)
// CONFIRMING: isLoading=true
// DONE: UI closed by Fragment (cleanup)
// ERROR: error != null, UI shows message
```

---

## Error Handling

| Scenario | Response |
|----------|----------|
| **No entries available** | Result.failure("No entries to add time to") |
| **No time to add** | Result.failure("Stopwatch has 0 elapsed time") |
| **Entry not found during save** | Result.failure("Entry was deleted") |
| **DB transaction fails** | Result.failure(Exception) - no partial updates |
| **Lap data corrupted** | Result.failure("Lap validation failed") |
| **User selects invalid entry** | UI validation prevents (disabled states) |

---

## Performance Contracts

| Operation | Target | Notes |
|-----------|--------|-------|
| Initialize flow | < 500ms | Query stopwatch + entries |
| Toggle lap selection | < 50ms | In-memory state update |
| Assign lap to entry | < 50ms | In-memory map update |
| Confirm assignment | < 1s | DB transaction + reset |

---

## Thread Safety

All operations are **coroutine-based** (suspend functions):

```kotlin
// Safe to call from UI thread (Main dispatcher)
coroutineScope.launch {
    service.confirmAndAddTimeToEntries()
}

// Service implementation switches to IO dispatcher if needed
// UI thread never blocked
```

---

## Testing Expectations

### Unit Tests

```kotlin
// Test P1 simple add
fun addTotalTime_WithValidEntry_UpdatesEntry() { }

// Test P2 lap selection
fun toggleLapSelection_TogglesCheckbox() { }
fun assignLapsToEntry_UpdatesState() { }

// Test validation
fun confirmAssignment_WithoutEntries_ReturnsError() { }
fun confirmAssignment_WithInvalidEntry_ReturnsError() { }

// Test transactions
fun confirmAssignment_PartialFailure_RollsBack() { }
```

### Integration Tests

```kotlin
// Test end-to-end flow
fun addStopwatchTime_E2E_UpdatesDatabase() { }
fun multipleLatAssignment_E2E_AccumulatesCorrectly() { }
```

---

## Assumptions

- Stopwatch is not running when reset pressed
- TimeEntry repository available and working
- Database transactions supported
- All lap durations are valid (>0)
- User can have multiple entries created

---

## Implementation Notes

- Implement as ViewModel (lifecycle-aware)
- Use StateFlow for reactive state
- Repository pattern for data access
- Room transactions for atomicity
- Clear error messages for UI display

**Ready for Phase 2**: Task generation with detailed implementation steps
