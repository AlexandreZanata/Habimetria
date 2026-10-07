# Independent offline package verifier

- [main.go](main.go): CLI and exit codes.
- [types.go](types.go): published Manifest/receipt/verdict fields.
- [receipt.go](receipt.go): canonical receipt encoding.
- [signature.go](signature.go): digest, public-key parsing and ECDSA checks.
- [package_files.go](package_files.go): bounded reads confined to package.
- [package_check.go](package_check.go): file, version and fingerprint binding.
- [main_test.go](main_test.go): independently exported fixture tests.
- [package_binding_test.go](package_binding_test.go): forged fingerprints,
  unknown versions and symlink escapes.

`go run ./cmd/verify -format json <extracted-package-directory>`

Exit 0 confirms integrity and local signature binding. UNKNOWN issuer trust
remains UNKNOWN. No result certifies photographed facts or civil identity.
Supports Manifest v1–v4 and receipt v1/v2; the app's stored ZIP can be extracted by standard
ZIP tools before verification. Original ART/RRT PDF attachments, when
present, are checked against their frozen digest, not against a council.

- [declaration_types.go](declaration_types.go): index, intent, holder proof and separate trust statuses.
- [declarations.go](declarations.go): exact signed set/order and Report binding.
- [declaration_certificate.go](declaration_certificate.go): bounded X509 chain and RSA/P-256 holder signature checks.
- [declarations_test.go](declarations_test.go): unknown/pinned issuers, omission/alteration/reordering, foreign Report and legacy unbound statements.

Receipt v2 requires an index even without statements. Certificate holder
signatures are custom detached proofs, not PAdES. Revocation/trusted time
remain UNKNOWN; included certificates cannot grant their own trust.
