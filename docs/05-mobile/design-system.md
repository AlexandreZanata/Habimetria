# Design System — Material 3 Tokens and Components (L2)

Normative identity: [brand-identity.md](brand-identity.md). Code:
`vault.ui.Theme` (brand roles, Manrope, shapes), `vault.ui.Strings`
(pt-BR copy), `vault.ui.VaultComponents` (shared building blocks),
`vault.ui.brand.HabimetriaBrand` (monogram).

## Brand palette

Base palette is fixed by the brand document: Pinho `#123D35`, Marfim
`#F4F3EE`, Grafite `#28332F`, Sálvia `#A7B8A7`. UI roles below derive
from it; no screen hardcodes a color.

Light roles:

- `primary` Pinho; `onPrimary` Marfim; primary action and headers.
- `primaryContainer` Sálvia; `onPrimaryContainer` Pinho.
- `secondary` Sálvia; `onSecondary` Grafite.
- `background`, `surface`, `surfaceContainerLow/Lowest/High` Marfim and
  ivory tints, including warm white `#FBFAF6` for raised reading surfaces;
  `onBackground`, `onSurface` Grafite.
- `surfaceVariant`, `surfaceDim`, `surfaceContainer/Highest` sage-ivory
  supporting tints; `onSurfaceVariant` Grafite.
- `outline` and `outlineVariant` muted pinho-gray; never pure black.
- `error` `#9C3B2E` with Marfim label.

Dark roles:

- `primary` `#BFD3C4` (light sálvia), `onPrimary` Pinho.
- `primaryContainer` `#1E3A32`, `onPrimaryContainer` `#E8EFE9`.
- `background` `#0E1512`; `surface` and `surfaceContainer` `#15201C`;
  `surfaceVariant` `#1E2C27`; `onBackground`, `onSurface` Marfim.
- Dark tokens are independent constants so light edits never recolor
  dark mode. Dark follows the system setting.

Both schemes use Pinho, Marfim and Sálvia for the inverse feature panels.

## Typography

- **Manrope** is bundled as static 400/500/600/700 weights
  (`composeResources/font`); the system sans is only a fallback.
- Titles use Medium/SemiBold with tight leading; body uses Regular.
- Roles: `displaySmall` 38/42, `headlineMedium` 32/38, `headlineSmall` 24/32, `titleLarge`
  20/28 SemiBold, `titleMedium` 16/24 SemiBold, `bodyLarge` 16/24,
  `bodyMedium` 14/20, `labelLarge` 14/20 Medium.
- Numbers and evidence codes stay tabular where the platform allows.

## Shapes and density

- Rounded scale 4/10/14/20/28 for chips, cards, sheets and the app icon
  language; keep right angles only inside the brand mark.
- 20 dp horizontal screen padding, 12 dp stack spacing, 48 dp minimum
  touch target.
- Cards and buttons use `shape` roles; no per-screen radii.

## Components

- `ScreenColumn`: the single content wrapper (spacing + padding once).
- `ScreenTitle`, `BodyText`, `ErrorText` (error role, never bare red).
- `VaultButton`: full-width, 52 dp minimum height.
- `ProgressStatus`, outlined `VaultTextField`.
- `RecordPanel`: icon and copy for contextual states, with an inverse
  treatment when it carries a primary seal or share action.
- `StatusBadge`: a rounded icon and label for Property and Inspection
  states. Light mode uses white ink on Pinho. Selectable status filters
  reverse this pair for the current selection.
- `HabimetriaMark`: geometric H from the brand asset, tinted by the
  current content color.
- [Architectural elevation](home_artwork.md): decorative drawing for Home
  and Property cover fallbacks.

## Adaptive shell

- In light mode the masthead and compact bottom bar use the same Pinho
  `inverseSurface` as the Home hero, with white (`#FFFFFF`) text
  and icons. The selected bottom-bar tile reverses the pair.
- Breakpoints live in shared `vault.nav` (`windowClassFor`,
  `maxContentWidthDp`, `roomsColumns`, `useSideBySide`) so Android,
  iOS, tablets, foldables and multi-window behave identically.
- `VaultShell` caps content width on medium/expanded windows and places
  the monogram and `titleLarge` title in the same row across all routes.
  Context sits below the title row and Back sits at its trailing edge.
- Phones stack preview above controls; wide windows place them side
  by side with an independently scrolling control column, so no
  action hides in landscape.
- Rooms list grows from 1 to 3 grid columns with window width.

## Rules

- pt-BR strings live in `Strings`; no user-facing literal inline.
- "Salvo neste dispositivo" means local durability only.
- Minimum touch target 48 dp; images carry content descriptions;
  form screens stay usable with keyboard open and large text.
- Never encode state by color alone; pair with text or icon.
- Insets and navigation shell: see L2.2/L2.3 (`VaultShell`,
  `VaultNavigator`, window classes).
