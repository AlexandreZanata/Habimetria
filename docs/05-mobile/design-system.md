# Design System — Material 3 Tokens and Components (L2)

Code: `vault.ui.Theme` (roles, type, shapes), `vault.ui.Strings`
(pt-BR copy), `vault.ui.VaultComponents` (shared building blocks).

Next requested changes (planned, not implemented):
[local navigation UX](../02-product/local_navigation_ux.md) defines a
white/green light palette, contextual header and bar/rail destinations;
[U01–U12](../06-roadmap/local_usability_plan.md) owns implementation gates.

## Tokens

- Seal-green brand cue on `primary`; full light/dark roles, no
  hardcoded colors in screens. Dark follows the system setting.
- System font only (zero font deps, offline-safe).
- Shapes: 4/8/12/16/28 rounded scale for chips, cards and sheets.

## Components

- `ScreenColumn`: the single content wrapper (spacing + padding once).
- `ScreenTitle`, `BodyText`, `ErrorText` (error role, never bare red).
- `VaultButton`: full-width, 48 dp minimum touch target.
- `ProgressStatus`, `VaultTextField`.

## Adaptive shell

- Breakpoints live in shared `vault.nav` (`windowClassFor`,
  `maxContentWidthDp`, `roomsColumns`, `useSideBySide`) so Android,
  iOS, tablets, foldables and multi-window behave identically.
- `VaultShell` caps content width on medium/expanded windows.
- Phones stack preview above controls; wide windows place them side
  by side with an independently scrolling control column, so no
  action hides in landscape.
- Rooms list grows from 1 to 3 grid columns with window width.

## Rules

- pt-BR strings live in `Strings`; no user-facing literal inline.
- "Salvo neste dispositivo" means local durability only.
- Minimum touch target 48 dp; images carry content descriptions;
  form screens stay usable with keyboard open and large text.
- Insets and navigation shell: see L2.2/L2.3 (`VaultShell`,
  `VaultNavigator`, window classes).
