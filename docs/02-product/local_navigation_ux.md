# Local Navigation UX — Planned Product Contract

Status: planned; this document does not claim that the UI is implemented.
Execution: [task backlog](../06-roadmap/local_usability_plan.md).
Preserve the [capture flow](capture-flow.md) and
[domain terminology](../03-architecture/domain-model.md).

## Global destinations

- `Início`: hero banner (cover art with the title and one `Nova vistoria`
  action), followed directly by
  `Meus imóveis` cards. No stat cards. Each card shows the cover photo
  (or placeholder art), nickname/address with pin, rooms + status chips
  and `Continuar` for an open draft; the `X imóveis >` header opens `Imóveis`.
  Creation still follows the existing address, Tenancy and manually named
  Room flow.
- `Imóveis`: local Property list, address/nickname, create/rename actions,
  and a Property detail showing its Inspections. Removal follows the
  [Property removal contract](../03-architecture/local_property_removal.md).
- `Vistorias`: local Inspections grouped by Property, with clearly labeled
  `Em andamento` and `Selada` filters. DRAFT resumes capture/review;
  SEALED opens read-only review. Archived Properties remain discoverable
  through an explicit filter and label.
- `Relatórios`: locally finalized Reports, grouped by Property, with
  open/share/verify actions. SEALED Inspections awaiting a PDF expose
  `Gerar PDF`, without claiming a Report already exists.

Each destination works without signup, API, internet or payment. Empty
lists explain the next real action; loading and errors are distinct states.
No new domain entity is introduced by a destination or filter.

## Header and navigation bar

- One shared header on every app screen, including Home: short page title,
  and Property nickname/address plus Room context when applicable.
- Global screens have no redundant Back button. Detail screens have one
  labeled Back control; device Back/swipe follows the same hierarchy.
- Compact windows use a bottom bar with four icons and always-visible
  labels: `Início`, `Imóveis`, `Vistorias`, `Relatórios`. Selected state is
  conveyed by semantics, shape and text as well as green.
- Medium/expanded windows use a navigation rail with the same destinations,
  based on existing `vault.nav` window classes. No additional breakpoint.
- Re-selecting a global destination returns to its root; switching tabs
  does not repeatedly push routes onto the detail back stack.
- Keep the global controls available during capture with compact chrome;
  shutter, preview and checklist retain the primary action area. Give
  `Cômodos` and `Revisão` direct contextual shortcuts for that Inspection.
- A global destination is reachable with one tap from any app detail;
  returning to the Property or Room list does not require repeated Back.
- Apply Scaffold/system/gesture/keyboard insets once. A keyboard must not
  obscure a focused field or force controls over the capture surface.

## State preservation and interruption

Navigation never substitutes for saving. Persist captures before showing
local success; preserve form input, Finding notes, selected Room and list
position when changing destinations. Persist resumable progress outside
domain tables; restore routes by stable IDs and revalidate their existence
and Inspection status after process death. Do not trust stored Room labels.

During an atomic seal or capture commit, prevent duplicate writes and keep
a visible busy state. During long PDF work, users can visit global screens;
progress/result belongs to the Inspection, and completion must not navigate
away from the destination the user selected. Cancelling native capture or
share leaves already committed records intact.

Remove screen-level full-width `Voltar` buttons after the shared header,
global bar and contextual shortcuts cover their actions. Keep one prominent
task action per screen; navigation controls are secondary, consistent chrome.

## Light theme and accessibility

- Light background, surface and header: pure white `#FFFFFF`.
- Primary actions/selected destination: existing green `#1B7A4D`, with
  white on-primary text. Selected containers: pale green `#DDF2E5`.
- Neutral secondary surfaces: `#F3F4F6`; primary text `#1A1A1A`, secondary
  text `#4B5563`. Replace beige/yellow light surface roles, including all
  Material container roles used by bars, cards, dialogs and sheets.
- Screens consume theme roles; define light tokens independently from
  dark roles so replacing shared beige constants cannot recolor dark mode.
  Retain system light/dark selection and semantic error colors.
- Use 48 dp minimum touch targets, readable text at 200% scale, explicit
  field labels, visible keyboard focus and logical screen-reader order.
- Specify normal text contrast of at least 4.5:1 and required non-text
  indicators of at least 3:1. Verify rendered state combinations, including
  errors, focus and selection; white alone does not guarantee accessibility.
- Announce save/removal/error results, label Property menus with the
  Property name, and restore focus after dialogs. Decorative icons do not
  add duplicate announcements. Do not communicate state through color alone.

These are product acceptance targets, not a certification claim. References:
[W3C text contrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html),
[W3C non-text contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html),
[Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
