# On-Device PDF — Sealed Local Reports

Follows [local authority](../03-architecture/local_validation_architecture.md).
The local pipeline runs without VaultApi, server rendering, remote fonts,
maps or geocoding: capture → durable originals → review → seal → PDF → share.
See [the detailed contract](local_pdf_contract.md) and
[execution evidence](../06-roadmap/local_pdf_execution.md).

## Frozen report inputs

The tenancy presentation describes the property, participants and visible
observations. The professional presentation supports an Inspection directly
under Property without a fabricated Tenancy. Entry/exit retains its existing
Tenancy linkage. SQLite v16 migrates existing links and report versions.

Review collects declared author/title/role, municipality/state, participants,
purpose, scope and photo presentation. Professional seal requires author,
title, purpose, scope, methods, limitations, analysis, conclusions, matching
CREA/ART or CAU/RRT, registration/state/reference, an attached PDF and explicit
author acknowledgment. The app checks completeness and byte integrity;
it does not validate council registration, professional attribution, an
external signature or whether the document is an authentic registered ART/RRT.
Recommendations and standards are included when supplied by the author.

Manifest v3 freezes these declarations, attachment hash, Finding labels and
encoded/display photo dimensions. Generation validates the canonical schema,
Manifest digest/signature and historical public key, then reads only signed
substantive inputs. Old v1/v2 artifacts retain their original signed bytes.
SEALED is terminal; later corrections use Occurrence/Addendum.

## Photo geometry and measured layout

All EXIF orientations 1–8, including mirrors, use the same display convention.
Absent/invalid orientation tags use identity; undecodable formats fail clearly.
Native/JVM derivatives normalize orientation; original bytes never change.
The PDF uses hash-checked originals, bypassing unverified preview caches.
Bounded codecs prevent full-resolution camera pixels from entering PDF drawing.

A4 portrait pages contain each portrait/landscape image without crop or stretch.
Standard image height is at most 300 pt; detail presentation allows 520 pt.
Changing height changes width to preserve the oriented aspect ratio. Images
are derivatives up to 1600 px; layout identity is local-pdf/v3. Presentation
is selected before seal; new rendering requires a new Report ID/version.

Native font metrics feed shared pagination before drawing. Cover, physical
Room index, reviewed professional sections, photo references/categories,
coverage gaps/non-applicable categories, all Findings/notes, methodology and
full hash appendix use complete text. Long paragraphs split across pages;
no silent truncation, guessed photo aspect or emergency unnumbered pages.
Native adapters render exact positions and final page/total values.
Android uses PdfDocument; iOS uses UIKit PDF, CoreText and ImageIO. iOS closes
its PDF context before consuming final bytes. Bytes may differ by platform.

## Device claims

Every photo prints capture time/origin/UTC offset where recorded, location
availability or WGS84 coordinates, estimated horizontal accuracy, permission
level, provider, fix age/time and nullable simulation indication. Denied,
disabled, absent, approximate and stale/unknown readings stay explicit.
EXIF GPS is a separate declaration; discrepancies are displayed.

Capture does not wait for GPS. GPS/time are device claims, not certified
scene location or trusted shutter time. A negative simulation indication
cannot certify authenticity. No coordinates are invented for missing data.

## Publication, history and verification

ReportGenerator serializes generation/export/removal. It validates originals
before drawing, hashes exact finalized PDF bytes and atomically publishes a
detached signed receipt binding Report/Manifest IDs, digests, key, purpose
and version. READY follows stored receipt/signature checks and a final source
recheck. Errors/cancellation retain SEALED and can retry unfinished artifacts.
Restart finds unfinished status for retry; no partial output is shareable.

READY bytes are immutable and reverified before reuse/share. Modified READY
artifacts fail instead of being overwritten. Explicit new versions preserve
previous IDs/bytes/receipts. Local public signing material survives rotation
for historical verification. Inspection removal also removes its local
responsibility attachment and generated package copies; external shared
copies remain outside the app's control. Unavailable historical private keys block new
issuance under that seal instead of inventing a signature.

The QR uses correction level M and carries Report/Manifest identity only.
The exact PDF digest is detached,
never embedded inside that same PDF. Export offers readable PDF or a portable
ZIP containing PDF, receipt, Manifest, public key, originals and declared
responsibility attachment. A staged ZIP is reimported and cryptographically
verified before atomic publication. Received original app ZIPs are selected
through the system document picker and verified without the local database.
Compressed/repacked third-party ZIPs are explicitly unsupported.

Kotlin and the independent Go verifier separate file integrity, signature
validity and issuer trust. Fingerprints bind actual public-key bytes; an
included key remains UNKNOWN until independently trusted. No private key is
exported. Hash/QR/installation signatures never imply civil identity or
ICP-Brasil certification. Optional external PAdES/ACT/Gov.br integrations
are not implemented; they require a separate authorized workflow/version.

## Budgets and qualification

PDF output cap: 50 MiB; package: 512 MiB. Target: <10 MiB for 100 photos,
subject to readable evidence. Work runs off the UI thread with progress,
cancel and retry. Total native PDF memory can grow with retained pages;
large reports and low-memory hardware need separate qualification.

[Execution evidence](../06-roadmap/local_pdf_execution.md) records native
Android emulator measurements and independent reader checks. Physical-camera
orientation, physical-device memory, iOS native runtime, print and scoped
professional/legal compliance remain release gates. Local processing detects
changes against signed references; it cannot guarantee universal legal acceptance.
