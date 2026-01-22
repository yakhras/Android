# Data Model: Add Time to Entry from Stopwatch

**Purpose**: Define entities and data structures for stopwatch add-time feature
**Date**: 2026-01-19
**Feature**: Add Time to Entry from Stopwatch (with Laps)

## Entity Overview

This feature reuses existing Stopwatch, Lap, and TimeEntry entities. No new database tables needed.

---

## Entities (Existing)

### 1. Stopwatch

```kotlin
data class Stopwatch(
    val id: String,                    // Unique identifier
    val name: String,                  // User-assigned name (optional)
    val elapsedTimeMs: Long,           // Total accumulated time
    val isRunning: Boolean,            // Current state
    val createdAtMs: Long,             // Creation timestamp
    val laps: List<Lap> = emptyList()  // Recorded laps
)
```

### 2. Lap

```kotlin
data class Lap(
    val id: String,                    // Unique lap ID
    val stopwatchId: String,           // Parent stopwatch ID
    val durationMs: Long,              // Lap duration
    val startTimeMs: Long,             // When lap started
    val endTimeMs: Long,               // When lap ended
    val lapNumber: Int                 // Sequence number (1, 2, 3...)
) {
    val displayName: String get() = "Lap $lapNumber"
}
```

### 3. TimeEntry

```kotlin
data class TimeEntry(
    val id: String,                    // Unique entry ID
    val name: String,                  // Entry name (task/project)
    val durationMs: Long,              // Total accumulated time
    val createdAtMs: Long,             // Creation timestamp
    val lastModifiedAtMs: Long         // Last update timestamp
)
```

---

## New Entities (for Feature)

### StopwatchAddTimeState (UI State)

Represents the state of the add-to-entry workflow.

```kotlin
data class StopwatchAddTimeState(
    val stopwatch: Stopwatch,
    val availableLaps: List<Lap>,      // Laps to display in UI
    val selectedLaps: Set<String>,     // User-selected lap IDs
    val availableEntries: List<TimeEntry>,  // Entries user can choose
    val lapToEntryAssignments: Map<String, String>,  // lapId -> entryId
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // Total time to be added
    val totalTimeToAdd: Long get() =
        selectedLaps.sumOf { lapId ->
            availableLaps.find { it.id == lapId }?.durationMs ?: 0L
        }

    // Grouped by entry: entryId -> total time from assigned laps
    val assignmentsByEntry: Map<String, Long> get() =
        lapToEntryAssignments.mapNotNull { (lapId, entryId) ->
            val lapTime = availableLaps.find { it.id == lapId }?.durationMs
            lapTime?.let { entryId to it }
        }.groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.sumOf { it.second } }
}
```

---

## Relationships

```
┌─────────────────────────────────────┐
│      Stopwatch (Existing)           │
│  - id, name, elapsedTimeMs, laps[]  │
└──────────────────┬──────────────────┘
                   │ (1:M)
                   ▼
┌─────────────────────────────────────┐
│      Lap[] (Existing)               │
│  - id, durationMs, startTimeMs...   │
└──────────────────┬──────────────────┘
                   │ (User selects)
                   ▼
┌─────────────────────────────────────┐
│    StopwatchAddTimeState (New)      │
│  - selectedLaps: Set<String>        │
│  - lapToEntryAssignments: Map       │
└──────────────────┬──────────────────┘
                   │ (User assigns to)
                   ▼
┌─────────────────────────────────────┐
│      TimeEntry[] (Existing)         │
│  - id, name, durationMs             │
│  (durationMs updated after add)     │
└─────────────────────────────────────┘
```

**Multiplicity**:
- 1 Stopwatch → 0..M Laps (recorded during session)
- 1 Stopwatch reset event → 1 StopwatchAddTimeState (UI state)
- N Laps → 1..M TimeEntry (distribution depends on user assignment)
- Multiple Laps → Same Entry (allowed per spec Q2:C)

---

## Operations

### User Story 1 (P1): Add Total Stopwatch Time

```kotlin
// User presses Reset with no laps
suspend fun addTotalStopwatchTimeToEntry(
    stopwatch: Stopwatch,
    entryId: String
): Result<Unit> {
    return try {
        val newDuration = timeEntryRepository.getEntry(entryId).durationMs +
                         stopwatch.elapsedTimeMs

        timeEntryRepository.updateEntryDuration(entryId, newDuration)
        stopwatchRepository.resetStopwatch(stopwatch.id)

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### User Story 2 (P2): Assign Individual Laps

```kotlin
// User selects multiple laps and assigns to entries
@Transaction
suspend fun assignLapsToEntries(
    stopwatch: Stopwatch,
    lapToEntryAssignments: Map<String, String>  // lapId -> entryId
): Result<Unit> {
    return try {
        // Group laps by target entry
        val assignmentsByEntry = lapToEntryAssignments
            .groupBy { it.value }  // Group by entryId
            .mapValues { (_, assignments) ->
                // Sum all lap times for this entry
                assignments.keys.sumOf { lapId ->
                    stopwatch.laps.find { it.id == lapId }?.durationMs ?: 0L
                }
            }

        // Update all entries within transaction
        assignmentsByEntry.forEach { (entryId, timeToAdd) ->
            val entry = timeEntryRepository.getEntry(entryId)
            val newDuration = entry.durationMs + timeToAdd
            timeEntryRepository.updateEntryDuration(entryId, newDuration)
        }

        // Reset stopwatch after successful assignment
        stopwatchRepository.resetStopwatch(stopwatch.id)

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## State Transitions

```
[Stopwatch Running]
        │
        └─> User presses Reset
            │
            ├─> IF no laps:
            │   Show: "Add {total} to entry?"
            │   Options: Select entry, Cancel
            │
            └─> IF has laps:
                Show: Lap list (checkboxes)
                Options: Select laps, Assign to entries, Cancel
                        │
                        ├─> User selects lap(s)
                        │   │
                        │   └─> Show entry selection dialog
                        │       │
                        │       └─> User selects entry
                        │           │
                        │           └─> Update assignment (live)
                        │
                        └─> User confirms all assignments
                            │
                            └─> Add times to entries
                                │
                                └─> Reset stopwatch
                                    │
                                    └─> [Ready for next session]

            User can Cancel at any point:
            Cancel → [Stopwatch unchanged] → Back to running view
```

---

## Validation Rules

| Field | Rule |
|-------|------|
| Lap Duration | Must be > 0ms and <= stopwatch total |
| Entry ID | Must exist in database |
| Selected Laps | Can be 1 or more, or all laps |
| Time Addition | newDuration = oldDuration + lapDuration (no overflow check needed for reasonable durations) |
| Stopwatch State | Must be not running (paused or stopped before assignment) |

---

## Persistence

**In-Memory** (ViewModel):
- Selected lap IDs
- Lap-to-entry assignments (Map)
- UI state (loading, error)

**Database** (after confirmation):
- TimeEntry.durationMs updated (via UPDATE query)
- Stopwatch reset (laps cleared, elapsed = 0)
- All within transaction

**Cleanup**:
- ViewModel cleared when UI destroyed
- No draft state persisted
- Only confirmed assignments saved to DB

---

## Edge Cases & Handling

| Case | Handling |
|------|----------|
| Zero-duration lap | Validation: reject in assignment |
| Entry not found | DB error: show error message, don't update |
| Stopwatch deleted mid-flow | Clear UI, show error |
| App backgrounded during assignment | ViewModel state preserved (LiveData) |
| User rapidly clicks confirm multiple times | Disable button during transaction |
| Very large stopwatch total (24+ hours) | Store as Long ms, display formatted |
| Many laps (100+) | RecyclerView handles efficiently |

---

## Performance Considerations

- **Lap Retrieval**: O(1) query by stopwatch ID (indexed)
- **Entry Updates**: O(N) where N = number of entries being updated (typically 1-5)
- **Sum Calculation**: O(M) where M = number of selected laps (typically 1-10)
- **Overall**: Responsive for typical use cases

---

## Assumptions

- Stopwatch/Lap/TimeEntry entities already exist and modeled in database
- Laps are already being recorded by stopwatch functionality
- Database supports Room transactions
- TimeEntry.durationMs can hold any reasonable duration value

---

## Phase 2 Readiness

✅ Entity relationships defined
✅ Operations specified
✅ State transitions mapped
✅ Validation rules clear
✅ Edge cases handled

**Status**: Ready for contract and quickstart generation
