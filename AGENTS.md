# AGENTS.md — Working Guidelines

Behavioral rules for working on this codebase. For project facts, architecture, and feature specs, see `docs/INDEX.md`.

## Naming
- Use **AppTabs**, never "Destinations" or "Sections".
- Avoid boilerplate names ("Scaffold", "Shell", "Wrapper", "Container") for structural UI unless they're standard library names.
- **No unsolicited renaming** of variables/constants/functions unless explicitly requested.

## Code Style
- **No `GlobalScope`** — always require an explicit, lifecycle-managed `CoroutineScope`.
- **No wildcard/star imports** — list each import explicitly.
- **No premature factorization** — don't create shared helpers/utils unless used in *all* applicable locations immediately.
- **Minimalism** — smallest/most efficient code possible; avoid redundant local state when a direct check on existing data suffices; don't call update/notify methods if underlying data hasn't changed.
- **Centralized config** — always use `AppConfig` for UI constants (colors, padding, dimensions). Add new values there first, never hardcode.
- **DRY** — centralize navigation/task-creation logic in `MainActivity.companion` (`openTask()`, `createNewTask()`).

## Compose & State
- Re-assigning an identical object instance to `MutableState` may not trigger recomposition — ensure a fresh instance or a changed key property (e.g. `lastUpdate`).
- Only trigger expensive save/sync operations when content is actually "dirty" (changed from initial state).
- If a component holds local `mutableStateOf`, sync it back from the source-of-truth prop (e.g. via `LaunchedEffect`) whenever the component isn't in active use, or inferred data-layer changes get missed.
- After saving/updating a task, always refresh local references (e.g. `selectedTask`) from the repository.

## Anti-Patterns / Lessons Learned
- Don't over-optimize repository list updates by checking specific fields (e.g. dates) unless flicker is a *proven* issue — a simple full replacement is safer.
- Never do bulk filesystem I/O (e.g. reading 300+ files) or service setup synchronously on the Main Thread — always use `Dispatchers.IO`.
- `WatchService` echoes writes asynchronously — a `finally` block that resumes watching immediately after a write will usually catch its own echo. Always use a settlement delay matching the debounce window before resuming.

## Scope Discipline
- **Strict file scope** — only touch files explicitly named or clearly targeted. E.g. "add a test case" → only edit the test file, don't touch implementation unless told to "fix"/"implement".
- **Strict aspect scope** — feedback on one aspect (e.g. alignment) must not lead to changes on unrelated aspects (e.g. size).
- **Component isolation** — fix issues local to a screen/sub-panel within that component. Never modify shared/global components (e.g. `TopPanel`) for a local fix.
- **Global component stability** — only change shared components when a global change is explicitly requested; don't "centralize" logic into them if it risks side effects elsewhere.
- **Don't modify `TODO.md`** — reserved for the user's manual tracking.
- **Comment preservation** — never remove/modify/"clean up" existing comments unless explicitly instructed.
- **Data integrity** — include exactly what the user specifies (e.g. in a test snippet), even if it looks unused/redundant.

## Maintaining This Documentation
This project keeps two kinds of memory. New information must go to the right one:

- **`AGENTS.md` (this file)** — *how to behave*: coding conventions, scope-control rules, interaction protocol, anti-patterns. Update this when the user gives a meta-instruction, a forbidden rule, or a correction about *how* to work (see Memory Update Protocol below).
- **`docs/`** — *what the project is*: architecture, feature behavior, data formats, UI/service specs. Update this when the user explains or changes something about *the app itself*.

**Where a doc change goes:**
1. Check `docs/INDEX.md` for an existing file that matches the topic (architecture, obsidian-sync, navigation, ui-styling, services-notifications) and update that file directly.
2. If the change spans an existing file's clear boundary, put it in the file it most concretely belongs to — don't duplicate the same fact across files.
3. If no existing file fits (a genuinely new area, e.g. a new feature module), create a new `docs/<topic>.md` file and add a row/link for it in `docs/INDEX.md`. Never leave a doc file unlinked from the index.
4. Keep `docs/INDEX.md` itself limited to the project overview, key-files table, and the documentation map — it should stay skimmable, not accumulate detail.

**Keep both layers compact:**
- Guidelines in `AGENTS.md` should stay short, imperative rules — not narrative explanations. If a rule needs justification, put the "why" in `docs/` (e.g. under Anti-Patterns/Lessons Learned) and keep the `AGENTS.md` entry to the actionable instruction.
- Docs should describe current behavior/decisions, not conversation history — write updates as if stating a fact about the app, not "the user asked for X".
- When a doc update makes an old rule obsolete (e.g. a policy changes), edit or remove the outdated line rather than appending a contradicting one.

**Timing:** apply the Memory Update Protocol (below) in the same turn the information is given — don't defer documentation to a later pass.

## Interaction Protocol
- **Question vs. task**: a question ("how"/"why"/"where", or ending in "?") is never an implicit request to implement or change something. Answer it first, in text.
- **MANDATORY TEXT-ONLY RESPONSE**: if the user's message is an inquiry, do NOT call `write_file` / `replace_file_content` / `multi_replace_file_content` in that same turn. Only implement in a subsequent turn if explicitly instructed ("implement this", "fix this").
- Always answer direct questions about past actions before treating the message as a new task.
- **Memory Update Protocol (STRICT)**: any meta-instruction, technical restriction, forbidden rule, or behavioral correction from the user must be added to this file (`AGENTS.md`) in the same turn it's given.
- Proactively update this file with meta-learnings/architectural rules at the end of a task, without waiting for a reminder.
