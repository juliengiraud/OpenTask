# UI Styling & Interaction

## Shadow & Depth
Use `shadow(elevation = 2.dp)` and `zIndex(0.5f)` on top/sub-top panels for clear visual hierarchy over scrollable content.

## Clipping
Use `.clip(RoundedCornerShape(dp))` on parent containers that have both a background and rounded corners, to prevent child backgrounds (headers/footers) from "bleeding" over the rounded corners.

## Adaptive UI
For components like popups: `heightIn(max = screenHeight * 0.8f)` on the main container, `weight(1f, fill = false)` on scrollable content (e.g. `NotesList`), so the UI adapts to content size instead of taking fixed space.

## Text Styling
Primary action buttons in headers/footers (e.g. "Ajouter" in the popup) use `MaterialTheme.typography.titleLarge` to match header weights.

## AppConfig Color Map
| Token | Value | Usage |
|---|---|---|
| `DefaultBackgroundColor` | `0xFFEEEEEE` | Main app background |
| `TopPanelBackgroundColor` | `0xFFD6D6D6` | Main headers |
| `SubPanelBackgroundColor` | `0xFFE0E0E0` | Sub-headers, popup header/footer backgrounds |
| `AddNoteButtonBackgroundColor` | `0xFF4CAF50` | Standard action green |
| `AddNoteButtonIconColor` | `Color.White` | Usually white |
| `EditorFocusBorderColor` | orange (typical) | Active focus border in editors |

## Window Insets & Stability
To prevent "jumping" headers or black status bars when the keyboard appears:
- Headers use `statusBarsPadding()`.
- Scrollable content uses `imePadding()` and `navigationBarsPadding()` within its own isolated container.
- This prevents the system from trying to "pan" the entire root layout.

## Editor Interaction
- **Focus management:** when opening an editor, if the title is empty (new note) focus the title field; otherwise focus the body content.
- **Empty space interaction:** tapping empty space below the text requests focus and moves the cursor to the end of the content.
- **Visual feedback:** use `AppConfig.EditorFocusBorderColor` for active focus borders.
