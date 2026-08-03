# RMC Booking System — Docs index

Normative product and engineering documentation for building and proving the RMC Booking System.

| Document | Role |
|---|---|
| [`RESEARCH.md`](./RESEARCH.md) | Product research baseline — scope, decisions, journeys, design direction |
| [`prototype/rmc-booking.html`](./prototype/rmc-booking.html) | **Visual source of truth** — clickable guest + staff prototype |
| [`SPEC.md`](./SPEC.md) | Greenfield build contract — `RMC-SPEC-*` requirements + appendices |
| [`SPEC_GUIDE.md`](./SPEC_GUIDE.md) | Spec structure map — ID format, section/appendix index, where to change what |
| [`VALIDATION.md`](./VALIDATION.md) | Acceptance criteria (`AC-*`) mapped to SPEC IDs |
| [`AC_COVERAGE.md`](./AC_COVERAGE.md) | Which test/manual/doc gate owns each AC |
| [`VALIDATION_RUN.md`](./VALIDATION_RUN.md) | Dated PASS/FAIL/PARTIAL evidence only |

## How they relate

```text
RESEARCH  →  SPEC  →  build  →  VALIDATION (AC-*)  →  VALIDATION_RUN (evidence)
                ↑                      ↑
         RMC-SPEC-* IDs         AC_COVERAGE + tests
                ↑
         prototype/rmc-booking.html (visual SoT)
```

## Rules of thumb

1. Change **product intent** in RESEARCH first, then SPEC.  
2. Every feature needs a `RMC-SPEC-*` ID (and sub-IDs) before implementation.  
3. “Done” means relevant `AC-*` pass with evidence in VALIDATION_RUN — not a status column in SPEC.  
4. Configurable hotel knobs (tax %, cutoffs, branding colors) are settings; payment truth and holds are not.  
5. Guest/staff UI must match `docs/prototype/rmc-booking.html` (`RMC-SPEC-UX-001.10` / `AC-UI-001`; interaction fidelity `UX-001.17`–`.19` / `AC-UI-004`).  
6. For “where do I look in SPEC?”, start with [`SPEC_GUIDE.md`](./SPEC_GUIDE.md).

## Current versions

| Doc | Version |
|---|---|
| RESEARCH | 1.0.3 |
| SPEC | 1.0.11 |
| VALIDATION | 1.6 |
| Spec guide | `docs/SPEC_GUIDE.md` |
| Visual prototype | `docs/prototype/rmc-booking.html` |