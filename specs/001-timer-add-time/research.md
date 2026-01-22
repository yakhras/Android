# Research: Add Elapsed Time to Entry from Timer

**Purpose**: Resolve technical unknowns and validate approach
**Date**: 2026-01-19
**Feature**: Add Elapsed Time to Entry from Timer

## Research Findings

### 1. Timer Completion Detection
**Decision**: Observer pattern on timer state (LiveData/Flow)
- **Why**: Matches Android best practices, already used in codebase
- **Implementation**: TimerViewModel observes timer.isRunning state change to false

### 2. Add-to-Entry UI Pattern
**Decision**: BottomSheetDialogFragment with entry list
- **Why**: Material Design standard, non-intrusive, familiar to Android users
- **Implementation**: Shows entries in RecyclerView, easy entry selection

### 3. Time Accumulation Logic
**Decision**: Simple arithmetic (newDuration = oldDuration + timerElapsed)
- **Why**: Simplicity principle, no new data model needed
- **Implementation**: Direct Room query update

### 4. New Entry Creation
**Decision**: Quick inline dialog on timer completion if no entries exist
- **Why**: Spec Q1: C requires "pre-populate with New Entry option"
- **Implementation**: Show dialog with name input field, save immediately

### 5. State Preservation
**Decision**: Store timer elapsed in ViewModel, recover on UI dismiss
- **Why**: No data loss (per FR-009), ViewModel survives configuration changes
- **Implementation**: LiveData holds timer state during UI flow

## Phase 1 Readiness

✅ All decisions confirmed
✅ No unresolved clarifications

**Status**: Ready to proceed to Phase 1 design
