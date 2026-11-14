# Commercial Configuration — Provisioning and Exposure Rollback

Authority: [configuration contract](commercial_configuration.md),
[operations](commercial_operations.md) and
[release gates](../06-roadmap/commercial/release_gates.md).
C06 prepares internal adapter configuration; there is no purchase endpoint,
store client or native checkout to activate in this delivery.

## Provision a reviewed namespace

1. Keep both providers and new purchases false for default/local operation.
   No secret mount, store identity or provider network call is required.
2. Prepare an isolated sandbox deployment and its independently verified app
   identities. Match actual app/store values; never change vault.app or invent
   an iOS publisher identity just to satisfy configuration. Production commerce
   requires APP_ENV=production and the existing strong server-signing settings.
3. Provision regular credential files under an operator-controlled root with
   `<environment>/<store>/<filename>` layout. Files must be nonempty, at most
   1 MiB, mode 0400/0600, and readable by the API identity. Mount read-only;
   the API must not be able to rewrite credentials. References are metadata,
   not raw secret contents. Do not put credentials in git, images or env values.
4. Namespaces/store directories/files may not be symlink aliases. Projected
   secret layouts using symlinks require a controlled regular-file provisioning
   step or a later dedicated adapter; do not weaken namespace checks ad hoc.
5. Set only the configured provider flag true. Start/restart the deployment
   with new purchases still false. Config syntax validates at startup; the
   internal activation gate opens the required credential only on adapter setup.
   Missing/unreadable/wrong-scope credentials fail that setup with a redacted
   error, never a fabricated provider success or paid entitlement.

The optional [Compose overlay](../../infra/docker-compose.commerce.yml) mounts
an explicit host directory read-only and never creates a missing source path:

```sh
docker compose -f infra/docker-compose.yml -f infra/docker-compose.commerce.yml config --quiet
docker compose -f infra/docker-compose.yml -f infra/docker-compose.commerce.yml up --build -d api
```

Set COMMERCE_SECRETS_SOURCE_DIR to the reviewed absolute host directory first.
The default stack omits this overlay. Config-only validation does not prove
that the source exists, is private, or belongs to the configured publisher.
Provision/inspect those separately before an activated adapter is deployed.

## Internal activation and verification

The [activation package](../../backend/internal/service/commerce/activation/README.md)
retains a validated value snapshot. Environment changes affect a new snapshot
after redeployment, not requests already running under an old process.
It compares configured store/environment/app identity, including Apple's numeric
app ID, before opening credential files. A client claiming the correct strings
is not verified store proof or permission to private data.

Credential traversal uses pinned directory handles and rejects alias/traversal,
non-private modes and non-regular/oversized files. Returned readers are bounded
to the inspected file size and must be closed. [Go Root](https://pkg.go.dev/os@go1.25.0#Root)
provides directory confinement; namespace identity checks add explicit scoping.
Logs/JSON formatting redact credential readers and error messages do not echo
paths or supplied values. Underlying errors remain inspectable for diagnosis.

Gate preparation is separate from credential parsing, provider permission
checks, transaction verification, ledger, ownership and offer eligibility.
Those checks belong to their planned adapters/services. C06 performs no remote
provider call and cannot determine whether a supplied key is accepted there.

## Pause new purchases without stopping recovery

1. Set COMMERCE_NEW_PURCHASES_ENABLED=false and redeploy the reviewed build.
   Keep provider flags, matching namespaces, secret mounts and identities intact.
2. New-purchase adapter preparation must be unavailable. Delivery, restore,
   reconciliation and notifications retain credential access for enabled stores.
   This keeps pending delivery/recovery viable once those flows are implemented;
   C06 does not implement or simulate their provider/ledger outcomes.
3. Verify account controls, existing authorized recipients, private ownership
   enforcement and the full free local journey. Do not wrap them in this gate.
4. Preserve stored transaction/catalog history, signing keys and SEALED bytes.
   Pausing sale exposure never cancels a store subscription or recalls a PDF.

Disabling a provider is a distinct incident action that also stops its adapter
preparation. Pause new purchases first; do not leave sales true with no enabled
store. Credential/namespace outages need recovery and reconciliation, not a
fallback to a different environment or a fake paid state.

For rotation, provision a new private file/reference in the same namespace,
validate a separate deployment and redeploy atomically. Existing open readers
keep their inspected object; later preparation uses the new reference/object.
Provider acceptance, permission revocation and real production rotation remain
manual qualification gates. No rotation or sale activation was executed by C06.

## Repeatable deployment configuration check

`make verify-commerce-config` runs [the Compose regression](../../infra/verify_commerce_config.py)
with synthetic values and an empty temporary mount source. It renders the base,
configured sandbox, paused-sale and isolated-provider profiles without starting
containers or contacting a store. It excludes ambient project .env inputs and
checks exact identity/reference forwarding, no default secret mount, explicit
read-only/non-creating bind semantics and recovery-preserving sale rollback.
The backend CI runs this check. It is not evidence of real secret provisioning,
provider access or a successful store transaction.
