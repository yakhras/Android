<!--
  ============================================================================
  SYNC IMPACT REPORT
  ============================================================================
  Version change: N/A → 1.0.0 (Initial ratification)

  Modified principles: N/A (initial creation)

  Added sections:
    - Core Principles (3 principles)
    - Development Standards
    - Quality Gates
    - Governance

  Removed sections: N/A

  Templates status:
    - .specify/templates/plan-template.md: ✅ compatible (Constitution Check section present)
    - .specify/templates/spec-template.md: ✅ compatible (requirements-driven approach)
    - .specify/templates/tasks-template.md: ✅ compatible (testing gates mentioned)

  Follow-up TODOs: None
  ============================================================================
-->

# TimeTrack Constitution

## Core Principles

### I. Code Quality

All code MUST pass static analysis and linting before merge. This includes:

- Kotlin code MUST compile without warnings (treat warnings as errors in CI)
- Code MUST follow Kotlin coding conventions and Android Kotlin style guide
- No suppression of lint warnings without documented justification in code comments
- All public APIs MUST have KDoc documentation

**Rationale**: Consistent code quality reduces bugs and improves maintainability across
the team and over time.

### II. Testing Discipline

Tests are REQUIRED for business logic and critical paths:

- Unit tests MUST cover service/repository logic
- Instrumented tests SHOULD cover critical user flows
- Test coverage for new code MUST be at minimum 60% for logic classes
- Mocking is permitted but real implementations are preferred where practical

**Rationale**: Tests provide confidence for refactoring and guard against regressions.

### III. Simplicity

Prefer simple, direct solutions over complex abstractions:

- YAGNI (You Aren't Gonna Need It) - do not build for hypothetical future requirements
- Avoid premature abstraction - three similar lines are better than an unnecessary helper
- Dependencies MUST be justified - no library additions without clear benefit over stdlib
- Features MUST be traceable to user requirements in spec.md

**Rationale**: Complexity is the enemy of reliability. Simple code is easier to understand,
test, and maintain.

## Development Standards

### Technology Stack

- **Language**: Kotlin 1.9+ with Coroutines for async operations
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 34 (Android 14)
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: MVVM with Lifecycle components
- **Testing**: JUnit 4 + AndroidX Test + Espresso

### Code Organization

- Feature code resides in `app/src/main/java/com/yaxer/timetrack/`
- Test code mirrors source structure in `app/src/test/` and `app/src/androidTest/`
- Resources follow Android naming conventions (snake_case)

## Quality Gates

All pull requests MUST satisfy these gates before merge:

1. **Build passes**: `./gradlew assembleDebug` succeeds without errors
2. **Lint passes**: `./gradlew lint` reports no errors (warnings acceptable with justification)
3. **Tests pass**: `./gradlew test` and instrumented tests pass
4. **Code review**: At least one approval from a maintainer (when team > 1)

## Governance

### Amendment Process

1. Propose changes via pull request modifying this file
2. Document rationale for change in PR description
3. Obtain approval from project maintainer(s)
4. Update version according to semantic versioning:
   - MAJOR: Principle removal or fundamental redefinition
   - MINOR: New principle or significant expansion
   - PATCH: Clarifications, wording improvements

### Compliance

- This constitution supersedes conflicting informal practices
- All feature implementations MUST reference applicable principles
- Complexity additions require explicit justification in plan.md

**Version**: 1.0.0 | **Ratified**: 2026-01-19 | **Last Amended**: 2026-01-19
