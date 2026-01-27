# TimeTrack Android App - Complete Application State Documentation

> **Last Updated:** January 2026
> **Version:** 1.0
> **Package:** `com.yaxer.timetrack`

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Project Structure](#2-project-structure)
3. [Build Configuration](#3-build-configuration)
4. [Architecture Overview](#4-architecture-overview)
5. [Features & Screens](#5-features--screens)
6. [Data Layer](#6-data-layer)
7. [API Integration](#7-api-integration)
8. [Offline-First Architecture](#8-offline-first-architecture)
9. [UI Components](#9-ui-components)
10. [State Management](#10-state-management)
11. [Configuration](#11-configuration)
12. [Testing](#12-testing)
13. [Known Limitations & Future Considerations](#13-known-limitations--future-considerations)

---

## 1. Executive Summary

### App Overview

**TimeTrack** is an Android time tracking application that synchronizes with an Odoo backend server. It allows users to:

- Track time using stopwatch and countdown timer functionality
- Manage projects from Odoo
- Create and view time entries
- Work offline with automatic synchronization

### Core Functionality

| Feature | Description |
|---------|-------------|
| **Stopwatch** | Track elapsed time with lap recording |
| **Countdown Timer** | Set duration, runs countdown, creates entry on completion |
| **Projects** | View and create projects from Odoo |
| **Time Entries** | View entries with inline timer controls (start/pause/stop) |
| **Offline Mode** | Full offline support with sync queue |

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 1.9.x |
| Min SDK | Android 8.0 (API 26) | - |
| Target SDK | Android 14 (API 34) | - |
| Database | Room | 2.6.1 |
| Navigation | Jetpack Navigation | 2.7.7 |
| Background Work | WorkManager | 2.9.0 |
| API Protocol | XML-RPC | Apache 3.1.3 |
| Async | Kotlin Coroutines | 1.7.3 |
| JSON | Gson | 2.10.1 |

### Architecture Pattern

**MVVM + Repository + Offline-First**

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI LAYER                                │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐    │
│  │ Stopwatch │  │  Timer    │  │ Projects  │  │  Entries  │    │
│  │ Fragment  │  │ Fragment  │  │ Fragment  │  │ Fragment  │    │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘    │
│        │              │              │              │           │
│  ┌─────┴─────┐  ┌─────┴─────┐  ┌─────┴─────┐  ┌─────┴─────┐    │
│  │ Stopwatch │  │  Timer    │  │ Projects  │  │ Entries   │    │
│  │ ViewModel │  │ ViewModel │  │ ViewModel │  │ ViewModel │    │
│  └───────────┘  └───────────┘  └─────┬─────┘  └─────┬─────┘    │
└──────────────────────────────────────┼──────────────┼──────────┘
                                       │              │
┌──────────────────────────────────────┼──────────────┼──────────┐
│                      REPOSITORY LAYER│              │          │
│                            ┌─────────┴──────────────┴────┐     │
│                            │    TimeTrackRepository      │     │
│                            │  (Single Source of Truth)   │     │
│                            └──────────────┬──────────────┘     │
└───────────────────────────────────────────┼────────────────────┘
                                            │
        ┌───────────────────────────────────┼───────────────────────┐
        │                                   │                       │
┌───────┴───────┐                 ┌─────────┴─────────┐   ┌─────────┴─────────┐
│   DATA LAYER  │                 │   NETWORK LAYER   │   │   SYNC LAYER      │
│ ┌───────────┐ │                 │ ┌───────────────┐ │   │ ┌───────────────┐ │
│ │   Room    │ │                 │ │ OdooApiClient │ │   │ │  SyncManager  │ │
│ │  Database │ │                 │ │  (XML-RPC)    │ │   │ │  SyncWorker   │ │
│ └───────────┘ │                 │ └───────────────┘ │   │ └───────────────┘ │
│ ┌───────────┐ │                 │ ┌───────────────┐ │   └───────────────────┘
│ │   DAOs    │ │                 │ │NetworkMonitor │ │
│ └───────────┘ │                 │ └───────────────┘ │
└───────────────┘                 └───────────────────┘
```

---

## 2. Project Structure

### Directory Tree

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/yaxer/timetrack/
│   │   │   │
│   │   │   │   # Application Entry Point
│   │   │   ├── TimeTrackApplication.kt      # Application class, DI setup
│   │   │   ├── HostActivity.kt              # Single Activity host
│   │   │   │
│   │   │   │   # Fragments (UI)
│   │   │   ├── StopwatchFragment.kt         # Stopwatch screen
│   │   │   ├── TimerFragment.kt             # Countdown timer screen
│   │   │   ├── ProjectsFragment.kt          # Projects list screen
│   │   │   ├── EntriesFragment.kt           # Time entries screen
│   │   │   │
│   │   │   │   # ViewModels
│   │   │   ├── StopwatchViewModel.kt        # Stopwatch state (simple)
│   │   │   ├── TimerViewModel.kt            # Timer state (simple)
│   │   │   │
│   │   │   │   # Adapters
│   │   │   ├── EntriesAdapter.kt            # RecyclerView adapter with live timers
│   │   │   ├── ProjectsAdapter.kt           # RecyclerView adapter for projects
│   │   │   ├── LapTimesAdapter.kt           # RecyclerView adapter for lap times
│   │   │   │
│   │   │   │   # API Layer
│   │   │   ├── OdooApiClient.kt             # Odoo XML-RPC client (singleton)
│   │   │   ├── OdooConfig.kt                # Server configuration
│   │   │   │
│   │   │   │   # Data Layer
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── Entities.kt          # Room entities (4 tables)
│   │   │   │   │   ├── Daos.kt              # Room DAOs (4 interfaces)
│   │   │   │   │   ├── TimeTrackDatabase.kt # Room database class
│   │   │   │   │   └── Converters.kt        # Type converters
│   │   │   │   │
│   │   │   │   ├── network/
│   │   │   │   │   └── NetworkMonitor.kt    # Connectivity observer
│   │   │   │   │
│   │   │   │   └── repository/
│   │   │   │       ├── TimeTrackRepository.kt # Single source of truth
│   │   │   │       └── Mappers.kt           # API -> Entity mappers
│   │   │   │
│   │   │   │   # Sync Layer
│   │   │   ├── sync/
│   │   │   │   ├── SyncManager.kt           # WorkManager scheduling
│   │   │   │   ├── SyncWorker.kt            # Background sync worker
│   │   │   │   └── SyncPayloads.kt          # JSON payload data classes
│   │   │   │
│   │   │   │   # UI Layer
│   │   │   ├── ui/
│   │   │   │   ├── ViewModelFactory.kt      # Manual DI factory
│   │   │   │   ├── entries/
│   │   │   │   │   └── EntriesViewModel.kt  # Entries screen state
│   │   │   │   └── projects/
│   │   │   │       └── ProjectsViewModel.kt # Projects screen state
│   │   │   │
│   │   │   │   # Utilities
│   │   │   └── util/
│   │   │       ├── DurationFormatter.kt     # Time formatting utilities
│   │   │       └── TimerDurationCalculator.kt # Timer calculations
│   │   │
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_host.xml        # Main activity layout
│   │   │   │   ├── fragment_stopwatch.xml   # Stopwatch UI
│   │   │   │   ├── fragment_timer.xml       # Timer UI
│   │   │   │   ├── fragment_projects.xml    # Projects list UI
│   │   │   │   ├── fragment_entries.xml     # Entries list UI
│   │   │   │   ├── item_entry.xml           # Entry list item
│   │   │   │   ├── item_project.xml         # Project list item
│   │   │   │   ├── item_lap_time.xml        # Lap time list item
│   │   │   │   ├── dialog_create_entry.xml  # Create entry dialog
│   │   │   │   └── dialog_add_timer_entry.xml # Timer completion dialog
│   │   │   │
│   │   │   ├── navigation/
│   │   │   │   └── nav_graph.xml            # Navigation graph
│   │   │   │
│   │   │   └── menu/
│   │   │       └── bottom_nav_menu.xml      # Bottom navigation menu
│   │   │
│   │   └── AndroidManifest.xml              # App manifest
│   │
│   ├── test/java/com/yaxer/timetrack/       # Unit tests
│   │   ├── timer/
│   │   │   ├── DurationFormatterTest.kt
│   │   │   ├── TimerDurationCalculatorTest.kt
│   │   │   ├── TimerViewModelTest.kt
│   │   │   └── TimerBoundaryTest.kt
│   │   └── api/
│   │       └── OdooApiClientTest.kt
│   │
│   └── androidTest/java/com/yaxer/timetrack/ # UI tests
│       └── timer/
│           ├── AddEntryDialogUITest.kt
│           ├── TimerFragmentUITest.kt
│           └── TimerStateTransitionUITest.kt
│
└── build.gradle.kts                          # App-level build config
```

### Package Organization

| Package | Purpose |
|---------|---------|
| `com.yaxer.timetrack` | Root - Application, Activity, Fragments, ViewModels, Adapters |
| `com.yaxer.timetrack.data.local` | Room database entities, DAOs, converters |
| `com.yaxer.timetrack.data.network` | Network connectivity monitoring |
| `com.yaxer.timetrack.data.repository` | Repository pattern, data mappers |
| `com.yaxer.timetrack.sync` | Background sync with WorkManager |
| `com.yaxer.timetrack.ui` | ViewModelFactory, screen-specific ViewModels |
| `com.yaxer.timetrack.util` | Utility classes for formatting and calculations |

---

## 3. Build Configuration

### Gradle Setup (`app/build.gradle.kts`)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")  // For Room code generation
}

android {
    namespace = "com.yaxer.timetrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yaxer.timetrack"
        minSdk = 26          // Android 8.0 Oreo
        targetSdk = 34       // Android 14
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```

### Dependencies

#### Core Android

| Dependency | Version | Purpose |
|------------|---------|---------|
| `androidx.core:core-ktx` | 1.12.0 | Kotlin extensions |
| `androidx.appcompat:appcompat` | 1.6.1 | AppCompat support |
| `com.google.android.material:material` | 1.11.0 | Material Design |
| `androidx.activity:activity-ktx` | 1.8.2 | Activity extensions |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | ConstraintLayout |
| `androidx.fragment:fragment-ktx` | 1.6.2 | Fragment extensions |

#### Architecture Components

| Dependency | Version | Purpose |
|------------|---------|---------|
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.7.0 | Lifecycle-aware components |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.7.0 | ViewModel support |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.7.0 | SavedState support |
| `androidx.navigation:navigation-fragment-ktx` | 2.7.7 | Navigation component |
| `androidx.navigation:navigation-ui-ktx` | 2.7.7 | Navigation UI |

#### Data & Networking

| Dependency | Version | Purpose |
|------------|---------|---------|
| `androidx.room:room-runtime` | 2.6.1 | Room database |
| `androidx.room:room-ktx` | 2.6.1 | Room Kotlin extensions |
| `androidx.room:room-compiler` (KSP) | 2.6.1 | Room annotation processor |
| `org.apache.xmlrpc:xmlrpc-client` | 3.1.3 | Odoo XML-RPC protocol |
| `com.google.code.gson:gson` | 2.10.1 | JSON serialization |

#### Background Processing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.7.3 | Coroutines |
| `androidx.work:work-runtime-ktx` | 2.9.0 | WorkManager |

#### Testing

| Dependency | Version | Purpose |
|------------|---------|---------|
| `junit:junit` | 4.13.2 | Unit testing |
| `io.mockk:mockk` | 1.13.5 | Mocking library |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.7.3 | Coroutines testing |
| `app.cash.turbine:turbine` | 1.1.0 | Flow testing |
| `androidx.test.espresso:espresso-core` | 3.5.1 | UI testing |
| `androidx.test.ext:junit` | 1.1.5 | AndroidX JUnit |

---

## 4. Architecture Overview

### MVVM Pattern Implementation

#### Layer Responsibilities

| Layer | Components | Responsibility |
|-------|------------|----------------|
| **View** | Fragments, Adapters | Display UI, handle user input |
| **ViewModel** | ViewModels | Hold UI state, expose data via StateFlow |
| **Model** | Repository, DAOs, Entities | Data operations, business logic |

#### Single Activity Architecture

```
HostActivity
    └── NavHostFragment
        ├── StopwatchFragment (startDestination)
        ├── TimerFragment
        ├── ProjectsFragment
        └── EntriesFragment
```

**HostActivity** (`HostActivity.kt:9-22`)
- Minimal single activity
- Sets up Navigation Component
- Configures BottomNavigationView

### Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              READ FLOW                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Fragment ──observe──> ViewModel ──collect──> Repository ──Flow──> DAO  │
│     ▲                      │                      │                │    │
│     │                      │                      │                │    │
│     └──────────────────────┴──────────────────────┴────────────────┘    │
│                           StateFlow updates                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                             WRITE FLOW                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Fragment ──action──> ViewModel ──suspend──> Repository                 │
│                                                    │                    │
│                            ┌───────────────────────┴───────────────┐    │
│                            │                                       │    │
│                       [if online]                            [if offline]│
│                            │                                       │    │
│                            ▼                                       ▼    │
│                      OdooApiClient                           SyncQueue  │
│                            │                                       │    │
│                            ▼                                       │    │
│                        Update DB ◄─────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Dependency Injection (Manual)

The app uses manual dependency injection through:

1. **TimeTrackApplication** - Provides singleton instances
2. **ViewModelFactory** - Creates ViewModels with dependencies

```kotlin
// TimeTrackApplication.kt
class TimeTrackApplication : Application() {
    val database: TimeTrackDatabase by lazy { ... }
    val networkMonitor: NetworkMonitor by lazy { ... }
    val repository: TimeTrackRepository by lazy { ... }
    val syncManager: SyncManager by lazy { ... }
}

// ViewModelFactory.kt
class ViewModelFactory(
    private val repository: TimeTrackRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ProjectsViewModel::class.java) ->
                ProjectsViewModel(repository) as T
            modelClass.isAssignableFrom(EntriesViewModel::class.java) ->
                EntriesViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}

// Usage in Fragment
private val viewModel: ProjectsViewModel by viewModels {
    ViewModelFactory((requireActivity().application as TimeTrackApplication).repository)
}
```

---

## 5. Features & Screens

### Screen Overview

| Screen | Fragment | ViewModel | Navigation ID | Purpose |
|--------|----------|-----------|---------------|---------|
| Stopwatch | `StopwatchFragment` | `StopwatchViewModel` | `stopwatchFragment` | Track elapsed time with lap recording |
| Timer | `TimerFragment` | `TimerViewModel` | `timerFragment` | Countdown timer, creates entry on completion |
| Projects | `ProjectsFragment` | `ProjectsViewModel` | `projectsFragment` | View/create projects from Odoo |
| Entries | `EntriesFragment` | `EntriesViewModel` | `entriesFragment` | View entries with inline timer controls |

### Navigation Configuration

**Start Destination:** `stopwatchFragment`

```xml
<!-- nav_graph.xml -->
<navigation app:startDestination="@id/stopwatchFragment">
    <fragment android:id="@+id/stopwatchFragment" ... />
    <fragment android:id="@+id/timerFragment" ... />
    <fragment android:id="@+id/projectsFragment" ... />
    <fragment android:id="@+id/entriesFragment" ... />
</navigation>
```

**Bottom Navigation Menu Order:**
1. Projects
2. Entries
3. Stopwatch
4. Timer

---

### 5.1 Stopwatch Screen

**Files:**
- `StopwatchFragment.kt`
- `StopwatchViewModel.kt`
- `LapTimesAdapter.kt`
- `fragment_stopwatch.xml`

**State Machine:**

```
       ┌─────────┐
       │  IDLE   │◄────────────────┐
       └────┬────┘                 │
            │ Start                │ Reset
            ▼                      │
       ┌─────────┐           ┌─────┴─────┐
       │ RUNNING │──Pause───>│  PAUSED   │
       └────┬────┘           └─────┬─────┘
            │                      │
            │ Lap                  │ Resume
            ▼                      │
     Record lap time               │
            │                      │
            └──────────────────────┘
```

**ViewModel State (`StopwatchViewModel.kt:1-13`):**

```kotlin
enum class StopwatchState { IDLE, RUNNING, PAUSED }

class StopwatchViewModel : ViewModel() {
    var state: StopwatchState = StopwatchState.IDLE
    var startTimeMillis: Long = 0      // SystemClock.elapsedRealtime() at start
    var elapsedTimeMillis: Long = 0    // Accumulated time
    var lastLapTimeMillis: Long = 0    // For calculating lap duration
    val lapTimes: MutableList<LapTime> = mutableListOf()
}
```

**Lap Time Data Class:**

```kotlin
data class LapTime(
    val lapNumber: Int,
    val lapDuration: Long,   // Duration since last lap
    val totalTime: Long      // Total elapsed time at lap
)
```

**Timer Update Mechanism:**
- Uses `Handler` with `postDelayed(runnable, 10)` for 10ms updates
- Display shows centiseconds (`.XX` format)

---

### 5.2 Timer Screen

**Files:**
- `TimerFragment.kt`
- `TimerViewModel.kt`
- `fragment_timer.xml`
- `dialog_add_timer_entry.xml`

**State Machine:**

```
       ┌─────────┐
       │  IDLE   │◄─────────────────┐
       └────┬────┘                  │
            │ Start                 │ Stop/Complete
            ▼                       │
       ┌─────────┐            ┌─────┴─────┐
       │ RUNNING │───Pause───>│  PAUSED   │
       └────┬────┘            └─────┬─────┘
            │                       │
            │ onFinish              │ Resume
            ▼                       │
     Show Add Entry Dialog          │
            │                       │
            └───────────────────────┘
```

**ViewModel State (`TimerViewModel.kt:1-12`):**

```kotlin
enum class TimerState { IDLE, RUNNING, PAUSED }

class TimerViewModel : ViewModel() {
    var timerState: TimerState = TimerState.IDLE
    var remainingMillis: Long = 0     // Current countdown value
    var totalMillis: Long = 0         // Original timer duration
    var timerEndTimeMillis: Long = 0  // SystemClock.elapsedRealtime() when timer will end
}
```

**NumberPicker Configuration:**
- Hours: 0-23 (wrapping)
- Minutes: 0-59 (wrapping)
- Seconds: 0-59 (wrapping)

**Timer Completion Flow:**

```
Timer completes
    │
    ▼
Calculate elapsed minutes
    │
    ▼
Show Add Entry Dialog
    │
    ├── Has projects? ──Yes──> Show project spinner + description
    │       │
    │       ▼
    │   Create entry with time data
    │
    └── No projects? ──> Show "Create Project" dialog
            │
            ▼
        Create project + entry
```

---

### 5.3 Projects Screen

**Files:**
- `ProjectsFragment.kt`
- `ProjectsViewModel.kt` (`ui/projects/`)
- `ProjectsAdapter.kt`
- `fragment_projects.xml`

**ViewModel State (`ProjectsViewModel.kt:21-107`):**

```kotlin
class ProjectsViewModel(private val repository: TimeTrackRepository) : ViewModel() {

    // Observable project list from local cache
    val projects: StateFlow<List<ProjectEntity>>

    // Loading state for progress indicators
    val isLoading: StateFlow<Boolean>

    // One-time error events
    val error: SharedFlow<String>

    // Online/offline status
    val isOnline: StateFlow<Boolean>

    // Actions
    fun refresh()                    // Refresh from API
    fun createProject(name: String)  // Create new project
}
```

**Data Flow:**

```
Fragment observes projects StateFlow
         │
         ▼
Repository.getProjects() returns Flow<List<ProjectEntity>>
         │
         ▼
DAO queries projects table
         │
         ▼
On refresh: API fetch -> update DB -> Flow emits new list
```

---

### 5.4 Entries Screen

**Files:**
- `EntriesFragment.kt`
- `EntriesViewModel.kt` (`ui/entries/`)
- `EntriesAdapter.kt`
- `fragment_entries.xml`
- `item_entry.xml`
- `dialog_create_entry.xml`

**ViewModel State (`EntriesViewModel.kt:23-180`):**

```kotlin
class EntriesViewModel(private val repository: TimeTrackRepository) : ViewModel() {

    // Observable entry list from local cache
    val entries: StateFlow<List<TimeEntryEntity>>

    // Observable project list for entry creation
    val projects: StateFlow<List<ProjectEntity>>

    // Loading state
    val isLoading: StateFlow<Boolean>

    // One-time error events
    val error: SharedFlow<String>

    // Online/offline status
    val isOnline: StateFlow<Boolean>

    // Actions
    fun refresh()                                    // Full refresh
    fun refreshTodayOnly()                           // Light refresh
    fun createEntry(projectId: Int, description: String?)
    fun createEntryWithTime(projectId, startTime, endTime, durationMinutes, description)
    suspend fun startTimer(entryId: Int): Boolean
    suspend fun stopTimer(entryId: Int, pausedSeconds: Long): Boolean
}
```

**Entry List Item Features:**
- Project name
- Status badge (RUNNING/PAUSED or hidden)
- Date display
- Time range (start - end/In progress/Paused)
- Duration (live updating for running entries)
- Description (if present)
- Action buttons (Start/Pause-Resume/Stop)

**Inline Timer Controls:**

```
┌─────────────────────────────────────────────────┐
│  [Entry not running]                            │
│  ┌─────────┐                                    │
│  │  Start  │  (Green button)                    │
│  └─────────┘                                    │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  [Entry running]                                │
│  ┌─────────┐  ┌─────────┐                       │
│  │  Pause  │  │  Stop   │  (Orange + Red)       │
│  └─────────┘  └─────────┘                       │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  [Entry paused]                                 │
│  ┌─────────┐  ┌─────────┐                       │
│  │ Resume  │  │  Stop   │  (Green + Red)        │
│  └─────────┘  └─────────┘                       │
└─────────────────────────────────────────────────┘
```

---

## 6. Data Layer

### Room Database

**Database Configuration (`TimeTrackDatabase.kt:17-58`):**

```kotlin
@Database(
    entities = [
        ProjectEntity::class,
        TimeEntryEntity::class,
        SyncQueueEntity::class,
        RunningTimerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeTrackDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun runningTimerDao(): RunningTimerDao
}
```

**Database Name:** `timetrack.db`

**Migration Strategy:** `fallbackToDestructiveMigration()` (data loss on schema change)

---

### Entities

#### 6.1 ProjectEntity

**Table:** `projects`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Int (PK) | Odoo project ID (negative for offline) |
| `name` | String | Project name |
| `code` | String? | Project code (nullable) |
| `active` | Boolean | Active status |
| `syncStatus` | SyncStatus | SYNCED, PENDING_SYNC, SYNC_FAILED |
| `lastSyncedAt` | Long | Timestamp of last sync |

```kotlin
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val code: String?,
    val active: Boolean,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
```

---

#### 6.2 TimeEntryEntity

**Table:** `time_entries`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Int (PK) | Odoo entry ID (negative for offline) |
| `projectId` | Int | Foreign key to project |
| `projectName` | String | Denormalized project name |
| `description` | String | Entry description |
| `date` | String | Date (YYYY-MM-DD format) |
| `unitAmount` | Float | Duration in minutes |
| `startTime` | String? | Start time (YYYY-MM-DD HH:mm:ss) |
| `endTime` | String? | End time (YYYY-MM-DD HH:mm:ss) |
| `isRunning` | Boolean | Timer running flag |
| `syncStatus` | SyncStatus | Sync state |
| `localId` | String? | UUID for offline entries |
| `lastSyncedAt` | Long | Timestamp of last sync |

```kotlin
@Entity(tableName = "time_entries")
data class TimeEntryEntity(
    @PrimaryKey val id: Int,
    val projectId: Int,
    val projectName: String,
    val description: String,
    val date: String,
    val unitAmount: Float,
    val startTime: String? = null,
    val endTime: String? = null,
    val isRunning: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val localId: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
```

---

#### 6.3 SyncQueueEntity

**Table:** `sync_queue`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long (PK, auto) | Auto-generated ID |
| `entityType` | String | "project", "time_entry", "timer_start", etc. |
| `entityId` | String | ID of the entity to sync |
| `operation` | SyncOperation | CREATE, UPDATE, DELETE |
| `payload` | String | JSON payload for the operation |
| `createdAt` | Long | Queue entry timestamp |
| `retryCount` | Int | Number of sync attempts |
| `lastError` | String? | Last error message |

```kotlin
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)
```

---

#### 6.4 RunningTimerEntity

**Table:** `running_timers`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Int (PK) | Always 1 (single timer) |
| `projectId` | Int | Project for the timer |
| `projectName` | String | Project name |
| `description` | String | Timer description |
| `startTime` | Long | Start timestamp |
| `elapsedSeconds` | Long | Accumulated time |
| `isPaused` | Boolean | Paused state |

```kotlin
@Entity(tableName = "running_timers")
data class RunningTimerEntity(
    @PrimaryKey val id: Int = 1,
    val projectId: Int,
    val projectName: String,
    val description: String,
    val startTime: Long,
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false
)
```

---

### Enums

```kotlin
enum class SyncStatus {
    SYNCED,        // In sync with server
    PENDING_SYNC,  // Waiting to be synced
    SYNC_FAILED    // Sync attempt failed
}

enum class SyncOperation {
    CREATE,  // Create new record
    UPDATE,  // Update existing record
    DELETE   // Delete record
}
```

---

### DAOs

#### ProjectDao (`Daos.kt:13-36`)

```kotlin
@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("SELECT * FROM projects WHERE active = 1 ORDER BY name ASC")
    fun getAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE active = 1 ORDER BY name ASC")
    suspend fun getAllSync(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Int): ProjectEntity?

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}
```

#### TimeEntryDao (`Daos.kt:41-76`)

```kotlin
@Dao
interface TimeEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimeEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimeEntryEntity>)

    @Query("SELECT * FROM time_entries ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries ORDER BY date DESC, id DESC")
    suspend fun getAllSync(): List<TimeEntryEntity>

    @Query("SELECT * FROM time_entries WHERE date = :date ORDER BY id DESC")
    fun getByDate(date: String): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries WHERE date = :date ORDER BY id DESC")
    suspend fun getByDateSync(date: String): List<TimeEntryEntity>

    @Query("SELECT * FROM time_entries WHERE id = :id")
    suspend fun getById(id: Int): TimeEntryEntity?

    @Query("SELECT * FROM time_entries WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): TimeEntryEntity?

    @Update
    suspend fun update(entry: TimeEntryEntity)

    @Query("DELETE FROM time_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM time_entries")
    suspend fun deleteAll()
}
```

#### SyncQueueDao (`Daos.kt:81-107`)

```kotlin
@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun getAll(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllSync(): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getCountSync(): Int

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}
```

#### RunningTimerDao (`Daos.kt:112-129`)

```kotlin
@Dao
interface RunningTimerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timer: RunningTimerEntity)

    @Query("SELECT * FROM running_timers")
    fun getAll(): Flow<List<RunningTimerEntity>>

    @Query("SELECT * FROM running_timers LIMIT 1")
    suspend fun getActive(): RunningTimerEntity?

    @Query("DELETE FROM running_timers")
    suspend fun deleteAll()

    @Query("DELETE FROM running_timers WHERE id = :id")
    suspend fun deleteById(id: Int)
}
```

---

### Type Converters (`Converters.kt`)

```kotlin
class Converters {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSyncOperation(operation: SyncOperation): String = operation.name

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation = SyncOperation.valueOf(value)
}
```

---

## 7. API Integration

### Odoo XML-RPC Protocol

**Library:** Apache XML-RPC Client (`org.apache.xmlrpc:xmlrpc-client:3.1.3`)

**Endpoints:**
- Authentication: `{SERVER_URL}/xmlrpc/2/common`
- Operations: `{SERVER_URL}/xmlrpc/2/object`

### OdooApiClient (`OdooApiClient.kt`)

**Singleton object** that handles all Odoo API communication.

#### Authentication

```kotlin
suspend fun authenticate(): Int? = withContext(Dispatchers.IO) {
    val config = XmlRpcClientConfigImpl().apply {
        serverURL = URL("${OdooConfig.SERVER_URL}/xmlrpc/2/common")
    }
    val client = XmlRpcClient()
    client.setConfig(config)

    val result = client.execute(
        "authenticate",
        listOf(
            OdooConfig.DATABASE,
            OdooConfig.USERNAME,
            OdooConfig.API_KEY,
            emptyMap<String, Any>()
        )
    ) as Int

    userId = result
    result
}
```

---

### API Methods

| Method | Model | Operation | Parameters | Returns |
|--------|-------|-----------|------------|---------|
| `fetchProjects()` | `timetrack.project` | `search_read` | domain: `[["active", "=", true]]` | `List<Map<String, Any>>` |
| `fetchTimeEntries()` | `timetrack.entry` | `search_read` | optional date filter | `List<Map<String, Any>>` |
| `createProject(name)` | `timetrack.project` | `create` | `{name, active: true}` | `Int` (new ID) |
| `createEntry(projectId, description?)` | `timetrack.entry` | `create` | `{project_id, date, description?}` | `Int` (new ID) |
| `createEntryWithTime(...)` | `timetrack.entry` | `create` | full entry data | `Int` (new ID) |
| `startTimer(entryId)` | `timetrack.entry` | `write` | `{start_time, end_time: false, date}` | `Boolean` |
| `stopTimer(entryId)` | `timetrack.entry` | `write` | `{end_time, duration_minutes}` | `Boolean` |
| `stopTimerWithPause(entryId, pausedSeconds)` | `timetrack.entry` | `write` | adjusted duration | `Boolean` |
| `updateTimeEntry(...)` | `timetrack.entry` | `write` | entry fields | `Boolean` |
| `deleteTimeEntry(id)` | `timetrack.entry` | `unlink` | `[[id]]` | `Boolean` |

---

### API Response Mapping (`Mappers.kt`)

#### Project Mapping

```kotlin
fun Map<String, Any>.toProjectEntity(): ProjectEntity {
    return ProjectEntity(
        id = (this["id"] as Number).toInt(),
        name = this["name"]?.toString() ?: "",
        code = null,
        active = true,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}
```

#### Time Entry Mapping

```kotlin
fun Map<String, Any>.toTimeEntryEntity(): TimeEntryEntity {
    val projectIdField = this["project_id"]

    // Odoo returns project_id as [id, "name"] array or false
    val (projectId, projectName) = when (projectIdField) {
        is Array<*> -> Pair(
            (projectIdField[0] as? Number)?.toInt() ?: 0,
            projectIdField.getOrNull(1)?.toString() ?: ""
        )
        is List<*> -> Pair(
            (projectIdField.getOrNull(0) as? Number)?.toInt() ?: 0,
            projectIdField.getOrNull(1)?.toString() ?: ""
        )
        is Number -> Pair(projectIdField.toInt(), "")
        else -> Pair(0, "")
    }

    return TimeEntryEntity(
        id = (this["id"] as Number).toInt(),
        projectId = projectId,
        projectName = projectName,
        description = this["description"]?.toString()?.takeIf { it != "false" } ?: "",
        date = this["date"]?.toString() ?: "",
        unitAmount = (this["duration_minutes"] as? Number)?.toFloat() ?: 0f,
        startTime = this["start_time"]?.toString()?.takeIf { it != "false" },
        endTime = this["end_time"]?.toString()?.takeIf { it != "false" },
        isRunning = this["is_running"] == true,
        syncStatus = SyncStatus.SYNCED,
        localId = null,
        lastSyncedAt = System.currentTimeMillis()
    )
}
```

---

## 8. Offline-First Architecture

### Design Principles

1. **Local First:** All data comes from local database
2. **Background Refresh:** API data updates local cache
3. **Optimistic UI:** Local changes appear immediately
4. **Sync Queue:** Failed operations are queued for retry

### Sync Status Tracking

```
┌─────────────────────────────────────────────────────────────────┐
│                      RECORD LIFECYCLE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Online Create]                                                │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────┐                                                    │
│  │ SYNCED  │ ──────────────────────────────────────────────────►│
│  └─────────┘                                                    │
│                                                                 │
│  [Offline Create]                                               │
│       │                                                         │
│       ▼                                                         │
│  ┌──────────────┐    Sync Success    ┌─────────┐               │
│  │ PENDING_SYNC │ ──────────────────>│ SYNCED  │               │
│  └──────────────┘                    └─────────┘               │
│       │                                                         │
│       │ Sync Failure                                            │
│       ▼                                                         │
│  ┌─────────────┐    Retry Success    ┌─────────┐               │
│  │ SYNC_FAILED │ ───────────────────>│ SYNCED  │               │
│  └─────────────┘                     └─────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Negative ID Strategy

Offline-created records use negative IDs to avoid conflicts:

```kotlin
// Generate negative local ID
val localId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()

// On sync success: delete local, insert with server ID
if (serverId != null) {
    projectDao.deleteById(localId)
    projectDao.insert(ProjectEntity(id = serverId, ...))
}
```

### Sync Queue Processing

**SyncPayloads.kt** defines JSON payloads:

```kotlin
data class ProjectPayload(val name: String)

data class TimeEntryPayload(
    val projectId: Int,
    val description: String?
)

data class TimerPayload(
    val entryId: Int,
    val action: String,  // "start" or "stop"
    val pausedSeconds: Long = 0
)

data class TimeEntryWithTimePayload(
    val projectId: Int,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val description: String?
)
```

### Repository Pattern (`TimeTrackRepository.kt`)

**Key Methods:**

```kotlin
class TimeTrackRepository(
    private val database: TimeTrackDatabase,
    private val networkMonitor: NetworkMonitor
) {
    // === READ (always from local) ===
    fun getProjects(): Flow<List<ProjectEntity>>
    fun getTimeEntries(): Flow<List<TimeEntryEntity>>

    // === REFRESH (API -> local) ===
    suspend fun refreshProjects()
    suspend fun refreshTimeEntries(todayOnly: Boolean = false)

    // === WRITE (try online, fallback offline) ===
    suspend fun createProject(name: String): Int?
    suspend fun createTimeEntry(projectId: Int, description: String?): Int?
    suspend fun createTimeEntryWithTime(...): Int?
    suspend fun startTimer(entryId: Int): Boolean
    suspend fun stopTimer(entryId: Int, pausedSeconds: Long): Boolean

    // === SYNC ===
    suspend fun syncPendingChanges()
    fun getPendingSyncCount(): Flow<Int>
}
```

**Write Operation Flow:**

```kotlin
suspend fun createProject(name: String): Int? {
    // 1. Try online first
    if (networkMonitor.isOnlineNow()) {
        if (!OdooApiClient.isAuthenticated) {
            OdooApiClient.authenticate()
        }
        if (OdooApiClient.isAuthenticated) {
            val id = OdooApiClient.createProject(name)
            if (id != null) {
                projectDao.insert(ProjectEntity(id, name, null, true, SyncStatus.SYNCED))
                return id
            }
        }
    }

    // 2. Offline fallback: negative ID + sync queue
    val localId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    projectDao.insert(ProjectEntity(
        id = localId,
        name = name,
        code = null,
        active = true,
        syncStatus = SyncStatus.PENDING_SYNC
    ))

    syncQueueDao.insert(SyncQueueEntity(
        entityType = "project",
        entityId = localId.toString(),
        operation = SyncOperation.CREATE,
        payload = gson.toJson(ProjectPayload(name))
    ))

    return localId
}
```

### WorkManager Integration

**SyncManager.kt:**

```kotlin
class SyncManager(private val context: Context) {
    companion object {
        private const val SYNC_WORK_NAME = "timetrack_sync"
        private const val SYNC_NOW_WORK_NAME = "timetrack_sync_now"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun syncNow() {
        // One-time sync request
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_NOW_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .build()
        )
    }
}
```

**SyncWorker.kt:**

```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TimeTrackApplication

        return try {
            app.repository.syncPendingChanges()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
```

### Network Monitoring (`NetworkMonitor.kt`)

```kotlin
class NetworkMonitor(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var onNetworkAvailable: (() -> Unit)? = null

    fun setOnNetworkAvailableListener(listener: () -> Unit) {
        onNetworkAvailable = listener
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            onNetworkAvailable?.invoke()  // Trigger sync
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet && isValidated
        }
    }

    fun register() { /* Register callback */ }
    fun unregister() { /* Unregister callback */ }
    fun isOnlineNow(): Boolean { /* Synchronous check */ }
}
```

### Auto-Sync on Reconnection

Configured in `TimeTrackApplication.onCreate()`:

```kotlin
override fun onCreate() {
    super.onCreate()

    // Register network monitor
    networkMonitor.register()

    // Trigger sync when network becomes available
    networkMonitor.setOnNetworkAvailableListener {
        syncManager.syncNow()
    }

    // Schedule periodic background sync
    syncManager.schedulePeriodicSync()
}
```

---

## 9. UI Components

### Adapters

#### EntriesAdapter (`EntriesAdapter.kt`)

**Purpose:** Display time entries with live timer updates and inline controls.

**Features:**
- Live duration updates for running entries (1-second interval)
- Pause tracking with accumulated paused time
- Start/Pause/Resume/Stop buttons
- Status badges (RUNNING/PAUSED)

**Key Implementation:**

```kotlin
class EntriesAdapter(
    private val entries: List<TimeEntryEntity>,
    private val onStartTimer: (entryId: Int) -> Unit,
    private val onPauseTimer: (entryId: Int) -> Unit,
    private val onResumeTimer: (entryId: Int) -> Unit,
    private val onStopTimer: (entryId: Int, pausedSeconds: Long) -> Unit
) : RecyclerView.Adapter<EntriesAdapter.ViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())
    private val runningTimers = mutableMapOf<Int, Runnable>()
    private val pausedEntries = mutableMapOf<Int, LocalDateTime>()
    private val totalPausedSeconds = mutableMapOf<Int, Long>()

    // Live timer updates every 1 second
    private fun startLiveTimer(holder: ViewHolder, entryId: Int, startTimeStr: String) {
        stopLiveTimer(entryId)
        val runnable = object : Runnable {
            override fun run() {
                val totalSeconds = Duration.between(startTime, now).seconds - pausedSeconds
                holder.entryDuration.text = formatDuration((totalSeconds / 60).toInt()) + " (live)"
                handler.postDelayed(this, 1000)
            }
        }
        runningTimers[entryId] = runnable
        handler.post(runnable)
    }

    fun cleanup() {
        runningTimers.values.forEach { handler.removeCallbacks(it) }
        runningTimers.clear()
        pausedEntries.clear()
        totalPausedSeconds.clear()
    }
}
```

#### ProjectsAdapter (`ProjectsAdapter.kt`)

**Purpose:** Simple project list display.

```kotlin
class ProjectsAdapter(private val projects: List<ProjectEntity>) :
    RecyclerView.Adapter<ProjectsAdapter.ViewHolder>() {

    // Displays: project name, project ID
}
```

#### LapTimesAdapter (`LapTimesAdapter.kt`)

**Purpose:** Display stopwatch lap times.

```kotlin
data class LapTime(
    val lapNumber: Int,
    val lapDuration: Long,  // Duration since last lap
    val totalTime: Long     // Total elapsed time
)

class LapTimesAdapter(private val lapTimes: List<LapTime>) :
    RecyclerView.Adapter<LapTimesAdapter.ViewHolder>() {

    // Displays: "Lap X", lap duration (MM:SS.cc), total time
}
```

### Layouts

| Layout File | Purpose |
|-------------|---------|
| `activity_host.xml` | Main activity with NavHostFragment + BottomNavigationView |
| `fragment_stopwatch.xml` | Stopwatch display + buttons + lap RecyclerView |
| `fragment_timer.xml` | NumberPickers + timer display + buttons |
| `fragment_projects.xml` | Projects RecyclerView + FAB |
| `fragment_entries.xml` | Entries RecyclerView + FAB |
| `item_entry.xml` | Entry list item with timer controls |
| `item_project.xml` | Project list item |
| `item_lap_time.xml` | Lap time list item |
| `dialog_create_entry.xml` | Create entry dialog (spinner + description) |
| `dialog_add_timer_entry.xml` | Timer completion dialog (duration + spinner + description) |

### Navigation

**nav_graph.xml:**

```xml
<navigation app:startDestination="@id/stopwatchFragment">
    <fragment android:id="@+id/stopwatchFragment" android:name="...StopwatchFragment" />
    <fragment android:id="@+id/timerFragment" android:name="...TimerFragment" />
    <fragment android:id="@+id/projectsFragment" android:name="...ProjectsFragment" />
    <fragment android:id="@+id/entriesFragment" android:name="...EntriesFragment" />
</navigation>
```

**bottom_nav_menu.xml:**

```xml
<menu>
    <item android:id="@+id/projectsFragment" android:title="Projects" />
    <item android:id="@+id/entriesFragment" android:title="Entries" />
    <item android:id="@+id/stopwatchFragment" android:title="Stopwatch" />
    <item android:id="@+id/timerFragment" android:title="Timer" />
</menu>
```

---

## 10. State Management

### StateFlow for Reactive UI

**Pattern Used:**

```kotlin
// In ViewModel
val projects: StateFlow<List<ProjectEntity>> = repository.getProjects()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

// In Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.projects.collect { projects ->
        // Update UI
    }
}
```

### SharedFlow for One-Time Events

**Pattern Used:**

```kotlin
// In ViewModel
private val _error = MutableSharedFlow<String>()
val error: SharedFlow<String> = _error.asSharedFlow()

// Emit error
viewModelScope.launch {
    _error.emit("Error message")
}

// In Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.error.collect { error ->
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }
}
```

### Handler-Based Timer Updates

**Stopwatch:** 10ms interval for centisecond display
```kotlin
handler.postDelayed(runnable, 10)
```

**Countdown Timer:** Uses `CountDownTimer` with 1000ms interval
```kotlin
countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
    override fun onTick(millisUntilFinished: Long) { ... }
    override fun onFinish() { ... }
}.start()
```

**Entry Live Timer:** 1000ms interval
```kotlin
handler.postDelayed(runnable, 1000)
```

### ViewModel State Preservation

ViewModels survive configuration changes. State is stored in simple properties:

```kotlin
class TimerViewModel : ViewModel() {
    var timerState: TimerState = TimerState.IDLE
    var remainingMillis: Long = 0
    var totalMillis: Long = 0
    var timerEndTimeMillis: Long = 0
}
```

On fragment recreation, state is restored from ViewModel and timer is recalculated:

```kotlin
if (viewModel.timerState == TimerState.RUNNING) {
    val now = SystemClock.elapsedRealtime()
    viewModel.remainingMillis = maxOf(0, viewModel.timerEndTimeMillis - now)
    if (viewModel.remainingMillis > 0) {
        startCountDown()
    } else {
        resetTimer()
    }
}
```

---

## 11. Configuration

### OdooConfig (`OdooConfig.kt`)

```kotlin
object OdooConfig {
    const val SERVER_URL = "https://stage.tvo-oil.com/"
    const val DATABASE = "stagetvo"
    const val USERNAME = "admin"
    const val API_KEY = "fd6f6632bf8d6f7f43709d227e41a3d8f18851a7"
}
```

> **Note:** Credentials are hardcoded. See [Known Limitations](#13-known-limitations--future-considerations).

### AndroidManifest Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Application Configuration

```xml
<application
    android:name=".TimeTrackApplication"
    android:allowBackup="true"
    android:usesCleartextTraffic="true"
    android:theme="@style/Theme.Timetrack">

    <activity
        android:name=".HostActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

---

## 12. Testing

### Unit Tests

**Location:** `app/src/test/java/com/yaxer/timetrack/`

| Test Class | Target | Test Count |
|------------|--------|------------|
| `DurationFormatterTest` | `DurationFormatter` utility | 17 tests |
| `TimerDurationCalculatorTest` | `TimerDurationCalculator` utility | 13 tests |
| `TimerViewModelTest` | `TimerViewModel` state management | 16 tests |
| `TimerBoundaryTest` | Timer boundary conditions | Various |
| `OdooApiClientTest` | API data formatting logic | 21 tests |

#### DurationFormatterTest Examples

```kotlin
@Test
fun `formatDuration - 90 minutes shows 1 hr 30 min`() {
    val result = DurationFormatter.formatDuration(90)
    assertEquals("Duration: 1 hr 30 min", result)
}

@Test
fun `formatTimerDisplay - 1 hour 30 minutes 45 seconds`() {
    val millis = (1 * 3600 + 30 * 60 + 45) * 1000L
    val result = DurationFormatter.formatTimerDisplay(millis)
    assertEquals("01:30:45", result)
}
```

#### TimerViewModelTest Examples

```kotlin
@Test
fun `initial state is IDLE`() {
    assertEquals(TimerState.IDLE, viewModel.timerState)
}

@Test
fun `full state cycle - IDLE to RUNNING to PAUSED to RUNNING to IDLE`() {
    assertEquals(TimerState.IDLE, viewModel.timerState)
    viewModel.timerState = TimerState.RUNNING
    assertEquals(TimerState.RUNNING, viewModel.timerState)
    viewModel.timerState = TimerState.PAUSED
    assertEquals(TimerState.PAUSED, viewModel.timerState)
    viewModel.timerState = TimerState.RUNNING
    assertEquals(TimerState.RUNNING, viewModel.timerState)
    viewModel.timerState = TimerState.IDLE
    assertEquals(TimerState.IDLE, viewModel.timerState)
}
```

### UI Tests (Instrumented)

**Location:** `app/src/androidTest/java/com/yaxer/timetrack/`

| Test Class | Target | Description |
|------------|--------|-------------|
| `AddEntryDialogUITest` | Timer completion dialog | Dialog UI verification |
| `TimerFragmentUITest` | Timer screen | Initial state, button visibility |
| `TimerStateTransitionUITest` | Timer state changes | State transition verification |

#### TimerFragmentUITest Examples

```kotlin
@Test
fun timerCard_isVisible_onLaunch() {
    onView(withId(R.id.timerCard))
        .check(matches(isDisplayed()))
}

@Test
fun pickerContainer_isVisible_whenIdle() {
    onView(withId(R.id.pickerContainer))
        .check(matches(isDisplayed()))
}

@Test
fun runningButtons_notVisible_whenIdle() {
    onView(withId(R.id.runningButtons))
        .check(matches(not(isDisplayed())))
}
```

### Testing Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| JUnit | 4.13.2 | Unit testing framework |
| MockK | 1.13.5 | Kotlin mocking library |
| Turbine | 1.1.0 | Flow testing |
| Coroutines Test | 1.7.3 | Coroutine testing |
| Espresso | 3.5.1 | UI testing |
| AndroidX Test | 1.5.x | Android test infrastructure |

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests "com.yaxer.timetrack.timer.DurationFormatterTest"
```

---

## 13. Known Limitations & Future Considerations

### Security Concerns

| Issue | Location | Risk | Recommendation |
|-------|----------|------|----------------|
| Hardcoded credentials | `OdooConfig.kt` | HIGH | Use Android Keystore or encrypted SharedPreferences |
| Cleartext traffic enabled | `AndroidManifest.xml` | MEDIUM | Configure network security config for production |
| API key in source | `OdooConfig.kt` | HIGH | Use BuildConfig or environment variables |

### Database Limitations

| Issue | Impact | Recommendation |
|-------|--------|----------------|
| Destructive migration | Data loss on schema change | Implement proper Room migrations |
| No foreign key constraints | Data integrity | Add @ForeignKey to entities |
| No indices on frequently queried columns | Performance | Add @Index annotations |

### Architecture Limitations

| Issue | Impact | Recommendation |
|-------|--------|----------------|
| Manual DI | Verbose, hard to test | Consider Hilt/Dagger |
| No error boundaries | Crash on unhandled exceptions | Add global error handling |
| Simple ViewModel state | No SavedStateHandle | Implement for process death |

### Feature Limitations

| Feature | Current State | Recommendation |
|---------|---------------|----------------|
| Lap times | Not persisted | Save to Room database |
| Timer state | Only in ViewModel | Persist with RunningTimerEntity |
| Offline indicator | Not visible to user | Add UI indicator |
| Sync status | Not shown per-item | Add visual badges |

### Performance Considerations

| Area | Current State | Recommendation |
|------|---------------|----------------|
| Entry list | Full list in memory | Implement Paging 3 |
| Timer updates | Handler-based | Consider WorkManager for background |
| API calls | Main thread blocking possible | Ensure all on IO dispatcher |

### Future Enhancements

1. **Authentication**
   - Implement login screen
   - Store credentials securely
   - Handle session expiration

2. **Notifications**
   - Timer completion notifications
   - Sync status notifications
   - Running timer persistent notification

3. **Data Export**
   - Export entries to CSV
   - Share reports

4. **UI Improvements**
   - Dark mode support
   - Entry editing
   - Entry deletion
   - Project filtering

5. **Testing**
   - Integration tests with mock server
   - Repository unit tests
   - Higher UI test coverage

---

## Quick Reference

### Key File Paths

```
Application Entry:     app/src/main/java/com/yaxer/timetrack/TimeTrackApplication.kt
Main Activity:         app/src/main/java/com/yaxer/timetrack/HostActivity.kt
Database:              app/src/main/java/com/yaxer/timetrack/data/local/TimeTrackDatabase.kt
Repository:            app/src/main/java/com/yaxer/timetrack/data/repository/TimeTrackRepository.kt
API Client:            app/src/main/java/com/yaxer/timetrack/OdooApiClient.kt
Config:                app/src/main/java/com/yaxer/timetrack/OdooConfig.kt
Sync Worker:           app/src/main/java/com/yaxer/timetrack/sync/SyncWorker.kt
Navigation Graph:      app/src/main/res/navigation/nav_graph.xml
Build Config:          app/build.gradle.kts
```

### Common Commands

```bash
# Build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean assembleDebug

# Generate Room schemas
./gradlew kspDebugKotlin
```

### Architecture Decision Records

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Database | Room | Official Android persistence library |
| DI | Manual | Simplicity, no external dependencies |
| Navigation | Jetpack Navigation | Official, type-safe navigation |
| Async | Coroutines + Flow | Modern, first-class Kotlin support |
| API Protocol | XML-RPC | Odoo native protocol |
| Background Sync | WorkManager | Battery-efficient, constraint-aware |

---

*This documentation is intended to provide complete context for AI agents and developers working with the TimeTrack codebase.*
