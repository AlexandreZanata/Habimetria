# Local PDF — Rendering and Integrity Contract

Status: local core implemented; qualification and external integrations remain
gated. See [implemented behavior](pdf-ondevice.md) and
[execution evidence](../06-roadmap/local_pdf_execution.md).
Implements [the focused plan](../06-roadmap/local_pdf_plan.md), under the
[local architecture](../03-architecture/local_validation_architecture.md).
No API/server rendering dependency is introduced.

## Photo orientation and dimensions

Read encoded pixel dimensions and EXIF orientation from the immutable source.
Treat orientation 1–8 as a complete transform: identity, horizontal mirror,
180-degree rotation, vertical mirror, transpose, 90-degree rotation,
transverse and 270-degree rotation. Dimensions swap for orientations 5–8.
Do not infer orientation from the current screen rotation or width alone.

Android: prefer [AndroidX ExifInterface](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface)
and a tested rotation/mirror transform applied after sampled decode.
iOS: use bounded ImageIO decoding with
[kCGImageSourceCreateThumbnailWithTransform](https://developer.apple.com/documentation/imageio/kcgimagesourcecreatethumbnailwithtransform)
or an equivalent explicit transform; never apply orientation twice.
Normalize derivatives even when the original is already small. JPEG/HEIF
support must be explicitly qualified per adapter; unknown formats fail clearly.

Original bytes and their hashes never change. Store orientation, encoded/display
dimensions and any user-selected display correction as Evidence metadata;
freeze relevant new fields in the next Manifest version. Corrections are
DRAFT-only, explicitly indicated and applied to derivatives, not the original.
New renderings of old snapshots cannot silently add unsealed user edits.

All grid, review and PDF adapters consume the same display convention.
Previews are disposable caches, not authorities: regenerate from hash-checked
originals, or validate cache digest/source digest/transform version before use.
Do not trust an existing preview merely because its path exists.

For display size `(iw, ih)` and available box `(bw, bh)`:
`scale = min(bw / iw, bh / ih)`; draw `(iw * scale, ih * scale)` centered.
Changing maximum height also changes width. Use contain, never cover/stretch.
Leave whitespace rather than crop Evidence. A4 portrait is the default page;
landscape Evidence stays a landscape image within it. No manual display-correction editor is shipped in this slice. Provide larger detail
pages for professional Evidence without replacing the original attachment.

## Page composition and output quality

1. Cover: title/purpose, address/unit, Inspection/Report IDs, author/roles,
   reported dates, presentation choice and concise verification limits.
2. Summary: Room index with actual page references, evidence/Findings counts,
   coverage and explicit inaccessible/non-applicable categories.
3. Room sections: stable Evidence reference/category, intact photo, complete
   caption/notes, device capture time and per-photo location status.
4. Professional sections when applicable: scope, method/standards, analysis,
   conclusions, recommendations and ART/RRT reference/attachment information.
5. Verification appendix: full Manifest/original digests, key fingerprint,
   renderer/layout version, methodology, QR identity and receipt instructions.
6. Signing/acknowledgment statements: identify exactly whose action is recorded;
   empty signature lines, sharing and QR scans do not count as signatures.

Use restrained branding, whitespace, black body text, a navy accent, consistent
margins and embedded/local font resources when required. Body/captions should
be comfortably legible in A4 print; complete digests can use compact monospaced
text. Use 12 pt body text, 30 mm left/top and 20 mm right/bottom margins.
Number textual sections and show declared references; smaller captions and
source/footer text remain measured. Manual print/layout review is user-owned
after this code-only delivery. See [editorial scope](report_authorship_contract.md).
Keep all Portuguese accents and all original notes; do not use `.take()` to
silently discard Report content. Separate technical identifiers into appendix
where they do not help the reader understand an observation.

Measure text using the drawing font, wrap and split long paragraphs, reserve
caption/location/footer height, and keep photos with their reference labels.
Prepare layout with actual physical page count before final numbering.
Continuation pages receive unique sequential numbers and valid index entries.
Extraction checks complement rendered-page inspection; neither alone proves
that every correct photo was embedded. Verify both image identity and placement.

Use a documented render-quality preset selected before finalization. Keep
1280 px as the existing baseline, not a legal requirement; higher detail may
need a larger derivative. Do not blur a defect to satisfy a file-size target.
No generative enhancement, invented detail or scene manipulation is allowed.
Color/rotation/compression operations must be documented derivatives.

## Per-photo geolocation and device-time claims

Required product metadata, where exposed by the platform: latitude/longitude
(WGS84 decimal degrees), horizontal accuracy meters, fix timestamp and age at
shutter, capture timestamp with UTC offset/timezone, provider/source, permission
accuracy level, location availability and nullable simulation/accessory flags.
These are engineering requirements, not universal statutory GPS obligations.
Altitude/vertical accuracy are optional for a scoped technical need; heading,
speed and reverse-geocoded maps are not mandatory for this Report scope.

Bind the cached fix to the individual Evidence at capture, not PDF time.
Retain the existing 30-second freshness threshold as an initial policy;
display older/unknown-age fixes as stale/unknown. Permission accuracy and
measured accuracy are separate. Validate finite values, latitude/longitude
ranges, nonnegative accuracy and timestamp semantics; absence is null, never
a synthetic `(0,0)`. A genuinely measured `(0,0)` is valid, not a missing marker.

On Android use [Location](https://developer.android.com/reference/android/location/Location)
monotonic age and `isMock()` when supported. Its accuracy is an estimated
radius, not a guaranteed maximum error. On iOS inspect
[sourceInformation](https://developer.apple.com/documentation/corelocation/cllocation/sourceinformation)
and [isSimulatedBySoftware](https://developer.apple.com/documentation/corelocation/cllocationsourceinformation/issimulatedbysoftware)
when available. Unsupported is unknown, not false. A negative detection does
not certify that spoofing is impossible; retain old flags as legacy claims.

Capture never waits for or loses a photo to GPS. Collect only foreground
location, distinguish denied/services-disabled/no-fix/approximate/stale states
and keep working offline. EXIF coordinates/time stay separately labeled;
document discrepancies rather than replacing sensor claims. No universal meter
threshold is a legal location guarantee. Decimal precision is not accuracy.

Print “Localização informada pelo dispositivo”, measured accuracy and freshness,
or the specific reason it is unavailable. A professional can report a manual
address correction as a separate claim before seal. GPS does not prove that
the scene itself is at those coordinates. Never stamp GPS onto the original.

## Sealing, generation and independent verification

Validate canonical schema/version and Manifest signature with the historical
public key before reading its Report inputs. Read only frozen substantive
fields, not editable Property/author data. Verify every original's size/hash
and bounded decoding; prevent capture/export races through immutable inputs.
Missing/corrupt/undecodable Evidence blocks final success, never a verified
Report containing an unavailable-photo replacement. Explicit drafts are
clearly labeled, separate artifacts without final-verification claims.

Finalize the native PDF writer/context; save complete bytes to a temporary
durable file, atomically publish them, then hash those exact bytes. Sign a
detached receipt binding Report ID, Manifest ID/digest, PDF digest, signer and
format/purpose. Publish the receipt atomically and commit READY only after
local verification; reconcile interruptions between files/DB at restart.
Hashing/signing correctly rendered bytes does not itself verify scene truth.

Never embed the final PDF's whole-file digest inside that same PDF. It can
contain the Manifest digest and QR identity; the exact PDF hash is detached.
Report identity is immutable once READY. A changed layout/signature creates
new Report bytes and a new ID, preserving prior versions and SEALED.
SEALED never returns to DRAFT. Corrections use Occurrence/Addendum.

Offer plain PDF for reading, PDF + receipt for byte verification, and a full
Evidence package for original verification. Explain the differences at export.
Use the existing package layout and ECDSA P-256 contract; version extensions
with backward-compatible verification of old artifacts. Never export keys.
An external PAdES signature changes PDF bytes: preserve the earlier artifact
and issue/register a new Report/receipt against the final signed file.

Verifier results remain separate: PDF integrity, Manifest integrity, original
integrity, signature validity and issuer identity/trust. A self-included key
needs independently trusted fingerprint/certificate material. Offline trust
status includes freshness; fresh revocation/ACT/government signing are optional
external steps. Hashes/QR/password restrictions are not signer identity.
Keep historical public keys; new installation keys never replace old ones in
verification. A valid signature from an unknown issuer stays unknown.
