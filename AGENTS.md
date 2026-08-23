# AGENTS.md

## Project

Kotlin Multiplatform application built with **Clean Architecture**, focused on local-first, privacy-focused AI
transcription and note-taking.

Android is the primary platform. iOS support should remain possible without polluting shared business logic with
platform-specific code.

---

## Architecture

Follow strict **Clean Architecture** and the dependency rule:

```text
Presentation (Android: Jetpack Compose / iOS: SwiftUI)
     ↓
Application / Use Cases
     ↓
Domain
     ↑
Data / Infrastructure
```

* Domain must not depend on Android, iOS, Room, Supabase, Compose, SwiftUI, Whisper, or other infrastructure.
* UI & Presentation: Jetpack Compose is used for Android and SwiftUI for iOS, keeping UI platform-specific without
  polluting shared KMP business logic.
* Infrastructure implements domain interfaces.
* Keep business logic in domain/use cases, not UI or infrastructure.
* Prefer small, focused modules with clear responsibilities.
* Keep implementation details `internal` where possible.

---

## Modules

Modules represent **features or architectural responsibilities**, not arbitrary technical utilities.

Examples:

```text
model-manager
transcription
notes
database
analytics
```

Feature modules must communicate through stable public contracts.

Do not access another module's implementation details.

---

## Local First

Local functionality is the foundation of the application.

* Core transcription should work offline.
* Cloud services are optional infrastructure.
* Do not make authentication or network connectivity a requirement for core local functionality.
* Keep user data local unless cloud functionality explicitly requires otherwise.

---

## Model Manager

The Model Manager owns:

* Model metadata.
* Model discovery.
* Model download/delete.
* Download state.
* Model availability.
* Model selection.

Transcription should depend on a model abstraction, **not Model Manager internals**.

---

## Transcription

Whisper/NDK/JNI are implementation details.

```text
Domain API
    ↓
Android implementation
    ↓
JNI / C++
    ↓
Whisper
```

Never expose native/Whisper-specific types to shared domain code.

---

## Data

Repositories expose domain models.

```text
DTO / Entity
    ↓
Mapper
    ↓
Domain Model
```

Do not expose Room entities, DTOs, Supabase models, or network types outside their infrastructure boundaries.

---

## Dependency Injection

Use constructor injection and compose dependencies at application/module boundaries.

Avoid service locators and unnecessary global state.

---

## Testing

Business logic must be testable without Android or infrastructure.

Prioritize tests for:

* Domain rules.
* Use cases.
* Repository behavior.
* Model selection.
* Model lifecycle.
* Critical data mappings.

Prefer fakes over excessive mocking.

---

## Product Principles

Prioritize:

1. Privacy.
2. Offline functionality.
3. Simple UX.
4. Clear product value.
5. Minimal unnecessary complexity.

Challenge feature requests when they add complexity without clear user value.

---

## Agent Rules

Before changing code:

1. Inspect the existing architecture.
2. Identify the correct module/layer.
3. Preserve dependency direction.
4. Make the smallest appropriate change.
5. Verify tests/build where relevant.

**Do not generate code unless explicitly asked.**

When discussing architecture, prefer concise explanations, module diagrams, data-flow diagrams, and trade-offs over
large code examples.

Do not introduce new dependencies, frameworks, or architectural patterns without a clear reason.
