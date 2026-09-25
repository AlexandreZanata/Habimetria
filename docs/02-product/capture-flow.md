# Capture Flow — Entry and Exit

## Entry flow (MVP)

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
