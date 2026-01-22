# Research: Add Time to Entry from Stopwatch (with Laps)

**Purpose**: Resolve technical unknowns and validate approach before Phase 1 design
**Date**: 2026-01-19
**Feature**: Add Time to Entry from Stopwatch

## Research Questions & Findings

### 1. Lap Data Structure and Retrieval

**Question**: How are laps currently structured in the stopwatch model? Can we retrieve them efficiently?

**Research**: Existing stopwatch implementation + lap recording architecture

**Decision**: Extend existing Lap model with stop/end capability
- **Why chosen**: Laps are already modeled (per spec assumption); just need structured retrieval
- **Implementation approach**:
  - Query: `stopwatchRepository.getLapsForStopwatch(stopwatchId)`
  - Returns ordered list by start time
  - Each lap includes: duration, startTimeMs, endTimeMs

**Rationale**: Stopwatch already tracks laps (per spec). We just need read access for assignment UI.

---

### 2. Multi-Select UI Pattern for Lap Assignment

**Question**: Best UI pattern for users to select multiple laps and assign to entries?

**Research**: Android Material Design patterns for multi-item selection

**Decision**: RecyclerView with checkboxes + Bottom Sheet dialog for entry selection
- **Why chosen**:
  - RecyclerView: Efficient scrolling for 1-20 laps (typical case)
  - Checkboxes: Clear multi-select semantics
  - Bottom Sheet: Non-intrusive, common Android pattern
- **Alternatives considered**:
  - Drag-and-drop: Overkill for typical 1-5 laps
  - Spinner per lap: Verbose UI, slow interaction

**Implementation approach**:
  - RecyclerView adapter with checkbox state
  - Dialog for selecting destination entry
  - LiveData to track selected laps

**Rationale**: Matches Material Design guidelines, familiar to Android users, scales well.

---

### 3. Multiple Laps to Same Entry (Entry Accumulation)

**Question**: How to handle accumulation when multiple laps assigned to same entry?

**Research**: Time entry model and duration accumulation logic

**Decision**: Add durations together; no need for lap-entry join table
- **Why chosen**:
  - Time entries already have duration (sum of all added times)
  - Simple math: newDuration = currentDuration + lapDuration
  - Spec clarification Q2:C allows "both modes available"
- **Alternatives considered**:
  - Junction table: Over-engineers for simple addition
  - Fixed 1:1 lap-to-entry: Restricts user flexibility

**Implementation approach**:
  ```kotlin
  suspend fun assignLapsToEntry(
      entryId: String,
      lapIds: List<String>
  ) {
      val totalLapTime = lapIds
          .map { lapRepository.getLap(it).durationMs }
          .sum()

      val currentEntry = entryRepository.getEntry(entryId)
      val newDuration = currentEntry.durationMs + totalLapTime
      entryRepository.updateEntryDuration(entryId, newDuration)
  }
  ```

**Rationale**: Simplicity principle - no new data model, just arithmetic.

---

### 4. Lap Time Data Accuracy & Validation

**Question**: How to ensure no time is lost or corrupted when adding multiple laps?

**Research**: Transaction patterns, data consistency guarantees

**Decision**: Use Room transactions for lap assignment
- **Why chosen**:
  - ACID guarantees: All laps add or none add
  - Prevents partial updates if app crashes
  - Database-native, no app-level complexity
- **Alternatives considered**:
  - App-level validation: Slower, error-prone
  - Checksum verification: Overkill

**Implementation approach**:
  ```kotlin
  @Transaction
  suspend fun assignLapsToEntries(
      lapAssignments: Map<String, List<String>>  // entryId -> lapIds
  ) {
      lapAssignments.forEach { (entryId, lapIds) ->
          val totalTime = lapIds.sumOf { lapRepository.getLap(it).durationMs }
          // All updates succeed or all fail
          entryRepository.addTimeToEntry(entryId, totalTime)
      }
  }
  ```

**Rationale**: Database transactions provide ACID guarantees automatically.

---

### 5. UI Responsiveness with Many Laps

**Question**: Will UI be responsive if user has 100+ laps in one session?

**Research**: RecyclerView performance benchmarks

**Decision**: RecyclerView with pagination lazy loading if needed
- **Why chosen**:
  - RecyclerView handles 100+ items efficiently (view recycling)
  - Typical use case: 1-10 laps (rarely >20)
  - Edge case (100+ laps): Can add pagination if performance needed
- **Alternatives considered**:
  - Limit display: Misleads user about available laps
  - Firebase recycler: Overkill for local data

**Implementation approach**:
  - Default: Show all laps (1-20 typical)
  - Monitor performance
  - Add pagination if needed (Phase 2+)

**Rationale**: Keep simple for now; optimize if data shows problem.

---

### 6. Cancellation & Undo Recovery

**Question**: If user cancels mid-assignment, how to recover unsaved state?

**Research**: Android Fragment lifecycle and ViewModel state management

**Decision**: ViewModel holds assignment state; cancel=discard changes
- **Why chosen**:
  - ViewModel survives configuration changes
  - Clear semantics: assignment only happens on explicit confirm
  - User can restart if needed
- **Alternatives considered**:
  - Draft mode with save: Complex state management
  - Auto-save: Can confuse users

**Implementation approach**:
  - ViewModel.selectedLaps: MutableStateFlow (cleared on cancel)
  - Fragment lifecycle: Proper cleanup in onDestroyView()
  - No database writes until user confirms

**Rationale**: Simpler mental model for users and developers.

---

## Design Decisions Summary

| Decision | Chosen Approach | Key Rationale |
|----------|-----------------|--------------|
| Lap retrieval | Existing repo query | Laps already tracked |
| Multi-select UI | RecyclerView + checkboxes | Material Design standard |
| Entry accumulation | Simple duration addition | Simplicity, no new model |
| Data accuracy | Room transactions | ACID guarantees |
| Many laps handling | RecyclerView pagination ready | Optimize if needed |
| Cancellation | ViewModel state discard | Clear semantics |
| Data model | Extend existing (no new tables) | Reuse pattern |

---

## Phase 1 Readiness

✅ All research questions resolved
✅ UI patterns validated
✅ Data consistency approach confirmed
✅ No unresolved clarifications

**Status**: Ready to proceed to Phase 1 (data model, contracts, quickstart)
