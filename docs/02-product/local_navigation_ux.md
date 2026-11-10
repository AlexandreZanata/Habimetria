# Local Navigation UX — Product Contract

The visual contract applies to the redesigned 0.2.0 mobile app. The manual
acceptance gates below remain user-owned.
Execution: [task backlog](../06-roadmap/local_usability_plan.md).
Preserve the [capture flow](capture-flow.md) and
[domain terminology](../03-architecture/domain-model.md).

## Global destinations

- `Início`: the Habimetria lockup and a local-mode label sit above an
  ivory-and-pine layout. A pine hero pairs the brand signature with a
  decorative architectural elevation and the `Nova vistoria` action.
  `Meus imóveis` follows with a labeled empty state or photo-led cards,
  room counts, status labels and `Continuar` for an open draft. `Ver todos`
  opens `Imóveis`. The Home list composes visible cards on demand.
  Creation follows the existing address, profile and Room setup. A saved
  Property cover takes precedence over the drawn fallback and uses
  proportional center cropping. Property management keeps its denser
  side-by-side cover and metadata layout.
- `Imóveis`: local Property list, address/nickname, create/rename actions,
  and a Property detail showing its Inspections. Removal follows the
  [Property removal contract](../03-architecture/local_property_removal.md).
  Cards use ivory surfaces, soft corners and a fine outline. Search is
  a neutral rounded field with a visible label, search icon, clear action
  and search-keyboard submit. Ativos/Arquivados use a full-width segmented
  control with theme-green selection, a check icon and selected semantics.
  Novo imóvel is the primary green action before the list; it expands the
  existing address form in an outlined card, with a secondary Fechar cadastro
  toggle. Field values stay saveable when the form closes. Creation retains
  the existing create-and-start journey. Abrir imóvel is a labeled action
  inside each Property card, with a divider and chevron. Archived cards
  also retain restoration inside the same card. Loading, retry, search-empty
  and archived-empty states stay distinct; management errors are visible
  even when the creation form is closed. No API or persistence changes.
- `Vistorias`: local Inspections grouped by Property, with clearly labeled
  `Em andamento` and sealed filters. Visible row actions resume DRAFT or
  open the review of a SEALED Inspection;
  SEALED opens read-only review. Archived Properties remain discoverable
  through an explicit filter and label.
- `Relatórios`, reached from `Ajustes`: locally finalized Reports, grouped
  by Property, with open/share/verify actions. SEALED Inspections awaiting a PDF expose
  `Gerar PDF`, without claiming a Report already exists.

Each destination works without signup, API, internet or payment. Empty
lists explain the next real action; loading and errors are distinct states.
No new domain entity is introduced by a destination or filter.

## Header and navigation bar

- One shared masthead on every app screen. Home carries the brand lockup.
  Other screens show a title and Property or Room context when applicable.
  In light mode the masthead uses a pine background with white text and
  icons.
- Global screens have no redundant Back button. Detail screens have one
  labeled Back control; device Back/swipe follows the same hierarchy.
- Compact windows use a bottom bar with four icons and always-visible
  labels: `Início`, `Imóveis`, `Vistorias`, `Ajustes`. Light mode uses a
  pine bar with white icons and labels. The selected destination uses a
  white tile with pine ink and retains selected semantics.
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

- The [design system](../05-mobile/design-system.md) defines ivory
  backgrounds, pine action and title roles, graphite text and sage
  selection. Light and dark roles follow the system setting.
- In light mode Property and Inspection badges use white labels and
  icons on pine. Selected status filters reverse the pair.
- Forms use labeled outlined fields. Room capture highlights the category
  picker and shutter. Draft review and ready Reports use an emphasized
  action panel for sealing and sharing respectively.
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
