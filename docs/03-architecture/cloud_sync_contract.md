# Hosted Backup and Synchronization — Planned Contract

Local authority: [local validation](local_validation_architecture.md).
Commercial boundary: [service monetization](../02-product/service_monetization.md).
The existing ServerMirror/UploadPump are seams, not full backup delivery.

## Enrollment and ownership

Account, verified monthly entitlement and explicit collection enrollment are
required for new hosted consumption. Select account, data, transfer policy
and plan allowance before upload. Never automatically claim legacy server
rows or upload one account's local data after switching login.
Stable local IDs map to owner-scoped remote IDs; enforce ownership server-side.
Record an enrollment receipt/version and allow stopping future sync.
Stopping sync, deleting a local copy and deleting hosted data are distinct.

## Backup completeness

A complete snapshot includes Property/Tenancy/Inspection/Room structure,
Evidence originals/metadata, Findings, Manifest/signature, every finalized
Report/receipt, public verification material and supported declarations,
Occurrences/comparison history. Preserve versions, references and byte hashes.
Inventory exact formats before enabling backup of a format; reject unsupported
data explicitly, never label a partial upload “complete backup”.

Installation private keys may be non-exportable. A restored collection preserves
its historic public verification material; new signing uses the current
installation identity. Never restore by manufacturing the old private key.
User certificate secrets are not cloud-backed by default.

## Transfer protocol

1. Inventory selected files/metadata; reserve quota with an idempotent upload ID.
2. Obtain owner-scoped bounded upload destinations for expected MIME/size/hash.
3. Stream originals unchanged, resumably where supported; checkpoint progress.
4. Server validates bytes/hash/context, completes reservation once and writes
   durable references. Orphaned temporary objects are reconciled safely.
5. Finalize an immutable snapshot only when every required item validates.
6. Return a durable server acknowledgment/cursor; client may then mark the
   corresponding remote state confirmed, separately from local durability.

Enforce allowance before reserve and re-check ownership/completion validity.
Plan expiration mid-transfer follows a bounded already-reserved completion
policy recorded in the plan; never silently charge overage or discard local
bytes. Upload failures release/expire reservations without double metering.
Temporary signed URLs are not public Report shares.

## Change synchronization

Use versioned server changes/cursors, idempotent operation identities and
compare-and-set revisions for DRAFT updates. Never last-write-wins a seal.
Freeze a signed snapshot independently on each side; distinguish a local
sealed snapshot from a separately server-received/certified observation.
Handle rename, duplicate label, deleted draft and two-device edit conflicts
with recoverable choices. Public/core IDs never derive from Room names.
Tombstones have retention/cursor policies so offline devices cannot resurrect
deleted cloud content. Do not use synchronization to rewrite local history.

The current server has one-Report assumptions while local mode supports
multiple immutable Report identities. Migrate the server representation and
test legacy IDs before promising complete parity or restoring snapshots.
Do not flatten the local report set into a single overwritten server PDF.

## Restore

List authorized snapshots with completeness, versions, size and retention
status. Download into a staging area, check all hashes/relationships and
atomically import or surface a partial/error state. Handle existing IDs,
disk full, process death, corrupt objects, interrupted downloads and new keys.
Restoring a purchase and restoring collection bytes are separate operations.
Test a clean device and independent package verification after restore.

## Quota, expiration and privacy

Monthly plan limits cover hosted bytes, requests and remote services, never
local capture or PDF. Show actual usage and reserve headroom for recovery.
No hidden overage billing. Capacity upgrade is voluntary and store-priced.
After expiry, stop new paid operations, preserve local use and apply the
published recovery/export/retention period. Do not require resubscription
merely to submit a lawful data-right request or manage/cancel the purchase.

Hosted Share links and publication are paid publisher operations. Recipient
read/verification is authorized and rate-limited, not recipient-billed.
Revoke on next request, acknowledging already-downloaded bytes cannot be
recalled. Public verification must not expose private originals or precise
GPS/CPF by default. Keep storage private and URLs short-lived.
See [privacy and retention](commercial_privacy.md).
