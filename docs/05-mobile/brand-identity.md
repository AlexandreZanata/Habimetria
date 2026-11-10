# Brand Identity — Habimetria

Normative source for the visual identity. UI tokens and components are in
[design-system.md](design-system.md); source assets are in
[assets/](assets/README.md).

## Brand at a glance

- Name: **Habimetria**, always lowercase in the logotype (`habimetria`).
- Signature: **Cada detalhe, documentado.**
- Functional description: **Vistorias e documentação de imóveis.**
- Category: property inspection and documentation.
- Personality: calm, precise, trustworthy, local-first, non-bureaucratic.

## Symbol concept

The symbol is a geometric **H** inspired by a floor plan. The two stems
read as walls; the small central notch reads as a passage opening. The
mark keeps few elements so it stays legible as a phone icon, in report
covers and at small sizes.

Construction rules:

- Trace the supplied square monogram. The small central cutout opens
  upward from the gap between the two stems.
- Keep right angles; no rounded corners inside the mark.
- Use a single flat color. Never add shadows, gradients or outlines.
- Minimum size: 16 dp for the isolated mark, 24 dp beside the wordmark.
- Clear space: at least one crossbar height on every side.

## Wordmark and lockup

The name appears in lowercase with a clean typeface and rounded forms
that balance the geometry of the symbol. Manrope SemiBold is the
reference, with slight negative tracking.

- Lockup order: mark, then wordmark; never stack them off-grid.
- Keep the original spelling and casing: `habimetria`, not `Habimetria`
  or `HABIMETRIA` inside the logo.
- The wordmark may sit beside the mark on light backgrounds or below it
  in square placements (app icon, report stamp).
- Never stretch, outline, recolor outside the palette, or rotate.

## Palette

| Token | Hex | Role |
| --- | --- | --- |
| Verde pinho | `#123D35` | Icon, logotype, primary buttons, headers, report rules |
| Marfim | `#F4F3EE` | App backgrounds, documents, presentation surfaces |
| Grafite | `#28332F` | Body text and reading information |
| Sálvia | `#A7B8A7` | Secondary surfaces, selected containers, supporting detail |

Contrast on the base pairs (WCAG 2.1):

- Grafite on Marfim: 11.78:1 (AAA).
- Pinho on Marfim: 10.83:1 (AAA).
- Marfim on Pinho: 10.83:1 (AAA).
- Grafite on Sálvia: 6.27:1 (AA).
- Pinho on Sálvia: 5.76:1 (AA).

Green is an accent, never the reading color for long body text. Sálvia
does not replace Marfim as a full-screen background.

## Typography

Reference typeface: **Manrope** (OFL), bundled in the app as static
weights 400, 500, 600 and 700.

- Titles: Medium or SemiBold, tight leading.
- Body and captions: Regular, generous leading.
- Numbers and evidence codes: Manrope Medium with tabular figures where
  available.
- Fallback: system sans only when the bundled font cannot load.
- Do not use decorative, condensed or script faces.

Scale follows Material 3 roles in the app (see design-system tokens).
Report covers use title 20 pt, heading 16 pt, body 12 pt, small 9 pt.

## Voice and copy

- State facts and evidence; avoid promises the product cannot prove.
- Prefer short Portuguese sentences and concrete verbs.
- Never imply legal certification, inspection approval or authorship by
  the app.
- Signature may close marketing surfaces; operational screens stay
  functional and quiet.

## Application

App:

- The Home masthead carries the lockup. Other root mastheads carry the
  monogram beside the title; detail mastheads prioritize back and context.
- Primary actions use Pinho and Marfim in strong contrast. A Pinho feature
  panel uses a Marfim button so its action remains distinct.
- Background: Marfim. Cards use ivory tints and fine outlines. Sálvia
  supports selection and secondary details.
- Bottom navigation uses pinho selection with a Sálvia indicator.

Report cover:

- Mark and wordmark at the top, pinho rules, Marfim paper.
- Title in pinho, body in Grafite, supporting text in Grafite at reduced
  opacity but still AA.
- QR and verification text keep the same hierarchy; branding never
  obscures evidence or integrity claims.

Marketing and presentations:

- Marfim background, ample margin, one pinho accent per composition.
- Photography is documentary; no heavy filters that alter evidence.

## Accessibility

- Meet WCAG AA for all text; AAA where the base palette allows.
- Never encode state by color alone; pair with label, icon or shape.
- Keep 48 dp minimum touch targets and support large font scales.
- The logo is decorative when the product name is already announced;
  otherwise give it an accessible name.

## Do not

- Do not use the mark as a letter in running text.
- Do not place the logo on busy photos without a solid Marfim or Pinho
  safe area.
- Do not introduce new brand colors, gradients or neon accents.
- Do not edit the symbol proportions or remove the central notch.
