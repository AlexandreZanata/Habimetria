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

Manifest v4 freezes these declarations, attachment hash, Finding labels and
encoded/display photo dimensions. Generation validates the canonical schema,
Manifest digest/signature and historical public key, then reads only signed
substantive inputs. Old v1–v3 artifacts retain their original signed bytes.
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
are derivatives up to 1600 px; layout identity is local-pdf/v4. Presentation
is selected before seal; new rendering requires a new Report ID/version.

Native font metrics feed shared pagination before drawing. Cover, physical
Room index, reviewed professional sections, photo references/categories,
coverage gaps/non-applicable categories, all Findings/notes, methodology and
full hash appendix use complete text. Long paragraphs split across pages;
no silent truncation, guessed photo aspect or emergency unnumbered pages.
A logical outline carries physical page references; current Android/iOS
writers do not embed native bookmarks. Native adapters render exact positions and final page/total values.
Android uses PdfDocument; iOS uses UIKit PDF, CoreText and ImageIO. iOS closes
its PDF context before consuming final bytes. Bytes may differ by platform.
Manual display rotation (DRAFT-only quarter turns) is frozen in Manifest v5;
originals stay immutable and derivatives are regenerated from verified bytes.

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
included key remains UNKNOWN until independently trusted. Certificate chain
validity and device-time expiry are reported separately; revocation stays
UNKNOWN offline and trusted time stays ABSENT without a registered external
ACT token (EXTERNAL once a contracted-authority token is stored, still
requiring the issuing ACT for validity). No private key is exported. Hash/QR/installation signatures never imply civil identity or
ICP-Brasil certification. Embedded PAdES is not produced locally; optional
external PAdES/Gov.br/ACT handoffs preserve READY bytes, require explicit
user acknowledgment, accept only PDF-shaped distinct copies, store them
separately and stay EXTERNAL_UNVERIFIED until checked in ITI VALIDAR or the
issuing authority. They require an authorized holder certificate, Gov.br
prata/ouro account or contracted TSA and never block offline generation.
Gov.br takes no credential in the app; PAdES signers should enable DocMDP
to avoid Indeterminate at VALIDAR.

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

## Authorship and corporate editorial extension

[Authorship contract](report_authorship_contract.md) defines optional rental
identification, professional applicability declarations, A4 editorial margins
(30 mm top/left, 20 mm bottom/right), 12 pt body, summary/keywords, numbered
sections and a contents list with physical page references. Personal CPF is
masked unless the user selects full display; the ZIP retains supplied data.
Analysis/conclusions follow photographic evidence. Navy accents and black
body text provide a restrained corporate presentation using local fonts.
This editorial baseline does not certify full ABNT technical conformity.

New receipt v2 requires an installation-signed declaration index. Explicit
Share declarations can be appended without changing PDF/Manifest/receipt;
fresh export includes current verified statements and older exported snapshots
remain valid. Optional Android/JVM PKCS12 holder signatures are separate
from installation signatures and do not manufacture external certificate trust.
Android backup is disabled; credential copies are bounded/deleted and stale
picker imports are removed at startup. Inspection/Report removal cleans the
associated private declarations and generated package copies.

Current extension delivery uses code tests/compilation only, by user request.
No emulator, phone, native PDF probe, generated sample or visual/print review
is run. The user performs manual validation after all implementation phases;
iOS runtime qualification remains outside this request. Earlier native evidence
belongs only to its documented earlier build.
