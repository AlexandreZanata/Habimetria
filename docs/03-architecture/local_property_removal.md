# Local Property Removal — Planned Contract

Status: U07 implemented (visibility v14 + transactional delete/archive/restore);
U08 implemented (Imóveis confirmation, durable feedback, navigation revalidation);
U09 implemented (archived filter/restore, creation guard, history labels,
shared startup trail validation). Companion to the
[UX contract](../02-product/local_navigation_ux.md) and
[execution backlog](../06-roadmap/local_usability_plan.md).

## Removal policy

- A Property with no Tenancy, Inspection, Occurrence or optional mirror
  reference may be permanently deleted after explicit confirmation.
- A Property with history exposes `Arquivar imóvel`. Archiving hides it
  from active Property pickers and lists and permits `Restaurar imóvel`.
  Existing DRAFT Inspections remain resumable through archived history.
  Creating a new Inspection requires restoring the Property first.
- Archiving preserves every related row, original, Manifest, Report,
  receipt, comparison and mirror link. It does not change signed bytes.
- SEALED remains terminal. Never cascade deletion through an Inspection
  or represent archive visibility as a new Inspection state.
- Permanent deletion of populated Properties is outside this slice; it
  requires a separate retention/export and interrupted-file-cleanup design.

This provides a removal action without silently discarding evidence.
Archive is reversible organization, not storage cleanup or server deletion.

## Shared implementation boundary

Implement eligibility/delete/archive/restore in `vault.local`, reached
through `LocalSession`/repository callbacks. UI never sends raw delete SQL
or calls `VaultApi`. Use `vault.local.db` for transactional operations.

Add a local Property visibility field with an explicit forward Room
migration from the current schema (v13 at planning time) and a committed
schema export. Existing Properties migrate as active. Keep this field out
of frozen Manifest payloads and preserve existing hashes and signatures.
Review `LocalVault`, `VaultDaos`, `VaultEntities`, `VaultDatabase`,
`VaultMigrations` and platform database builders before assigning a version.

Recheck eligibility inside the same transaction as deletion: a stale UI
count cannot authorize removal after a Tenancy or sync reference appears.
Use existing foreign-key protections, never introduce destructive cascade.
Return explicit missing/blocked/failure results; repeated requests and
double taps are idempotent. Do not turn storage failures into empty lists.

For archive/restore, commit the visibility change before refreshing UI.
For deletion, commit DB removal first, then invalidate navigation/picker
references to that Property. DB and `progress.json` are separate storage:
revalidate restored IDs at startup to recover interruption between them.
Do not clear unrelated Inspection progress. Empty deletion never removes
files; archive never invokes original/derivative cleanup.

## Confirmation and feedback

- Empty Property: `Excluir imóvel?`, Property name/address, explanation
  that its empty local registration will be deleted, `Cancelar`/`Excluir`.
- Property with history: `Arquivar imóvel?`, explicit preservation of
  vistorias/fotos/relatórios, `Cancelar`/`Arquivar`.
- Show successful feedback only after durable commit. Keep the item and
  dialog context on failure, with an actionable retry message.
- Offer archived filtering and restore after restart, not just a transient
  undo message. Restore uses the same stable Property ID.
- Optional server links stay intact. Copy explains local scope and must
  never imply remote deletion or server confirmation.

## Required regression proof

Test empty deletion, populated deletion rejection, archive/restore after
DB reopen, legacy migration, stale eligibility/concurrent creation,
double taps, injected write failure and stale navigation restoration.
Compare hashes and independently verify a sealed package before/after
archive and restore. Include optional mirror references and ensure
`LOCAL_ONLY` operations require no network.
