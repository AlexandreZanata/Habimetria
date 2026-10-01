# Local Validation Architecture — Planned Contract

Planning companion to [the roadmap](../06-roadmap/local_validation_plan.md).
These decisions require the L0 normative update before code implementation.
Existing domain names remain authoritative in [domain-model.md](domain-model.md).

## Local authority and platform boundaries

The production app path for this scope uses shared Kotlin repositories
and use cases, transactional SQLite and durable private files. UI does not
call `VaultApi` directly. Property, Tenancy, Inspection, Room, Evidence,
Finding, Manifest and Report persist locally across process death.
Room KMP is the preferred SQLite candidate, subject to L0 compatibility
checks. API repositories remain optional and explicitly configured.
Docker on a developer's laptop is useful integration infrastructure;
it does not satisfy the requirement to operate on the phone offline.

Shared code owns validation, checklist coverage, snapshot creation,
canonical serialization, verification and the PDF layout contract.
Platform adapters own camera, location, protected signing keys, filesystem
durability details, native PDF drawing, share sheet and inset interop.
Organize features per the KMP layout; split files before approximately
200 lines and reference each new file in its folder README or parent doc.

## Persistence and state semantics

- Generate unique stable IDs; never name originals using photo counts.
- Write an original to a temporary durable file, finish the write, rename
  atomically and commit its Evidence reference. Reconcile interruption
  between filesystem and DB operations at startup; surface corruption.
- Preserve original bytes. Compression, EXIF changes, previews and PDF
  images create derivatives. Do not burn coordinates into the original.
- Persist work progress and navigation separately from domain states.
- `DRAFT | SEALED` remain the only Inspection states; SEALED is terminal.
- Local durability may display “Salvo neste dispositivo”. The existing
  `queued | uploading | confirmed | failed` states still mean server sync.
  Offline Evidence never becomes `confirmed` through a fabricated ack.
- Track Report generation/recovery separately from Inspection state.
  Failed rendering allows retry against the same sealed snapshot.
- Never regenerate/overwrite a finalized Report. A changed rendering
  requires a new Report identity referencing the same Manifest.

L0 must explicitly revise the current “hash + timestamp server-side only”
rule: local SHA-256 is an integrity measurement, and local time is a device
claim. Neither becomes a server-certified value. Never set local values
in `received_at_server` or label local `sealed_at` as trusted server time.

## Room labels, checklist and capture claims

User-entered Room labels preserve Unicode, with whitespace validation,
a documented length bound and duplicate-name handling. Generate an
internal key independently; changing a label cannot change Evidence IDs.
Only DRAFT allows edits. Require deliberate handling of attached Evidence
before removing a Room. Finding presets remain independent of Room names.
Resolve the existing four-versus-five-shot mismatch in L0; coverage uses
distinct applicable overview/walls/openings/floor/ceiling categories.
Record non-applicable categories explicitly, never infer them from counts.

Each Evidence records immutable original hash/size/MIME, stable sequence,
Room association, capture origin and device capture time. Location claims
include nullable coordinates, accuracy in meters, fix time/age, provider,
permission accuracy level, availability and simulation flags when exposed.
Request foreground location when useful; background location is not needed
for shutter-time evidence. A bounded freshness policy must be specified
and tested before release. GPS may work offline, but a fresh fix is not
guaranteed. Reverse geocoding and map tiles cannot block the local journey.
An absent fix is absent; EXIF coordinates remain a separately labeled claim.

## What the trust indicators actually prove

1. Original/PDF hashes detect changes relative to the signed snapshot.
2. An asymmetric signature proves possession of the corresponding key.
3. Issuer trust requires an independently trusted key or certificate.
4. GPS, device clock, Room names and Findings are captured/reported claims.
5. A signature cannot prove that a photographed scene is real, that the
   device clock was correct or that location was not spoofed.

Reports must not say “all data certified authentic”. State exactly which
integrity and issuer checks passed and which claims lack independent
confirmation. A new installation generating its own key is an unknown
issuer until a verifier independently trusts that identity.

## Signing and issuer identity

Use an installation-specific ECDSA P-256/SHA-256 signing adapter; prefer
Android Keystore and iOS Keychain/Secure Enclave where supported. Specify
public-key and signature encodings with cross-platform test vectors.
Hardware availability is detected, not assumed; simulator/software keys
are marked as validation-only and cannot impersonate production trust.
Do not embed a global signing secret, HMAC key or issuer private key in
the app. The existing server HMAC is not an offline public verifier format.

A verifier cannot trust a key solely because it arrived inside a package.
Allow independently compared/pinned fingerprints. For “issued by this
system”, optionally provision a publisher-signed installation certificate
under a pinned public root, with enrollment outside the offline capture
journey. Offline users without that provisioning get valid integrity
checks and an explicit unknown issuer; do not manufacture publisher trust.
Fresh revocation status and independently trusted time require external
information. An offline verifier must disclose its trust-data freshness.
Lost/rotated installation keys do not rewrite sealed artifacts; preserve
public verification material and create new reports under a new identity.

## Manifest, Report and avoiding circular hashes

Freeze a versioned canonical Manifest with deterministic field ordering,
encoding, number representation and sequence ordering. Bind Property and
Inspection identifiers, Room IDs/labels, all Evidence hashes and metadata,
Findings, checklist applicability and local seal-time claim. Signature
inputs must include format version and purpose to avoid cross-use.
Document nullable fields, Unicode and signed-byte encoding in L0.

Finalize in this order:

1. Validate all originals; commit the immutable snapshot and its signature.
2. Render the Report using exactly that snapshot and save final PDF bytes.
3. Calculate `report_sha256` over those exact bytes.
4. Sign a detached receipt binding Report ID, Manifest hash, PDF hash,
   format version and signer identity; commit completed artifacts.
5. Verify locally before enabling the final verification badge/share flow.

Never embed the hash of the entire final PDF inside that same PDF: changing
the text changes the bytes being hashed. The PDF can contain Manifest hash,
Report ID, key fingerprint, QR and verification instructions. Its exact
file hash lives in the detached signed receipt. This receipt is not a
standard embedded PDF digital signature; do not claim PDF-reader signature
badges, certificate-backed signing or PAdES support without a separate scope.

## Offline export and verification

Share a PDF normally; recommend PDF plus receipt for byte verification.
An evidence package additionally contains the Manifest/signature,
originals and public signer material; it never contains private keys.
Without originals, original-Evidence integrity remains unchecked. Without
a signed receipt, a PDF/QR alone cannot prove the final PDF's integrity.
QR content identifies Report ID, Manifest digest and format version;
it is a lookup/import aid, not a substitute for checking the actual bytes.
Never print a LAN/localhost URL as public verification.

Independent verification validates schema, signatures and issuer trust,
recomputes available file hashes, checks counts/IDs and returns separate
results for valid, modified, missing, unsupported and unknown issuer.
Package import requires file-size limits, safe paths and bounded decoding.
Public web verification and server acknowledgements remain later options.

## Optional server mirror (V-sync)

Local rows stay the source of truth; the API is a push target, never
the authority. `ServerMirror` (`vault.sync`, reached only through
`AppContainer.serverMirror()`) copies user-confirmed occurrences,
comparison tags and exit inspections to the existing REST endpoints.
Screens keep never calling `VaultApi` directly. `LOCAL_ONLY` mode
reports `SkippedLocalOnly` without network traffic, and failures return
an explicit `Failed` reason for retry instead of throwing. Confirmed
links (`localId -> remoteId`) persist in `mirror_links` (schema v11,
first write wins), so relaunch retries reuse the original remote row.
Comparison evidence references mirror as null: server evidences live in
a separate id space and tag history stays on device. Photo bytes stay
with `UploadPump`. Mirroring never marks local rows `confirmed` and
never changes seal/verification semantics.

## Primary implementation references

- [Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Android insets](https://developer.android.com/develop/ui/compose/system/insets)
- [iOS safe area](https://developer.apple.com/documentation/uikit/uiview/safeareainsets)
- [Compose compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [Android location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [iOS reduced location accuracy](https://developer.apple.com/documentation/corelocation/claccuracyauthorization/reducedaccuracy)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Apple protected keys](https://developer.apple.com/documentation/security/protecting-keys-with-the-secure-enclave)
