# Classified HTTP registration

- `policy.go`: transport classes and owner-scope metadata; no subscription grant.
- `router.go`: catalog-bound ServeMux registration, unavailable private access
  without a guard, and detached registered-route inventory.
- `router_test.go`: unclassified/invalid registration, missing guards, dispatch
  authority and immutable inspection tests.

API and panel catalogs live in their respective parent packages. See
[hosted entry points](../../../../docs/03-architecture/hosted_entry_points.md).
