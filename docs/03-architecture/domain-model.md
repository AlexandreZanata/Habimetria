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
- `SEALED`: terminal. No `SEALED → DRAFT`. Changes via `Addendum` or `Occurrence`.

## Report (in PDF footer + verify page)

- Report ID, sealed_at (server time), evidence count, report SHA-256, verify URL + QR.

Share ≠ agreement. Log `created | accessed | downloaded`.
