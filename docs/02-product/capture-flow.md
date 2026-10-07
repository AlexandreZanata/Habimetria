# Capture Flow — Entry and Exit

## Local journey (L0 authority)

1. `Start inspection` (no signup, no payment, no network).
2. Checkout-style address (street + number + optional complement +
   neighborhood) + Tenancy + Inspection created locally. The form
   prefills from the latest unfinished inspection.
3. Rooms typed manually by the user (for example "Suite principal").
   Automatic templates are not part of the primary flow.
   Unfinished inspections resume from Home with rooms intact;
   `Iniciar vistoria` opens the first room's capture with one tap.
4. Per room guided checklist, five categories:
   overview → walls → openings → floor → ceiling.
   `Próximo cômodo` moves forward in room order; the last room
   leads to review. Skipped rooms stay visible as gaps.
5. `+ Report issue` or `All good`.
6. Review → coverage % → seal locally → PDF on-device → share via system UI.

Coverage counts distinct applicable categories, never photo count.
Repeated photos of the same category do not complete the checklist.
Non-applicable categories are recorded explicitly.

## API-dependent flow (optional integration)

1. `Start inspection` (no signup wall)
2. Property address (minimal)
3. Rooms from template: living, kitchen, bedroom, bathroom, exterior
4. Per room guided checklist: overview → walls → doors/windows → floor → ceiling
5. `+ Report issue` or `All good`
6. Review → coverage % → email for magic link → seal → pay → share

## Finding input (minimal)

- Photo (required)
- Preset (required): stain | scratch | crack | broken | moisture | hole | other
- Note (optional)

No mandatory: description, state, classification, responsible fields.

## Exit flow (schema-ready in MVP, UI in V1.2)

- Reopen property → exit inspection → repeat same room/position
- Side-by-side or slider: entry photo vs exit photo
- User tags: `no_change | changed | wear`
- AI suggestion only after V1.4. AI never assigns liability.

## Coverage index (no AI)

Per room % + total %. Example prompt:

> "Bathroom 60% — add ceiling, inside shower, under sink."

Drives completeness and perceived protection.
