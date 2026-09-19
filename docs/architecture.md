# Architecture

## Current Structure
Single module Android application (`:app`), Kotlin + Jetpack Compose + Material3.

## Layout Structure
- `MainActivity` uses a standard `Scaffold`.
  - `MainBottomBar` → `bottomBar` slot
  - `AddNoteButton` → `floatingActionButton` slot
  - This ensures list content is correctly padded.

## Activity Lifecycle
- `MainActivity` uses `standard` launch mode (not `singleTop`) to allow transient editor instances to exist on top of `PopupActivity` without finishing the main app instance.
  - Use `exitOnBack = true` for these transient instances.
- `PopupActivity` uses `android:excludeFromRecents="true"` so transient overlay popups don't appear in the system recent-apps history.

## Target Project Structure
Not implemented yet — as the app grows, organize into feature packages:

```md
:root:
- core
  - data
    - db
  - domain
  - presentation
    - util
- feat1
  - data
  - di
    feat1Module => ex. fun provideXRepository
  - domain
    - user
      GetUserUseCase
      User
      UserValidation
  - presentation
    - components
    - usecase1
      - components
      usecase1Screen.tk
      usecase1State
    - usecase2
      - components
      usecase2Screen.tk
      usecase2State
- feat2
  - data
  - domain
  - presentation
- ui
MainActivity.kt
```
