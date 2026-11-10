# Capture UX — Photos First, Zero Friction

## Rules

1. No signup before value. `Protect tenancy → address → camera`.
2. One question per screen. Camera view is the home screen.
3. Shutter is the biggest element. Review happens after, never before save.
4. Every photo shows queue truth: `✓ confirmed | ↑ sending | ! waiting`.
5. Findings: `photo + preset + optional note`. No mandatory text.
6. Coverage bar per room; nudge text for missing shots (no AI).

## Local capture (current)

- No embedded preview: `Fotografar` opens the full-screen system
  camera; shots return with the selected category bound.
- Every photo is deletable individually (row, findings, files).
- Room setup and capture share the PDF/Property pages' outlined cards,
  neutral surfaces and green actions. The shared header supplies the Room
  or page title; the body does not repeat it.
- Room setup uses `Cômodos` as its header title and keeps the form and
  list in one scroll. The form asks for one Room name at a time, shows
  examples below the field and provides a labeled `Adicionar cômodo`
  button. Keyboard Done invokes the same action. Draft name input is
  saveable. Optional presets stay secondary. The empty state explains
  the first step, and `Iniciar vistoria` appears once a Room exists.
  Each Room card opens capture and shows its label, coverage bar and photo
  count, with a white house icon on Pinho. Its labeled options menu retains
  rename, reorder and delete.
- Capture shortcuts scroll with the content and show no false active
  destination. The first outlined section contains wrapping category
  selectors and the prominent shutter. The selected category shows a white
  label and check on Pinho.
  Category selection, N/A edits and onward navigation are disabled while
  capture is busy. System camera and durable-save semantics are unchanged.
- Coverage has its own card: repository percentage, distinct captured /
  applicable category counts, and named photographed, missing or N/A
  states. A hint explains N/A; its labeled action explicitly excludes or
  reinstates a category. Initial loading never claims category completion.
  N/A exclusions remain distinct from photographs.
- Saved photos appear in a separate card in repository order, with bounded
  proportional thumbnails, native category labels, full-screen viewing
  and individual removal. Next Room and review are secondary outlined
  actions. All content scrolls; controls retain 48 dp minimum targets and
  text can wrap as font scale increases. Device visual and accessibility
  acceptance remain manual checks.
- Review uses a Pinho selection with white content in its navigation
  segments. The seal prompt uses a Pinho icon on white. Report data and
  inspection assistant use white icons on Pinho. Review controls use
  the Material interaction layer for hover, focus and press. The photo
  rotation choices wrap when space is limited.
- The inspection assistant separates missing capture categories by Room
  from review reminders grouped by photo. Each photo group shows the
  sequence number, Room and capture category and opens the photo viewer.
  A reminder asks the user to inspect a photo. A Finding is created only
  when the user explicitly records a stain or crack. Ignoring a coverage
  reminder leaves the underlying missing category visible. SEALED
  Inspections display the assistant in read-only mode.
- The rules above still hold, except "Camera view is the home screen"
  and "Camera pre-warmed on Home", which described the old embedded
  CameraX preview.

## Screen flow (M2–M4)

```text
Home (property + coverage ring)
 → Room (checklist: overview/walls/openings/floor + shutter)
 → Finding sheet (preset chips + optional note)
 → Review (grid + coverage % + missing nudges)
 → Seal (server ack → verify code + QR)
 → PDF (render → share via WhatsApp/Email/link)
 → Verify view (in-app browser of /v/{code})
```

## Speed details

- Camera pre-warmed on Home. ZSL/`MINIMIZE_LATENCY`, flash off default
  (flash kills latency; CameraX discussion group evidence).
- JPEG quality 80–85, max edge 2048px for upload originals.
  (Trade-off recorded: pixel-peeping originals deferred post-MVP.)
- EXIF kept locally for device claims (time, GPS); upload sends
  `captured_at_device`, `latitude`, `longitude` as fields.
- Haptics on capture; shutter sound per regional API (CameraX 1.6+).
