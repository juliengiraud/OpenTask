# OpenTask — Project Documentation

A modern, Jetpack Compose-based replacement for **ColorNote**, with **Obsidian** synchronization.

- **Structure:** Single module Android application (`:app`)
- **Tech Stack:** Kotlin, Jetpack Compose, Material3

## Key Files

| File | Purpose |
|---|---|
| `AppConfig.kt` | Centralized UI configuration (colors, etc.) |
| `MainActivity.kt` | Main entry point / navigation controller; task selection & creation flow |
| `PopupActivity.kt` | Adaptive popup for quick task viewing and creation |
| `TaskRepository.kt` | Data management for tasks |
| `AppTabs.kt` | Enum defining the main navigation sections |
| `MainBottomBar.kt` | Bottom navigation component |
| `AddNoteButton.kt` | Primary FAB for note creation |
| `DateUtils.kt` | Localized date formatting helpers (e.g. "Wed. 17 July") |
| `SubTopPanel.kt` | Shared secondary-header component (shadow + z-index) |

## Documentation Map

- [`architecture.md`](./architecture.md) — target package structure, layout structure (Scaffold/bottom bar/FAB), activity lifecycle
- [`obsidian-sync.md`](./obsidian-sync.md) — file watcher, note parsing/model, conflict resolution, YAML handling
- [`navigation.md`](./navigation.md) — task creation flow, back-stack rules, tab state, popup ↔ activity navigation
- [`ui-styling.md`](./ui-styling.md) — shadow/clipping conventions, adaptive layout, AppConfig color map, editor focus/insets behavior
- [`services-notifications.md`](./services-notifications.md) — foreground service, permanent notification behavior

For behavioral rules the assistant must follow when working in this repo, see [`../AGENTS.md`](../AGENTS.md).
