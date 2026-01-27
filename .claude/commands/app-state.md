---
description: Generate comprehensive APP_STATE.md documentation for the current codebase
---

## User Input
$ARGUMENTS

## Instructions

You are generating comprehensive codebase documentation. The output path is: `$ARGUMENTS` (use `APP_STATE.md` in the current working directory if not specified).

---

## Phase 1: Project Discovery

First, detect the project type by checking for build files:

1. **Check for build files** (in order of priority):
   - `build.gradle.kts` or `build.gradle` → Android/Kotlin or Java
   - `package.json` → Node.js/JavaScript/TypeScript
   - `pyproject.toml` or `requirements.txt` or `setup.py` → Python
   - `go.mod` → Go
   - `Cargo.toml` → Rust
   - `pom.xml` → Java/Maven
   - `*.csproj` or `*.sln` → .NET/C#
   - `pubspec.yaml` → Flutter/Dart
   - `Gemfile` → Ruby

2. **Identify source directories** based on project type
3. **Locate test directories**
4. **Find configuration files**

---

## Phase 2: Structure Analysis

Generate a comprehensive directory tree:

1. Use `find` or equivalent to map all directories
2. Identify key files and their purposes:
   - Entry points (main, index, App)
   - Configuration files
   - Build scripts
   - Documentation
3. Map package/module organization

---

## Phase 3: Configuration Extraction

Parse build configuration files:

### For Android/Kotlin:
- Read `build.gradle.kts` for:
  - `compileSdk`, `minSdk`, `targetSdk`
  - Dependencies (implementation, testImplementation)
  - Plugins (kotlin, android, room, etc.)
- Read `gradle.properties` for additional config
- Read `settings.gradle.kts` for project name and modules

### For Node.js:
- Read `package.json` for:
  - Name, version, description
  - Dependencies and devDependencies
  - Scripts
- Read `tsconfig.json` if TypeScript

### For Python:
- Read `pyproject.toml` or `requirements.txt` for dependencies
- Check for virtual environment configuration

### For other platforms:
- Extract equivalent configuration from their build files

---

## Phase 4: Architecture Analysis

Detect architecture patterns:

### Android/Kotlin patterns to look for:
- **MVVM**: ViewModel classes, LiveData/StateFlow usage
- **MVP**: Presenter classes
- **Clean Architecture**: domain/, data/, presentation/ layers
- **Repository Pattern**: Repository classes
- **Dependency Injection**: Hilt/Dagger/Koin modules

### Web/Node patterns:
- **MVC**: controllers/, models/, views/
- **Clean Architecture**: domain/, infrastructure/
- **Layered**: services/, repositories/

### General patterns:
- Entry points and bootstrap
- Component relationships
- Data flow direction

---

## Phase 5: Feature Discovery

List all features/screens/routes:

### For Android:
- Parse `AndroidManifest.xml` for Activities
- Find Fragment classes
- Identify Navigation components
- Map menu items and actions

### For Web:
- Find route definitions
- List pages/views/components
- Map API endpoints

### General:
- Document UI components
- Map state management approach
- List user-facing features

---

## Phase 6: Data Layer Analysis

Analyze data persistence and API:

### Database:
- Find Entity/Model classes
- Document table schemas
- List DAO/Repository methods
- Note relationships between entities

### API Integration:
- Find API client classes
- Document endpoints (URL, method, parameters)
- List data transfer objects
- Note authentication approach

### Local Storage:
- SharedPreferences/UserDefaults usage
- File storage patterns
- Caching strategies

---

## Phase 7: Testing Analysis

Discover testing setup:

1. **Find test directories**:
   - `src/test/` (unit tests)
   - `src/androidTest/` (instrumented tests)
   - `__tests__/` or `*.test.js` (JS/TS)
   - `tests/` or `test_*.py` (Python)

2. **Identify testing libraries**:
   - JUnit, Mockito, Espresso (Android)
   - Jest, Mocha, Cypress (JS)
   - pytest, unittest (Python)

3. **Document test coverage areas**

---

## Phase 8: Documentation Generation

Write the APP_STATE.md file with ALL of the following sections:

```markdown
# [Project Name] - Application State Documentation

> Comprehensive codebase documentation for AI agents and developers
> Generated: [DATE]

---

## 1. Executive Summary

### Application Overview
- **Name:** [App name from config]
- **Package/ID:** [Package identifier]
- **Purpose:** [Brief description of what the app does]
- **Current Version:** [Version from config]

### Technology Stack
| Category | Technology | Version |
|----------|------------|---------|
| Language | [e.g., Kotlin] | [version] |
| Framework | [e.g., Android SDK] | [version] |
| Build Tool | [e.g., Gradle] | [version] |
| [Category] | [Tech] | [Version] |

### Architecture Pattern
[Describe the architecture: MVVM, Clean, etc.]

---

## 2. Project Structure

### Directory Tree
```
[Project root]/
├── [dir]/
│   ├── [subdir]/
│   └── [file]
└── ...
```

### Key Directories
| Directory | Purpose |
|-----------|---------|
| [path] | [description] |

### Important Files
| File | Purpose | Location |
|------|---------|----------|
| [filename] | [description] | [path:line] |

---

## 3. Build Configuration

### Build Tool
- **Tool:** [Gradle/npm/pip/etc.]
- **Version:** [version]
- **Config File:** [path]

### Dependencies
| Dependency | Version | Purpose |
|------------|---------|---------|
| [name] | [version] | [what it's used for] |

### Build Commands
```bash
# Build
[build command]

# Test
[test command]

# Run
[run command]
```

---

## 4. Architecture Overview

### Pattern: [Pattern Name]

[Description of the architecture pattern used]

### Data Flow Diagram
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     UI      │────▶│  ViewModel  │────▶│ Repository  │
│  (Fragment) │◀────│  (State)    │◀────│   (Data)    │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                          ┌───────────────────┼───────────────────┐
                          ▼                   ▼                   ▼
                    ┌──────────┐       ┌──────────┐       ┌──────────┐
                    │  Local   │       │  Remote  │       │  Cache   │
                    │    DB    │       │   API    │       │          │
                    └──────────┘       └──────────┘       └──────────┘
```

### Component Relationships
[Describe how components interact]

### Dependency Injection
[Describe DI approach if used]

---

## 5. Features & Screens

### Screen/Feature List
| Screen | Component | Path | Description |
|--------|-----------|------|-------------|
| [Name] | [Class/Component] | [path:line] | [description] |

### Navigation Flow
```
[ASCII diagram of navigation flow]
```

### Feature Details

#### [Feature 1 Name]
- **Location:** [path:line]
- **Components:** [list]
- **Description:** [what it does]

---

## 6. Data Layer

### Database Schema

#### [Entity/Table Name]
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| [name] | [type] | [constraints] | [description] |

**Location:** [path:line]

### Data Access Objects (DAOs)
| DAO | Entity | Key Methods | Location |
|-----|--------|-------------|----------|
| [name] | [entity] | [methods] | [path:line] |

### Repositories
| Repository | Purpose | Data Sources | Location |
|------------|---------|--------------|----------|
| [name] | [purpose] | [sources] | [path:line] |

---

## 7. API Integration

### API Client
- **Implementation:** [class/module name]
- **Location:** [path:line]
- **Protocol:** [REST/GraphQL/gRPC/etc.]

### Endpoints
| Endpoint | Method | Purpose | Parameters |
|----------|--------|---------|------------|
| [url] | [GET/POST/etc.] | [description] | [params] |

### Authentication
[Describe authentication approach]

### Error Handling
[Describe error handling strategy]

---

## 8. State Management

### State Pattern
[Describe state management approach]

### State Holders
| Component | State Type | Observables | Location |
|-----------|------------|-------------|----------|
| [name] | [type] | [LiveData/Flow/etc.] | [path:line] |

### State Flow
```
[ASCII diagram of state flow]
```

---

## 9. UI Components

### Adapters/List Components
| Adapter | Data Type | Layout | Location |
|---------|-----------|--------|----------|
| [name] | [type] | [layout] | [path:line] |

### Layouts/Views
| Layout | Purpose | Used By |
|--------|---------|---------|
| [name] | [purpose] | [components] |

### Reusable Components
| Component | Purpose | Location |
|-----------|---------|----------|
| [name] | [purpose] | [path:line] |

---

## 10. Configuration

### Environment Configuration
| Setting | Value | Location |
|---------|-------|----------|
| [name] | [value] | [path:line] |

### Permissions (if applicable)
| Permission | Purpose |
|------------|---------|
| [permission] | [why needed] |

### Feature Flags
| Flag | Default | Description |
|------|---------|-------------|
| [name] | [value] | [description] |

---

## 11. Testing

### Test Structure
| Type | Directory | Framework |
|------|-----------|-----------|
| Unit | [path] | [framework] |
| Integration | [path] | [framework] |
| E2E/UI | [path] | [framework] |

### Test Files
| Test | Tests For | Location |
|------|-----------|----------|
| [name] | [component] | [path:line] |

### Running Tests
```bash
# Unit tests
[command]

# Integration tests
[command]

# All tests
[command]
```

---

## 12. Known Limitations & Technical Debt

### Security Considerations
- [List any security notes or concerns]

### Technical Debt
- [ ] [Item 1]
- [ ] [Item 2]

### TODOs in Code
| Location | TODO |
|----------|------|
| [path:line] | [description] |

---

## 13. Quick Reference

### Key File Paths
| Purpose | Path |
|---------|------|
| Entry Point | [path] |
| Main Config | [path] |
| Database | [path] |
| API Client | [path] |

### Common Commands
```bash
# Build
[command]

# Run
[command]

# Test
[command]

# Clean
[command]
```

### Code Conventions
- [List naming conventions]
- [List file organization rules]
- [List other conventions]

---

## Appendix: File Index

### Source Files
| File | Purpose | Lines |
|------|---------|-------|
| [path] | [purpose] | [count] |

### Configuration Files
| File | Purpose |
|------|---------|
| [path] | [purpose] |

---

*Documentation generated by `/app-state` skill*
```

---

## Output Instructions

1. **Determine output path**:
   - If `$ARGUMENTS` is provided and not empty, use it as the output path
   - Otherwise, use `APP_STATE.md` in the current working directory

2. **Explore the codebase thoroughly** using all available tools:
   - Use `Glob` to find files by pattern
   - Use `Grep` to search for patterns
   - Use `Read` to examine file contents
   - Use `Bash` for directory listings and git information

3. **Fill in ALL template sections** with actual data from the codebase:
   - Replace ALL placeholders with real values
   - Include actual file paths with line numbers (format: `path/to/file.kt:123`)
   - Generate accurate ASCII diagrams based on actual architecture
   - List real dependencies with versions

4. **Write the complete documentation** to the output path

5. **Report completion** with:
   - Output file path
   - Number of files analyzed
   - Key findings summary

---

## Adaptation Notes

This skill automatically adapts to different project types:

| If you find... | The project is... | Focus on... |
|----------------|-------------------|-------------|
| `build.gradle.kts` + `AndroidManifest.xml` | Android/Kotlin | Activities, Fragments, Room, ViewModels |
| `package.json` + React/Vue/Angular | Frontend Web | Components, Routes, State stores |
| `package.json` + Express/Nest | Backend Node | Controllers, Services, Middleware |
| `pyproject.toml` + Django/FastAPI | Python Web | Views, Models, Serializers |
| `go.mod` | Go | Handlers, Services, Repositories |
| `Cargo.toml` | Rust | Modules, Crates, Traits |

Adjust section content based on what's relevant for the detected project type. Skip sections that don't apply (e.g., skip "Database Schema" for a stateless API).
