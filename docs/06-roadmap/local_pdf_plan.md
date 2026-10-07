# Local PDF — Focused Delivery Plan

Status: local core implemented; earlier Android evidence is historical.
Current authorship/editorial extension delivers code and code tests only.
By user instruction, do not run emulator/physical devices/native PDF probes
or visual artifact generation in any current phase. The user validates
manually after all phases; iOS checking is excluded. See
[current delivery](report_authorship_execution.md) and
[historical execution evidence](local_pdf_execution.md).
Planning baseline reviewed: `f0493c6`.
Research date: 2026-10-02. Scope: Brazil, residential tenancy records and
architecture/engineering work. This document does not certify the app.

References: [legal applicability](../03-architecture/brazil_report_legal_requirements.md),
[rendering/integrity contract](../05-mobile/local_pdf_contract.md),
[acceptance](local_pdf_acceptance.md), [domain](../03-architecture/domain-model.md),
[local architecture](../03-architecture/local_validation_architecture.md).
Continue the existing Capture → durable storage → review → seal → PDF → share
journey. This scope focuses exclusively on Report generation and the data
needed to make its output complete, understandable and independently verifiable.

## Two uses, one local pipeline

The tenancy presentation produces a descriptive entry/exit Report for ordinary
people: Property, parties and their roles where provided, Inspection purpose,
Rooms, Evidence, Findings, visible defects, coverage and limitations. Keep
photo + preset + optional note as the capture flow. Collect additional
identification during review; do not require a professional registration.
An observation of a crack does not become an engineering diagnosis.

The professional presentation adds the responsible User's identity, title,
CREA/CAU registration and jurisdiction, contracting party, scope, methodology,
applicable standards/editions, inspected systems, access limitations, analysis,
conclusion/recommendations and the applicable registered ART/RRT attachment.
Findings stay evidence-linked; technical conclusions are authored by the
qualified professional. Record signatory role and signing intent separately
from the installation key. A professional profile alone grants no authority.

These are planned presentation choices, not new Inspection states or domain
entities. Define their fields against the existing domain before implementing.
Cover professional work without a lease as well as Tenancy-based entry/exit:
PDF01 must explicitly revise the domain/product contracts for an Inspection
associated with Property and an optional Tenancy, then migrate existing links.
Never invent a dummy Tenancy. Judicial expert reports need a separately
qualified scope before being advertised as supported.

## Gaps observed at the planning baseline

- `PdfContract.kt` uses original pixel dimensions and caps photo height while
  retaining full content width. Both renderers draw into that rectangle;
  tall images can be stretched. Caption space is not included in the shared
  photo fit decision and text blocks are not fully measured there.
- Android `PdfDims.kt` ignores EXIF orientation. `AndroidScaler.kt` handles
  rotations but not all mirrored cases. The PDF fallback decodes originals
  without an EXIF transform. iOS `IosScaler.kt` returns unchanged bytes for
  small images; `IosPlanRenderer.kt` omits thumbnail orientation transform.
- Both renderers truncate Findings/captions and accept undecodable images as
  a drawn missing-photo message. Emergency pages reuse logical page numbers.
- `ReportGenerator.buildModel` rereads current Property/Room/Evidence/Finding
  rows and coverage. Generation checks original existence, not every original
  digest; previews are preferred without a verified source relationship.
- Generation can overwrite an existing Report. Deletion comments promise
  byte-identical regeneration, which is not established. iOS reads backing
  bytes before `UIGraphicsEndPDFContext`; finalization order needs validation.
- Location freshness exists (30 seconds), but missing GPS disappears from
  photo captions. iOS simulation is always false despite an available native
  source-information API. This is source review, not physical-device proof.

## Ordered implementation slices

### PDF01 — Freeze Report requirements and sealed inputs

Priority P0. Publish both presentation schemas and their review fields;
document the professional purpose/flow and Property link with optional Tenancy
before code. Preserve existing entry/exit meaning and DRAFT/SEALED states.
Version additions to Manifest/package formats and SQLite with migration
fixtures, including legacy required Tenancy links. Include every displayed
substantive field in the sealed snapshot,
including identity claims, professional attachments and report purpose.
Decode validated canonical Manifest bytes for generation; derive coverage
from that snapshot. Do not print later live Property edits under an old hash.
Accept: changing live rows after seal cannot change a Report's content.

### PDF02 — Normalize photo display orientation

Priority P0; depends on PDF01's metadata contract. Use all eight EXIF
orientations, including mirrored cases, with bounded platform decoding.
Normalize derivative pixels once; dimensions describe the same display
representation that the renderer uses. Portrait stays portrait, landscape
stays landscape, and mixed orientations coexist on an A4 page without crop
or stretching. Preserve original bytes; regenerate untrusted cached previews.
Accept: asymmetric orientation fixtures match capture, review and PDF.

### PDF03 — Make pagination complete and finalization reliable

Priority P0; depends on PDF02. Measure text and photo/caption blocks, split
long blocks, reserve footer space and calculate actual physical page totals.
Remove silent truncation, guessed dimensions and success on image decode
failure. Finish native PDF contexts before reading bytes. Validate originals
against Manifest hashes and close render/export races. Bound image memory,
write PDF/receipt atomically and recover interrupted generation.
Accept: every sealed Evidence/Finding appears; any missing/corrupt source
prevents final success; retries retain SEALED and do not overwrite READY.

### PDF04 — Deliver professional Report presentation

Priority P1; depends on PDF03. Implement the page hierarchy in the rendering
contract, a reusable restrained visual system, thumbnails in preview,
legible captions, bookmarks where supported, real page references and an
integrity appendix. Add the two presentations without burdening capture.
Freeze layout/version/quality selection before finalization. A4 stays the
default; a landscape photo changes its image slot, not the whole document.
Accept: visual/print review on both platforms and complete extracted text.

### PDF05 — Bind per-photo location and time claims

Priority P1; depends on PDF01. Extend metadata for fix timestamp, permission,
freshness and tri-state simulation/source detection. Keep EXIF GPS separate;
show missing/stale/approximate data explicitly. Include device capture time
and timezone/offset without changing old artifacts. Address is a reported
Property field, not a GPS-certified fact. No online geocoding/map dependency.
Accept: denial, old fixes, indoor/no-fix and mocked locations remain honest.

### PDF06 — Close integrity, export and issuer lifecycle gaps

Priority P0 before release; depends on PDF01–PDF05. Validate Manifest,
originals, PDF and detached receipt before final success. Keep verdicts for
integrity, signature and issuer identity separate. Retain historical public
keys; lost/rotated keys must not break verification of old artifacts.
Make READY immutable, allow explicit export-copy deletion, and give changed
renderings a new Report ID under the same Manifest. Reconcile the current
one-Report-per-Manifest constraint through a reviewed local migration.
Accept: independent offline verifier rejects substitutions and missing files;
an embedded self-generated key never becomes verified civil identity.

### PDF07 — Qualify professional/legal integrations

Priority P1 for the professional release; depends on PDF06. Enforce the
selected technical-service scope and ART/RRT/author checks. Review licensed
standards and location-specific rules before advertising compliance.
Optional certificate-backed PAdES uses an authorized holder's certificate,
independent trust material and explicit signing intent. Gov.br signing and
ACT timestamps are optional connected steps; local PDF generation remains
fully available. An installation signature is never renamed ICP-Brasil.
Accept: each claimed signature profile passes its applicable validation,
including ITI VALIDAR when supported; no unsupported certification label.

### PDF08 — Execute release proof

Priority P0 release gate; depends on all locally promised slices. Current
delivery runs semantic code tests and Android compilation only. After all
phases, the user manually executes applicable [acceptance](local_pdf_acceptance.md),
including PDF readers/print, orientation/sensors and storage/memory/crashes.
Do not launch an emulator, install on a phone or generate native PDF test
artifacts for this delivery. iOS checking is excluded. Record future manual
results by build/scenario rather than copying earlier measurements.
Accept: no omitted, rotated incorrectly, stretched or falsely certified
content; unexecuted platforms and regulatory checks remain unverified.

## Delivery and boundaries

Each slice: small PR, English docs, `make verify`, applicable mobile checks,
commit and push. Split touched files before approximately 200 lines.
Do not infer calendar estimates or completion from planned acceptance.
No mandatory server, account, payment, remote font, map or upload is added.
The locally supportable promise is tamper detection relative to a trusted
signed reference, documented completeness and tested offline generation.
Scene truth, independently trusted capture time/location and universal
judicial acceptance are not guarantees a local PDF generator can issue.
