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
Supports Manifest v1–v3; the app's stored ZIP can be extracted by standard
ZIP tools before verification. Original ART/RRT PDF attachments, when
present, are checked against their frozen digest, not against a council.
