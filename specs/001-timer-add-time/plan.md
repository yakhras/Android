# Implementation Plan: Add Elapsed Time to Entry from Timer

**Branch**: `001-timer-add-time` | **Date**: 2026-01-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-timer-add-time/spec.md`

## Summary

Enable users to quickly add timer elapsed time to time tracking entries when the timer completes or is manually stopped. Includes support for creating new entries on-the-fly if none exist. P1 focuses on core timer→entry workflow; P2 adds flexible entry selection with multiple entries.

**MVP (P1)**: Timer completion triggers add-to-entry interface
**Advanced (P2)**: Browse and select from multiple entries

## Technical Context

**Language/Version**: Kotlin 1.9+ with Android API 26+ (API 34 target)
**Primary Dependencies**:
- AndroidX Lifecycle (ViewModel, LiveData/Flow)
- AndroidX Room (time entry persistence)
- Kotlin Coroutines (async operations)

**Storage**: Local Room Database (reuse/extend existing timer and entry schemas)
**Testing**: JUnit 4 + Mockito (unit), Espresso (UI integration)
**Target Platform**: Android API 26-34
**Project Type**: Mobile (single Android app)
**Performance Goals**: Add-to-entry interface appears <1 second after timer stop
**Constraints**:
- Time data must be accurate (no rounding, precision to seconds)
- Support timer durations 0 seconds to 24+ hours
- New Entry creation must not exceed 2 seconds inline

**Scale/Scope**: Single device, local time tracking, typical 5-20 entries

## Constitution Check

✅ **Code Quality Principle**: UI and data operations MUST follow Android patterns
- ViewModel + LiveData/Flow for state management
- Fragment/Dialog for UI flows
- Room DAO for data access
- No deprecated APIs
- KDoc on all public methods

✅ **Testing Discipline**: Add-time logic MUST be tested
- Unit tests: time accumulation, entry updates, new entry creation
- UI tests: timer completion detection, add-to-entry flow
- Target: 60% coverage for TimerAddTimeViewModel

✅ **Simplicity Principle**: Reuse existing timer and entry models
- Extend existing infrastructure (no new tables)
- Single responsibility: timer completion → add-time UI
- No complex orchestration

**Status**: ✅ All principles satisfied - proceed to design phase

## Project Structure

### Documentation

```text
specs/001-timer-add-time/
├── plan.md              # This file
├── research.md          # Phase 0 (research findings)
├── data-model.md        # Phase 1 (data entities)
├── quickstart.md        # Phase 1 (implementation guide)
├── contracts/           # Phase 1 (UI/service contracts)
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code

```text
app/src/main/java/com/yaxer/timetrack/
├── ui/
│   ├── timer/
│   │   ├── AddTimerTimeFragment.kt         # Main UI for adding time
│   │   ├── EntrySelectionAdapter.kt        # RecyclerView for entry list
│   │   └── NewEntryDialog.kt               # Quick new entry creation
│   │
│   └── (existing timer fragments reused)
│
├── viewmodel/
│   ├── TimerViewModel.kt                   # Modify: timer completion handler
│   └── AddTimerTimeViewModel.kt            # New: add-time business logic
│
└── repository/
    ├── TimerRepository.kt                  # Reuse: timer queries
    └── TimeEntryRepository.kt              # Reuse: entry operations

app/src/test/java/com/yaxer/timetrack/
├── viewmodel/
│   └── AddTimerTimeViewModelTest.kt        # Test add-time logic
└── repository/
    └── TimeEntryRepositoryTest.kt          # Test entry updates

app/src/androidTest/java/com/yaxer/timetrack/ui/timer/
└── AddTimerTimeFragmentTest.kt             # Test UI flow
```

**Structure Decision**: Single mobile app. Timer completion event triggers add-to-entry flow (Fragments/Dialogs). Reuses existing timer and entry repositories. No new database tables—extends existing Timer/TimeEntry models.

## Complexity Tracking

**No Constitution violations identified.** Feature is straightforward UI event handler + time accumulation built on existing infrastructure.

---

## Phase 0: Research ✅ COMPLETE

**Output**: `research.md`

Resolved:
1. Timer completion detection (timer reaches 00:00 or user taps stop)
2. Add-to-entry UI pattern (Dialog with entry selection)
3. Time accumulation logic (add elapsed to existing duration)
4. New entry creation flow (inline quick add)
5. State preservation (no data loss on UI dismiss)

**Status**: Ready for Phase 1

---

## Phase 1: Design & Contracts ✅ COMPLETE

**Artifacts**:
- ✅ `research.md` - Research findings
- ✅ `data-model.md` - Entity relationships
- ✅ `contracts/timer-add-time-service.md` - Service interface
- ✅ `quickstart.md` - Implementation guide

**Constitution Check: Re-evaluated**

✅ **Code Quality**: ViewModel + Repository patterns, Kotlin conventions, KDoc
✅ **Testing**: Unit + UI tests, 60% coverage target
✅ **Simplicity**: Reuses models, no new complexity

**Status**: ✅ Ready for Phase 2 (task generation)

---

## Artifacts Generated

Documentation:
- ✅ plan.md (updated)
- ✅ research.md (research findings)
- ✅ data-model.md (entities and operations)
- ✅ contracts/timer-add-time-service.md (API specification)
- ✅ quickstart.md (implementation roadmap)

Source code (to be created in Phase 2):
```
New: AddTimerTimeFragment.kt, AddTimerTimeViewModel.kt,
     EntrySelectionAdapter.kt, NewEntryDialog.kt, tests (3)
Modify: TimerViewModel.kt
```

---

## Phase 2: Implementation (NOT in this command)

Next: `/speckit.tasks` command

Tasks will cover:
- P1: Timer completion → add-to-entry UI
- P2: Multiple entry selection
- Integration with existing timer
- Testing (unit + UI)

**Status**: Ready for task generation
