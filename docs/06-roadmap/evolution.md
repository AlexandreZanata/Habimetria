# Evolution — V1.1 → V1.5 (no ERP creep)

- **V1.1 Occurrences:** `+ Register occurrence` on timeline. Never edits sealed inspection.
- **V1.2 Exit inspection:** repeat photos per room/position.
- **V1.3 Compare:** side-by-side + slider, tags `no_change|changed|wear`.
- **V1.4 AI inspector:** coverage gaps + `possible stain/crack` suggestions with Confirm/Ignore.
- **V1.5 AI diff:** "visual difference found" list. Never assigns liability.
- **V-sync (optional):** mirror occurrences, comparison tags and exit to the API when online. Local stays the source of truth; offline never becomes `confirmed`.

Guardrail: if a request adds roles (broker, admin, approver) → reject from core, park in `non-goals`.
