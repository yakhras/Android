# Implementation Plan: Add Time to Entry from Stopwatch (with Laps)

**Branch**: `002-stopwatch-add-time` | **Date**: 2026-01-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/002-stopwatch-add-time/spec.md`

## Summary

Enable users to quickly add stopwatch elapsed time to time tracking entries when resetting the stopwatch. For stopwatches with recorded laps, users can assign each individual lap to different (or same) entries. Supports both simple one-tap-add workflows and advanced granular lap tracking for multi-task sessions.

**MVP (P1)**: Add total stopwatch time on reset
**Advanced (P2)**: Granular lap-to-entry assignment with flexibility

## Technical Context

**Language/Version**: Kotlin 1.9+ with Android API 26+ (API 34 target)
**Primary Dependencies**:
- AndroidX Room (for lap data persistence if new)
- AndroidX Lifecycle (ViewModel, LiveData/Flow)
- Kotlin Coroutines (async operations)

**Storage**: Local Room Database (reuse or extend existing timer/stopwatch schema)
**Testing**: JUnit 4 + Mockito (unit), Espresso (UI integration)
**Target Platform**: Android API 26-34
**Project Type**: Mobile (single Android app)
**Performance Goals**: Add-to-entry interface appears <1 second after reset press
**Constraints**:
- Lap data must be accurate (no time loss/corruption)
- Support lap durations from 1 second to 24+ hours
- UI responsive during lap selection (smooth scrolling for 100+ laps edge case)

**Scale/Scope**: Single device, local time tracking, typical 1-10 laps per session (rarely >20)

## Constitution Check

✅ **Code Quality Principle**: UI components and database operations MUST follow Android patterns
- Room DAO patterns for data access
- Fragment/Dialog for add-to-entry UI
- No deprecated APIs
- All public methods documented

✅ **Testing Discipline**: Lap time assignment logic MUST be tested
- Unit tests: lap calculation, entry accumulation, validation
- UI tests: lap selection, entry selection, confirmation
- Target: 60% coverage for lap assignment logic

✅ **Simplicity Principle**: Reuse existing time entry and stopwatch models
- Build on top of existing stopwatch infrastructure
- No new database table unless necessary
- Single responsibility: UI for lap assignment + validation

**Status**: ✅ All principles satisfied - proceed to design phase

## Project Structure

### Documentation (this feature)

```text
specs/002-stopwatch-add-time/
├── plan.md              # This file
├── research.md          # Phase 0 (research findings)
├── data-model.md        # Phase 1 (data entities & relationships)
├── quickstart.md        # Phase 1 (setup & implementation guide)
├── contracts/           # Phase 1 (UI/data contracts)
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
app/src/main/java/com/yaxer/timetrack/
├── ui/
│   ├── stopwatch/
│   │   ├── AddStopwatchTimeFragment.kt      # Main UI for adding time
│   │   ├── LapAssignmentAdapter.kt          # RecyclerView adapter for lap list
│   │   └── EntrySelectionDialog.kt          # Entry picker dialog
│   │
│   └── (existing fragments reused)
│
├── viewmodel/
│   ├── StopwatchViewModel.kt               # Modify to handle add-time flow
│   └── AddStopwatchTimeViewModel.kt        # New: handles lap assignment logic
│
└── repository/
    ├── StopwatchRepository.kt              # Reuse: get stopwatch + laps
    └── TimeEntryRepository.kt              # Reuse: add time to entries

app/src/test/java/com/yaxer/timetrack/
├── viewmodel/
│   └── AddStopwatchTimeViewModelTest.kt    # Test lap assignment logic
└── repository/
    └── StopwatchRepositoryTest.kt          # Test lap data retrieval

app/src/androidTest/java/com/yaxer/timetrack/ui/stopwatch/
└── AddStopwatchTimeFragmentTest.kt         # Test UI flow
```

**Structure Decision**: Single mobile app architecture. UI-driven feature (Fragments/Dialogs) that orchestrates lap assignment. Reuses existing stopwatch/entry repositories. No new database tables - extends existing Stopwatch/Lap models with assignment logic.

## Complexity Tracking

**No Constitution violations identified.** Feature integrates with existing stopwatch and time entry infrastructure. Lap assignment is business logic (not architectural complexity).

---

## Phase 0: Research & Decisions ✅ COMPLETE

**Output**: `research.md`

Research resolved:
1. Lap data structure and retrieval from stopwatch
2. Multi-select UI patterns for lap assignment
3. Entry accumulation logic (multiple laps to same entry)
4. Data validation (time accuracy, no corruption)
5. UI responsiveness with large lap counts
6. Undo/recovery if assignment cancelled mid-flow

**Status**: ✅ Ready for Phase 1 design

---

## Phase 1: Design & Contracts ✅ COMPLETE

**Artifacts generated**:
- ✅ `research.md` - Research findings and technical decisions
- ✅ `data-model.md` - Lap, StopwatchTimeAssignment, data relationships
- ✅ `contracts/stopwatch-add-time-service.md` - Business logic contract
- ✅ `quickstart.md` - Implementation guide with code examples

### Constitution Check: Re-evaluated (Post-Design)

✅ **Code Quality Principle**: CONFIRMED
- Room DAO patterns used for lap retrieval
- Fragment/Dialog UI follows Android guidelines
- Kotlin conventions maintained
- Public methods documented

✅ **Testing Discipline**: CONFIRMED
- Unit tests: lap calculation, entry accumulation
- UI tests: lap selection flow, entry selection
- Target: 60% coverage for AddStopwatchTimeViewModel

✅ **Simplicity Principle**: CONFIRMED
- Reuses existing Stopwatch, Lap, TimeEntry models
- No new tables (extends existing schema)
- Lap assignment is isolated business logic
- UI is straightforward: list + dialog + confirmation

**Status**: ✅ All principles maintained through design phase

---

## Artifacts Generated

Documentation:
- ✅ `research.md` - Research findings and decisions
- ✅ `data-model.md` - Entity definitions and relationships
- ✅ `contracts/stopwatch-add-time-service.md` - Service interface
- ✅ `quickstart.md` - Implementation roadmap with code

Source code structure (to be created in Phase 2):
```
New Files:
├── ui/stopwatch/AddStopwatchTimeFragment.kt
├── ui/stopwatch/LapAssignmentAdapter.kt
├── ui/stopwatch/EntrySelectionDialog.kt
├── viewmodel/AddStopwatchTimeViewModel.kt
├── Test: AddStopwatchTimeViewModelTest.kt
└── Test: AddStopwatchTimeFragmentTest.kt

Modified:
├── StopwatchViewModel.kt (add reset handler)
├── (existing repositories for data access)
```

---

## Phase 2: Implementation (NOT in this command)

The `/speckit.plan` command is complete. Next step: `/speckit.tasks`

Tasks will be generated to cover:
- P1 (MVP): Add total stopwatch time to entry on reset
- P2 (Advanced): Granular lap-to-entry assignment
- Integration: Fragment lifecycle, ViewModel state management
- Testing: Unit + UI tests

**Status**: Ready for `/speckit.tasks` command
