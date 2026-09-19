# Navigation & Task Flow

## Task Creation Flow
Task creation is deferred until the target activity handles it (using `EXTRA_CREATE_NEW`), avoiding visual glitches in the calling UI (e.g. empty items briefly appearing in a list before the editor opens).

## Navigation Flow
- When opening a task for editing from an external source (e.g. a popup), use `exitOnBack = true` so the user returns to the previous context when finishing or going back.
- **Unique page stack:** for tab/pager-based navigation, the back stack only contains unique pages. When pushing a page that already exists, remove its oldest occurrence — the back button then cycles through distinct recent locations instead of growing unbounded.
- **Tab state reset:** screens like `CalendarScreen` reset local view state (e.g. selected month) to default ("Today") when the user navigates away from the tab.
  - **Swiping:** use `pagerState.settledPage` so the reset only happens once the transition completes and the user releases the screen (avoids jumps during "peeking").
  - **Button navigation:** use a manual trigger in `onTabClick` so the reset happens immediately even if the user navigates back before the animation settles.

## List Interaction Pattern
In list components (e.g. `NotesList`), use a `getDefaultNoteClickHandler` pattern to automatically determine whether `exitOnBack` should be true, based on context.

## DRY / Centralization
Navigation and task-creation logic is centralized in `MainActivity.companion`. Use `MainActivity.openTask()` and `MainActivity.createNewTask()` for consistent behavior across the app and popups.
