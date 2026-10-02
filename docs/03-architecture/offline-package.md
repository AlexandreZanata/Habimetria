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
<package>/declarations-index.json  signed Report-specific index (receipt v2)
<package>/declarations/<id>.json    detached signed Share declarations
```

## Canonical bytes

- Receipt: fixed field order
  `algorithm, formatVersion, manifestId, manifestSha256, pdfSha256,
  purpose, reportId, signerKeyId`, JSON quoting per writer for receipt v1.
  Receipt v2 inserts `declarationsRequired:true` directly after `algorithm`
  and signs `formatVersion:2`; the index is required even when empty. The Go
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

## Portable export and bounded import

- The app exports a stored (uncompressed) ZIP and imports it through the
  system document picker. Only the five control files, original photo IDs
  optional responsibility document IDs and signed declaration entries are allowed; compression,
  duplicate names, traversal and descriptors are rejected.
- Maximum original/PDF size: 50 MiB; metadata: 8 MiB; responsibility
  document: 10 MiB; package: 512 MiB and 1,039 entries. Processing is local.
- Verifiers bind fingerprints to public-key bytes, check supported purpose,
  version and algorithm, and reject malformed/oversized imports. Unknown
  keys never acquire trust from package metadata or QR alone.
- [Independent Go implementation](../../backend/cmd/verify/README.md) checks
  the same Manifest v1–v4 and receipt v1/v2 formats with bounded reads and confined paths.
- Selecting an ART/RRT PDF preserves its original bytes and freezes its
  digest in reportDetails.responsibilityDocument. The package includes
  `responsibility/<id>.bin`; external certificate/council validation is
  separate from byte integrity and is never claimed automatically.

## Report-specific declarations (receipt v2)

Index envelope: `payload` (serialized DeclarationIndex JSON string) and
`signature` (raw P-256). Index body: `reportId,pdfSha256,manifestSha256,entries`,
`formatVersion:1,purpose:local-share-index/v1`; entries are ordered `id,sha256`.
The index is signed by the original Report installation key. Maximum 32
unique declarations; index <=1 MiB, declaration file <=256 KiB, payload <=40 KB.

Declaration envelope: `payload,keyId,signature,publicKey,certificate`.
The signed UTF-8 payload is used verbatim; no canonical reserialization.
Body: `id,reportId,pdfSha256,manifestSha256,intent,actorName,actorRole,note`,
`createdAtDevice,previousSha256,formatVersion:1,purpose:local-share-declaration/v1`.
First previous hash is empty; later values hash the complete previous file.
Index requires the exact file set/order, all signatures and exact Report
binding. Receipt v1 rejects unbound declaration files. Old exports remain
valid historical snapshots; no offline verifier can discover future updates.

Installation proofs carry uncompressed P-256 public key and raw signature.
Certificate proofs carry a leaf-first unique chain of 1–8 DER certificates
(hex), `algorithm:SHA256withRSA|SHA256withECDSA` and DER/provider signature
(hex). RSA 2048–8192 or named P-256, end entity usable for signing; included
parent signatures/CA/key usage must match. Fingerprint is SHA-256 of leaf DER.
Certificate subject is independent of the typed actor name. Trust is UNKNOWN
unless independently pinned; revocation/trusted time remain unchecked.
This is a detached declaration format, not PAdES/PDF-A or an ITI profile.

Private app commit uses an atomic signed-index update: prepared declaration
bytes are staged under `pending`, index commits their digest, then bytes move
to the indexed directory. Recovery completes only missing files whose pending
bytes match a valid Report-bound signed index. Received packages are never
repaired. Pending credential/import files and private keys are never exported.
