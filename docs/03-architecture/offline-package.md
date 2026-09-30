# Offline Package — Published Format (L7)

The contract both the app (`vault.local.PackageFormat`) and the
independent verifier (`backend/cmd/verify`) implement. Either side
accepts genuine output of the other; see
`backend/testdata/fixture-package` (synthetic throwaway key).

## Layout

```text
<package>/report.pdf          final PDF bytes
<package>/receipt.json        {"receipt": {...}, "signature": hex}
<package>/manifest.json       canonical manifest bytes, verbatim
<package>/manifest.sig.json   {"keyId","algorithm","signature"}
<package>/signer.json         {"keyId","publicKey","hardwareBacked"}
<package>/originals/<id>.bin  immutable originals by evidence id
```

## Canonical bytes

- Receipt: fixed field order
  `algorithm, formatVersion, manifestId, manifestSha256, pdfSha256,
  purpose, reportId, signerKeyId`, JSON quoting per writer. The Go
  verifier rebuilds these bytes from parsed values; byte equality
  with the Kotlin builder is covered by the shared fixture test.
- Manifest: `manifest.json` must byte-match `manifestSha256`, and its
  evidence `sha256` values must match the shipped originals.

## Crypto

- ECDSA P-256/SHA-256. Public keys: 65-byte uncompressed
  `0x04||X||Y`. Signatures: 64-byte raw `R||S` hex (never DER).
- `keyId` = hex SHA-256 of the uncompressed public key.

## Verdict dimensions (shown separately, never merged)

- PDF integrity (hash vs receipt), manifest integrity (hash vs
  receipt + key binding), evidence integrity per original,
  signature validity (manifest + receipt), issuer trust.
- Missing and modified stay distinct. Unknown signers stay unknown:
  integrity can pass while trust does not.
- Trust sources: PINNED (compared fingerprint) and CERTIFIED
  (publisher certificate against a caller-provided root). Publisher
  enrollment lives outside the offline journey; no production root
  is hardcoded.

## QR

`vault-report/v1/<reportId>/<manifestSha256>`: identity for lookup
and local import only. Scanning it never certifies bytes or photos.
