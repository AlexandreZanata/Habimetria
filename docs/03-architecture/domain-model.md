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
