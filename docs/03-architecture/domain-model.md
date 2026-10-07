# Domain Model — Normative Terms

Use these exact names. No synonyms.

```text
User
 └── Property
      ├── Tenancy
      ├── Inspection (DRAFT | SEALED)
      │    ├── Room
      │    │    ├── Evidence
      │    │    └── Finding
      │    ├── Manifest
      │    └── Report
      ├── Occurrence (+ Evidence, never edits sealed Inspection)
      └── Share (+ AccessLog)
```

## Inspection states

- `DRAFT`: editable, upload allowed.
- `SEALED`: terminal in every mode. No `SEALED → DRAFT`.
  Changes via `Addendum` or `Occurrence`.

## Local authority (L0)

- Local mode persists Property, Tenancy, Inspection, Room, Evidence,
  Finding, Manifest and Report on-device. The API path stays optional.
- Room labels are user-typed Unicode, validated for whitespace/length.
  Internal IDs and keys never derive from the label.
  Only `DRAFT` allows label edits; `SEALED` freezes them.
- Device time, GPS and Room names are device claims, never certified facts.
  Local SHA-256 is an integrity measurement.
  Never set `received_at_server` locally and never label local
  `sealed_at` as trusted server time.
- Local durability may show "Salvo neste dispositivo".
  Server states `queued | uploading | confirmed | failed` still mean
  server sync only. Offline Evidence never becomes `confirmed` locally.

## Report (in PDF footer + verify page)

- Report ID, sealed_at (server time), evidence count, report SHA-256, verify URL + QR.

Share ≠ agreement. Log `created | accessed | downloaded`.

## Local PDF profiles

- New local Inspection rows may own a Property directly. Tenancy is
  optional only for `kind = technical`; `entry | exit` retain Tenancy
  semantics. Legacy rows resolve their Property through Tenancy.
- Report details belong to Inspection and are DRAFT-only: participant
  names, author identification, purpose, scope, methods, limitations,
  analysis, conclusions, recommendations and ART/RRT reference.
  Technical details require an explicit author acknowledgement and a
  council/registration declaration. These are user declarations; the
  app does not verify professional attribution or council registration.
- Manifest v4 freezes these details, optional personal identification,
  contract/date/company/summary/references, professional applicability
  acknowledgements, finding labels and photo display metadata. Older signed
  Manifest versions remain valid and are never rewritten.
- Multiple Report artifacts may reference one Manifest. Each has its
  own ID, byte digest and detached receipt. READY bytes are immutable;
  a new rendering creates a new Report rather than overwriting history.
- Neither a local installation signature nor a supplied ART/RRT number
  guarantees legal acceptance, certified identity or ABNT compliance.

## Local Share declarations

Report-specific Share declarations record explicit AUTHOR, RECEIVED, AGREED
or DISAGREED intent, optional declared actor/role and a device-time claim.
Disagreement requires a note. A signed index and previous-file hash chain
bind each statement to the exact Report ID, PDF and Manifest digests.
They are Report package attachments, not new Inspection states and not
edits to the frozen Inspection/PDF/receipt. Sharing alone creates none.
Installation statements do not prove another party participated.
Optional Android/JVM A1 certificate signatures cover the declaration bytes;
certificate subject, signature validity, fingerprint trust, revocation and
trusted time remain separate. Included certificates are UNKNOWN by default.
See [authorship contract](../05-mobile/report_authorship_contract.md).
