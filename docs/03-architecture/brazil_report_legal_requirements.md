# Brazilian Report — Legal Applicability and Official Sources

Research checked 2026-10-02. Companion to [the PDF plan](../06-roadmap/local_pdf_plan.md).
Scope: residential tenancy and architecture/engineering Reports in Brazil.
Distinguish statutory duties, conditional professional duties and product
engineering choices. This is a source-backed baseline, not an exhaustive
certificate of compliance for every municipality, specialty or proceeding.

## Electronic evidence and authorship

[MP 2.200-2/2001, article 10](https://www.planalto.gov.br/ccivil_03/mpv/antigas_2001/2200-2.htm)
recognizes electronic documents; ICP-Brasil provides a presumption regarding
signatories. Other authorship/integrity methods may be admitted by the parties
or accepted by the recipient. An installation key with unverified ownership
does not establish a person's identity. Preserve actual signing evidence.

[CPC, articles 369, 371, 384, 422 and 439–441](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2015/lei/l13105.htm)
supports electronic/photographic evidence subject to assessment and challenge.
Article 422 §1 addresses authentication or expert examination of challenged
digital photos. Keep originals and verification material. A notarial record
is an optional additional measure, not a universal app requirement.
Judicial expert reports fall under article 473: object, technical analysis,
method and answers to the submitted questions. An app template alone does
not make a private inspection a court-appointed expert report.

[Law 14.063/2020, articles 2 and 4](https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2020/lei/l14063.htm)
defines simple/advanced/qualified signatures. Chapter II's scope excludes
private-to-private interactions and judicial proceedings; do not use it as
an automatic blanket rule requiring qualified signatures for tenancy PDFs.
An image of a signature or a QR is not a cryptographic digital signature.

Product implication: no automatic “official validity”, “court acceptance”
or certified scene-truth badge. Identify exactly what was signed, by whom,
under what identity evidence, and which integrity checks were performed.

## Ordinary residential tenancy

[Tenancy Law 8.245/1991, article 22 V](https://www.planalto.gov.br/ccivil_03/leis/l8245compilado.htm)
requires the landlord, when requested, to provide a detailed description of
the property's condition at delivery, expressly identifying existing defects.
The cited provision does not prescribe a universal PDF template or GPS field.
Contract obligations can add requirements beyond this statutory baseline.

Recommended product content: address/unit, entry/exit purpose, parties/roles,
reported inspection date, room-by-room condition, evidence-linked defects,
coverage gaps, inaccessible areas and a contract reference where available.
Document explicit receipt, disagreement or agreement if collected; opening
or sharing a file is not agreement. Signatures by the relevant parties can
strengthen the record but are not asserted here as a universal article-22 duty.
Two witnesses and notarization are not universal requirements for every
photographic tenancy Report. Do not infer structural safety from photos.

## Engineering and architecture services

[Law 6.496/1977, articles 1–2](https://www.planalto.gov.br/ccivil_03/leis/l6496.htm)
requires ART for contracts for covered engineering/agronomy services and
uses ART to define their technical responsibility. The app cannot register
an ART by merely printing a number.
[Confea Resolution 1.137/2023](https://normativos.confea.org.br/Ementas/Visualizar?id=76099)
provides the registration/acervo framework; use the official consolidated
text and relevant CREA procedure at implementation time.

[Law 12.378/2010, articles 2 VI, 5 and 45](https://www.planalto.gov.br/ccivil_03/_ato2007-2010/2010/lei/l12378.htm)
covers architectural inspections/expert reports and the professional/RRT
framework. [CAU's official RRT guidance](https://transparencia.caubr.gov.br/perguntas-frequentes-registro-de-responsabilidade-tecnica-rrt/)
explains registration modalities, including these technical activities.
Applicable registration must match the responsible professional and work.

Professional release requirements: identify the author and registration,
confirm appropriate professional competence, collect the applicable registered
ART/RRT and freeze its reference/attachment digest. An offline entry or saved
certificate is declared/cached information unless independently checked;
disclose the check source/date rather than promising current council status.

Recommended template content: contracting party, object/scope, property,
inspection dates, methods, documents reviewed, Evidence/Findings, limitations,
reasoned analysis, conclusions and recommendations, references and authorship.
These fields are an engineering recommendation pending the selected standard,
contract and recipient rules, not a quoted universal statutory checklist.
Professional conclusions require review and signature by the responsible author.

## Standards and locality — unresolved applicability gate

[ABNT's official catalog](https://www.abntcatalogo.com.br/)
lists NBR 13752:2024 for engineering expertise in civil construction.
Building-inspection scope must also assess NBR 16747 in its current edition;
[IBAPE's publication index](https://biblioteca.ibape-nacional.com.br/abnt-nbr-16747-inspecao-predial/)
identifies that standard, but does not substitute its licensed full text.
Full clause-level requirements and current amendments were not established
from the public pages consulted. Do not claim conformity to either standard
until its licensed text, scope and edition have been reviewed.

Before professional compliance claims, enumerate supported work types and
municipalities/states; check inspection/building-maintenance laws, recipient
submission rules and specialty requirements. Track rule, authority, edition,
effective date, applicability and matching acceptance gate. No city was
specified, so this research cannot establish all local inspection obligations.
Ordinary tenancy photographs and regulated building inspections are different
scopes even when produced by the same app.

## GPS, EXIF and time

Inference limited to the general tenancy/electronic-document provisions
above: no universal per-photo GPS, altitude, map or EXIF requirement was
identified. Specific technical scopes or contracts can impose extra fields.
Coordinates add context but do not independently prove the scene's location.
EXIF and device clock are editable claims; hashing preserves the claim,
not its truth. Geolocation engineering fields live in
[the rendering contract](../05-mobile/local_pdf_contract.md).

[ITI's timestamp guidance](https://www.gov.br/iti/pt-br/acesso-a-informacao/perguntas-frequentes/carimbo-do-tempo)
states that a trusted timestamp records document existence at arrival to
the issuing authority, not its original creation date. A later ACT token
cannot retroactively certify the shutter time. Its issuance needs the
external trusted authority; it is not an offline device-clock feature.

## Signature validation and archival choices

[ITI VALIDAR](https://validar.iti.gov.br/) checks supported signatures for
signer identity and changes after signing, not the truth of the photographed
scene. App-specific detached ECDSA receipts are not automatically a supported
ITI signature format. [ITI's principal documents](https://www.gov.br/iti/pt-br/assuntos/legislacao/documentos-principais)
are the implementation authority for an optional ICP-Brasil/PAdES profile.

A certificate-backed signature can be computed locally when its authorized
key is accessible; identity enrollment, new revocation information and ACT
timestamps require external trust information. Gov.br signing is an optional
connected service, not a locally manufactured government signature.
PDF/A is an archival option, not a universal condition of evidentiary value.
Native PDF drawing alone establishes neither PDF/A nor PAdES conformity.

[Decree 10.278/2020, article 2 sole paragraph I](https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2020/decreto/d10278.htm)
excludes born-digital documents from its physical-document digitization scope.
Do not import its scanning resolution/metadata rules as universal rules for
camera-native Evidence or newly generated Reports. Scanned paper attachments
require separate applicability assessment.

## Personal data and preservation

[LGPD, articles 4, 6, 7, 15–16 and 46](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm)
requires purpose, necessity, a suitable legal basis, retention assessment and
security for covered processing. Exclusive private non-economic natural-person
use has an article-4 exception; a commercial app/service must assess its own
processing. Device permission is not itself a legal basis. Avoid unnecessary
faces, identity-document images and public disclosure of precise coordinates.

Define controller/operator roles for supported deployments, notices, lawful
retention and rights handling. Contract/exercise-of-rights bases may apply;
consent is not the sole possible basis. Keep private storage, protected keys,
controlled exports and user-managed backups. Preserve immutable historical
Evidence while retained; immutability does not justify indefinite retention.
Any redacted/share copy gets a new identity and is labeled as derived.

## Claims the product may make after acceptance

- Available offline: generation, measured hashes, signature verification and
  comparison against independently trusted public material.
- Conditional: identified signer, valid ART/RRT or certificate status, with
  the source, scope and freshness of the verification disclosed.
- Never automatic: scene authenticity, exact capture location/time, technical
  safety conclusions, public-document status or universal legal acceptance.
