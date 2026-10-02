# Report Authorship — Code Delivery and Manual Qualification

Date: 2026-10-02. Branch: `codex/legal-report-hardening`.
Baseline: `ef1736c`. Normative scope:
[authorship contract](../05-mobile/report_authorship_contract.md).

## User-directed validation policy

Deliver source code/documentation; run code tests and Android compilation.
Do not launch an emulator, install on a physical phone, execute native PDF
probes or generate/render visual PDF samples in any current phase. The user
performs manual validation after all phases. iOS checking is excluded.
Historical native measurements in [earlier execution](local_pdf_execution.md)
cover their earlier build only and do not qualify these changes.

## Implemented phases

1. Optional ordinary-user identification, supplied CPF check digits,
   masking/full-display choice and complete-data ZIP disclosure. Professional
   author/title/council requirements remain; CPF/contact remain optional.
2. Manifest v4 freezes context/date/company/summary/keywords/references and
   professional attribution/registered responsibility/applicability declarations.
   Old Manifest v1–v3 and receipt v1 verification retain their signed bytes.
3. Corporate editorial layout: A4, 30 mm left/top, 20 mm right/bottom, 12 pt
   body, navy headings/black body, summary, numbered sections, contents with
   physical page references and analysis/conclusions after photographic evidence.
   No blanket ABNT certification is emitted.
4. Explicit Report-specific AUTHOR/RECEIVED/AGREED/DISAGREED declarations,
   signed index, exact file set/order and previous-file hash chain. Receipt v2
   requires the index even when empty. PDF/Manifest/original receipt stay
   unchanged. Fresh ZIP export includes additions; old exports stay historical.
5. Optional local A1 PKCS12 holder signing on Android/JVM, subject/fingerprint
   preview, explicit intent, password clearing, bounded temporary imports and
   separate signature/trust/revocation/time status. RSA/P-256 checks reject
   altered payload, wrong passwords, weak/ambiguous/expired-at-device keys.
   No embedded PAdES, Gov.br, ACT or independently verified ICP root is claimed.
6. Independent Kotlin/Go verification and recovery of committed declaration
   bytes only when their staged digest matches a valid Report-bound signed
   index. Received packages never repair themselves. Removal clears local
   declarations/package copies; Android backup is disabled and stale picker
   copies are cleaned at startup.

## Executed code checks

- `make verify`: Go vet and all Go tests passed.
- `cd app && ./gradlew :shared:jvmTest :composeApp:assembleDebug
  :composeApp:assembleDebugAndroidTest`: passed; 178 JVM tests, no failures.
  Android production and instrumented-test sources compile. Instrumented
  tests were not executed and no APK was installed.
- JVM tests create ephemeral synthetic PKCS12 keys, sign test-artifact bytes
  and invoke the independent Go verifier. Test artifacts are not native PDFs;
  no private keys/certificates or real personal data are committed.
- Semantic cases cover optional identity, date/check-digit errors, masked PDF
  model text, complete editorial sections, declaration omission/alteration,
  foreign Report index, chain reorder, credential errors, unknown/pinned holder
  trust, historical exports and injected commit-move interruption/recovery.

## Manual follow-up after all phases (user-owned)

- Inspect full PDF layouts, accents, contents/footer references, long text,
  portrait/landscape/mirrored photos, detail mode, A4 printing and QR readability.
- Rental anonymous/identified entry and exit; professional CREA/ART and CAU/RRT;
  incomplete requirements, frozen metadata and unchanged historical versions.
- Actual supported A1 files/providers, password errors, holder preview,
  independent fingerprint comparison, temporary-copy cleanup and signed ZIPs.
- Physical camera/location permissions, denied/approximate/stale/mock states,
  offline restart, sharing/import, low storage, interruptions and memory.
- Recipient/legal/service applicability, licensed technical standards and any
  required external signature/trust/revocation/timestamp workflow.

All manual checks remain unverified in this delivery. Source tests establish
bounded code behavior, not universal judicial acceptance or full ABNT scope.
