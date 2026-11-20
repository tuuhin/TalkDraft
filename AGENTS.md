# AGENTS.md

## Project and architecture

TalkDraft is a Kotlin Multiplatform application for local-first, privacy-focused AI transcription. Shared Compose UI is
used in the KMP app; Android and iOS also have platform entry points. Do not describe iOS UI as SwiftUI: the inspected
app entry points integrate shared Compose, though platform-specific code exists.

Top-level responsibilities are organized under `core/` (shared infrastructure and utilities), `feature/` (product and
domain features), `presentation/` (shared UI, navigation, and design system), and `app/` (application setup and
composition). `androidApp/` and `iosApp/` are platform launch projects. These are observed groupings, not a mandate to
place every new concern in an identically structured module.

There is domain/data separation in parts of the repository, but layering varies; treat Clean Architecture dependency
direction as a design goal rather than an assertion that every feature has a use-case layer:

```text
Shared UI / ViewModel → feature or domain contracts ← data / platform implementations
```

Keep business rules independent of Android and infrastructure where practical. Repositories should expose domain
models; map persistence or remote representations at their boundary. For example, `feature/model-manager` has
`data/repository/TranscriptionModelRepository.kt`, domain interfaces/models, and data mappers. Do not expose database,
DTO, network, or native transcription types as shared domain contracts.

The app composition uses Koin modules, including `app/src/commonMain/.../di/FeatureModule.kt`; retain constructor
injection and compose dependencies at module/application boundaries. Avoid service locators and unnecessary global
state.

## Code and source-set layout

- `core/` and `feature/` contain independently configured modules; module directories commonly use `src/` source
  sets and responsibility packages such as `domain`, `data`, and `di`.
- Shared Kotlin code commonly lives in `src/commonMain`; platform implementations may live in `src/androidMain` or
  `src/iosMain`. Use the source set supported by the target and module rather than assuming every module has all three.
- `presentation/home` contains `HomeScreen.kt`, `HomeScreenViewmodel.kt`, and `HomeScreenContent.kt`; shared UI
  primitives and theme are in `presentation/design-system`.
- `app/src/commonMain` contains shared app setup, including `App.kt` and DI modules. Platform-specific app code is
  found under `app/src/androidMain` and `app/src/iosMain`; the `androidApp/` and `iosApp/` projects are launch points.
- Tests live alongside modules in their available test source sets. Examples include
  `core/database/src/commonTest/.../LocalDatabaseTest.kt` and Android instrumented tests under `src/androidTest`.
  Some modules also use `androidDeviceTest`; choose the source set configured for the target being tested.

## Local-first and platform boundaries

Preserve offline functionality for core transcription and keep cloud services optional. Do not make authentication or
network connectivity a prerequisite for local functionality; keep user data local unless a feature explicitly
requires cloud behavior.

Model-management features own model metadata, discovery, download/deletion, availability, and selection. Transcription
and other consumers should depend on model contracts rather than model-manager implementation details.

Treat Whisper, NDK, JNI, and native types as implementation details. Keep platform-specific engines behind shared
contracts and do not expose native/Whisper types to shared domain code.

## Navigation

Navigation is implemented with Navigation 3 in `presentation/navigation`. Add app-level destinations as serializable
keys in `NavDestinations` (for example, `RecommendedDownloadModelScreen(val modelId: Uuid)`). A destination module
provides a `NavDestinationBuilder` implementation and overrides `navEntry()`; `RecommendedModelNavEntry` registers
`entry<NavDestinations.RecommendedDownloadModelScreen>`, obtains its `modelId`, and creates the screen/ViewModel.

`AppRootNavHost` collects registered `NavDestinationBuilder`s from Koin and applies each `navEntry()` to its entry
provider. Screens navigate through `AppNavigator`; its `NavCommands` are collected by the root host and update the
back stack (`NavigateTo`, `Pop`, `PopTo`, `ClearAndNavigate`, or `UpdateBackStack`). Use typed destination keys and
parameters rather than inventing string routes. An iOS `IosDeepLinkRouter` accepts incoming URLs, but no URL-to-
destination mapping is established by the inspected navigation code; do not assume deep-link routing behavior.

## Naming patterns

Follow local module conventions, which are not perfectly uniform; these are examples, not universal rules:

- Kotlin source files use descriptive PascalCase names, commonly matching a primary type, e.g.
  `TranscriptionModelRepository.kt` and `RecommendedModelViewmodel.kt`.
- Composable screen functions and destination types use PascalCase, e.g. `HomeScreen()` and
  `NavDestinations.RecommendedDownloadModelScreen(modelId)`; destination parameters are typed (`Uuid` here).
- UI state and event types may use `...State` and `...Event`, e.g. `SelectedModelScreenState` and
  `SelectedModelScreenEvent`.
- Test files commonly end in `Test.kt`, e.g. `LocalDatabaseTest.kt` and `AndroidPlayerTest.kt`. Test source sets and
  method naming vary; preserve the conventions of the module rather than imposing one style.
- `Viewmodel` capitalization occurs in names such as `HomeScreenViewmodel`; do not silently rename existing types to
  enforce a different spelling convention.

## Testing

Keep business logic testable without Android or infrastructure. Prioritize domain rules, repository behavior, model
selection/lifecycle, and critical mappings; prefer fakes where practical. Test source-set availability and naming are
not fully uniform, so follow the module's existing setup rather than assuming one convention.

## Product principles

Prioritize privacy, offline functionality, simple UX, clear product value, and minimal unnecessary complexity.
Challenge requests that add complexity without clear user value.

## Agent rules

Before changing code, inspect the architecture, identify the correct module/layer, preserve dependency direction, make
the smallest appropriate change, and verify tests/build where relevant. Do not generate code unless explicitly asked.
Avoid new dependencies, frameworks, or architectural patterns without a clear reason. When explaining architecture,
prefer concise diagrams and trade-offs over large code examples.
