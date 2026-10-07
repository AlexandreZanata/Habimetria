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

## Local PDF profiles

- Property detail lets the user start a rental or technical Inspection.
  Technical Inspection owns Property directly and has no dummy Tenancy.
- Review includes optional report details for rental use. Technical use
  requires author/council, scope, methods, limitations, analysis,
  conclusions, an ART/RRT reference and author acknowledgement before seal.
  Professional review also requires the declared inspection date, summary,
  references and explicit attribution/registered responsibility/applicability
  acknowledgements. Capture fields remain unchanged; details save incrementally.
- SEALED review displays frozen metadata; editing requires a new Inspection
  or Occurrence, never an unlock. A new PDF version preserves previous bytes.
- Share checks stored PDF, receipt, Manifest and originals again. Users can
  share a plain PDF or a portable offline verification package. Unknown
  installation keys remain unknown; sharing never records party agreement.

## Local identification and post-generation declarations

Rental identification is opt-in: users can omit name, CPF and contact.
Supplied CPF check digits are checked; masking affects PDF presentation only,
while the signed Manifest/ZIP retains the complete supplied data. Explain
this before collection and export. Professional identity remains necessary;
additional CPF/email/phone remain optional.

After READY, the user can deliberately append authorship, receipt, agreement
or disagreement to the exact selected Report version. Show signing intent,
optional actor/role/note and an explicit confirmation. Anonymous local
statements identify only the installation key. Disagreement needs a note.
Optional Android A1 signing selects a bounded private PKCS12 import, previews
the certificate holder and asks for explicit signing intent. Private key and
password are never exported or saved. The PDF remains unchanged; signatures
and declarations accompany the fresh ZIP export. External identity trust,
revocation and certified time are not inferred from an included certificate.
