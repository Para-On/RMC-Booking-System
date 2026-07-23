# VALIDATION_RUN — Evidence log

**Role:** Dated PASS / FAIL / PARTIAL evidence against `docs/VALIDATION.md` `AC-*`  
**Current companions:** `docs/RESEARCH.md` v1.0.3 · `docs/SPEC.md` v1.0.11 · `docs/VALIDATION.md` v1.6 · `docs/prototype/rmc-booking.html` · `docs/AC_COVERAGE.md`  
**Not:** a substitute for SPEC or RESEARCH. Do not invent requirements here.

---

## How to use

1. Implement against `docs/SPEC.md` / prove with `AC-*` in `docs/VALIDATION.md`.  
2. A SPEC requirement is PASS only when its mapped AC(s) PASS with coverage owner + evidence here.  
3. Record each meaningful proof pass as a dated section below (newest first under **Current**).  
4. Cite commands, commit SHAs, and owners from `docs/AC_COVERAGE.md`.  
5. Keep **Archive** sections for historical context; they do not override SPEC v1.0 / VALIDATION v1.2.

---

## Current — Checkout CTAs, General settings, StaffModal

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.17` … `.19`  
**Validation:** `docs/VALIDATION.md` v1.6  

### Verdict

**PARTIAL — Checkout padding + Next/Maya/pay-at-hotel CTAs; General settings cards; StaffModal demo; MANUAL PASS not yet recorded**

### Evidence

- Checkout: `max-w-6xl`-style `.checkout-page` padding; Back + Next until last step; last CTA switches Maya vs pay-at-hotel; `[hidden]` beats `.btn` display
- Staff General: taxes, system config, rate plans, room types/units, MFA cards
- StaffModal: backdrop + header/body/footer on Promos create/edit (`RMC-SPEC-UX-001.19`)
- Refund policy page restored in SoT STAFF_PAGES

---

## Current — Checkout step isolation + refund policy stub

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.17`, `.18`  
**Validation:** `docs/VALIDATION.md` v1.5  

### Verdict

**PARTIAL — One-step checkout panes + live stepper spacers + refund-policy field stub; MANUAL PASS not yet recorded**

### Evidence

- Checkout panes use `.co-pane.is-active` (only one step visible); shared Back/Continue/Place booking
- Wizard has leading/trailing pads like `RoomTypeWizardStepper`
- Staff refund policy: name, enabled, cutoff+unit, partial %, check-in, timezone, description, manual refund toggle
- Hero Find rooms + filter Search use booking CTA height (`h-12`) with full-width min-width reset in bar

---

## Current — Checkout + control sizing pass

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.17`  
**Validation:** `docs/VALIDATION.md` v1.5  

### Verdict

**PARTIAL — Checkout rebuilt to live panel/sidebar pattern; CTAs h-12; guest cards borderless; MANUAL screenshot PASS not yet recorded**

### Evidence

- Checkout: step card + sticky sidebar with hero, panel intros/icons, pay options with check rings, shared Back/Continue actions
- Buttons: primary booking CTAs `h-12 min-w-9rem text-base`; stack card CTAs `sm`
- Guest `.card`: shadow only (no 1px stroke), matching live `.guest-shell .card`

---

## Current — Guest home pixel pass

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.13`, `.14`, `.17`  
**Validation:** `docs/VALIDATION.md` v1.5  

### Verdict

**PARTIAL — Guest home SoT rewritten against live SearchPage/HotelHero/BookingFilters/RoomSearchCarousel; MANUAL screenshot PASS not yet recorded**

### Evidence

- Hero: MapPin location, Find rooms CTA, LG vertical center, staggered slide-in, gradient
- Filters: absolute overlay bar with Hotel/Dates/Guests cells + Search
- Explore: centered display title; `lg` ⅓ card width; dots only; stack card footers
- Icons: Lucide-path SVGs (guest filters + staff nav/topbar)
- Scrollbars: thin 6px; guest page bars hidden
- Logo demo: `docs/prototype/assets/ramada-manila-central-logo.png`

---

## Current — SoT interaction fidelity (`UX-001.17` / `.18`)

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.17`, `.18`  
**Validation:** `docs/VALIDATION.md` v1.5  

### Verdict

**PARTIAL — Prototype deepened for remaining gaps; MANUAL screenshot PASS not yet recorded**

### Evidence

- Guests +/- counters (rooms/adults/kids); room gallery dots; horizontal carousel + prev/next/dots
- Checkout numbered stepper; home reveal + page-enter
- Staff: topbar scroll-hide, body enter, multi-chart dashboard, filter bars, catalog wizard, booking ops strip + audit

---

## Current — SoT gap closure (`AC-UI-004`)

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.10` … `.16`, `STAFF-001.8`–`.10`  
**Validation:** `docs/VALIDATION.md` v1.4  

### Verdict

**PARTIAL — Prototype rewritten for chrome + carousel + seed catalog + avatar menu; MANUAL screenshot PASS not yet recorded**

### Evidence

- Prototype: `docs/prototype/rmc-booking.html`
- Guest: hero `/images/hotel-hero.png`, seed Standard ₱2500 / Deluxe ₱3800, carousel sibling dim + logo, scroll-hide header, nav indicator, logo toggle
- Staff: logo/Hotel slot, Navigation label, collapsible icons, avatar dropdown (Profile / theme / Log out), top bar search + notifications, MFA login step, dark theme
- SoT fidelity scope documented in `RMC-SPEC-UX-001.10` (staff bodies = stubs)

---

## Current — Theme + motion ACs (`AC-UI-002` / `AC-UI-003`)

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.11`, `.12`, `RMC-SPEC-STAFF-001.9`  
**Validation:** `docs/VALIDATION.md` v1.3  

### Verdict

**PARTIAL — SPEC + SoT demo theme/motion; runtime e2e / MANUAL PASS not yet recorded**

### Evidence

- SPEC IDs: `RMC-SPEC-UX-001.11` (staff LIGHT/DARK), `RMC-SPEC-UX-001.12` (hover/motion + reduced-motion)
- ACs: `AC-UI-002` (GAP planned e2e), `AC-UI-003` (MANUAL)
- Prototype: staff profile + footer theme toggle; CTA/row hover; `prefers-reduced-motion` CSS

---

## Current — Visual SoT mirrors live frontend

**Date:** 2026-07-23  
**Kind:** Documentation / visual baseline  
**Spec:** `RMC-SPEC-UX-001.10`  
**Validation:** `docs/VALIDATION.md` v1.2  

### Verdict

**PARTIAL — Prototype expanded to full staff module tree + live DEFAULT_BRANDING; screenshot PASS for `AC-UI-001` not yet recorded**

### Evidence

- Prototype path: `docs/prototype/rmc-booking.html`
- Mirrors `rmc_frontend` guest flow (5-step checkout) and `DEFAULT_STAFF_MODULES` labels/paths
- Live tokens: `#1a1a1a` / `#f4f4f5` / Geist (Inter stand-in in HTML)
- Appendix A hospitality seed still documented as intended DB seed (SPEC 1.0.3)
- Coverage owner: MANUAL (`AC-UI-001`)

---

## Current — VALIDATION v1.1 AC alignment to SPEC v1.0

**Date:** 2026-07-23  
**Kind:** Acceptance-gate completeness (documentation)  
**Spec:** `docs/SPEC.md` v1.0  
**Validation:** `docs/VALIDATION.md` v1.1  

### Verdict

**PARTIAL — VALIDATION now maps critical SPEC rows to ACs; runtime proofs for new/GAP ACs still required**

### What changed

| Item | Detail |
|---|---|
| Pass rule | Documented in VALIDATION: SPEC → AC → coverage owner → VALIDATION_RUN evidence |
| New ACs | `AC-GUEST-007`–`008`, `AC-PAY-010`, `AC-CXL-006`–`007`, `AC-SEC-007`–`008`, `AC-CFG-001` |
| Pointer fixes | e.g. `AC-GUEST-001` → `GUEST-001.1`; `AC-CXL-003` covers `.3`+`.4` |
| Registry | `AC_COVERAGE.md`, `ac-coverage.tsv`, `AcCoverageMatrixTest` updated |

### New ACs initial posture

| AC | Initial status |
|---|---|
| AC-GUEST-007, AC-GUEST-008 | GAP |
| AC-PAY-010 | GAP |
| AC-CXL-006, AC-CXL-007 | GAP |
| AC-SEC-007 | MANUAL |
| AC-SEC-008 | PARTIAL (architecture/doc) |
| AC-CFG-001 | GAP |

Runtime evidence for these was **not** re-executed in this pass.

---

## Prior — SPEC v1.0 documentation alignment

**Date:** 2026-07-23  
**Kind:** Documentation realignment (not a full runtime UAT)  
**Spec:** `docs/SPEC.md` v1.0  
**Research:** `docs/RESEARCH.md` v1.0  
**Validation:** `docs/VALIDATION.md` v1.0 *(superseded by v1.1 above)*  

### Verdict

**PARTIAL — greenfield docs baseline complete; runtime/staging proofs still required for go-live ACs**

### Document set status

| Artifact | Status |
|---|---|
| `docs/RESEARCH.md` | v1.0 product baseline |
| `docs/SPEC.md` | v1.0 greenfield build contract (`RMC-SPEC-*` IDs) |
| `docs/VALIDATION.md` | was v1.0; now v1.1 |
| `docs/AC_COVERAGE.md` | Owners updated; `AC-DOC-005` retired |
| `ac-coverage.tsv` + `AcCoverageMatrixTest` | Registry matches VALIDATION Part 3 |
| `.cursor/rules/rmc-booking-system.mdc` | RESEARCH → SPEC → AC flow |

### AC-DOC review (builder appendices)

| AC | Result | Notes |
|---|---|---|
| AC-DOC-001 | PASS | Appendix F Maya contract sufficient to implement without inventing auth/URLs |
| AC-DOC-002 | PASS | Appendices E / G / H specify status, pricing, holds |
| AC-DOC-003 | PASS | Appendix D + I + J + §4 specify API/RBAC/IA |
| AC-DOC-004 | PARTIAL | Appendix A seed branding set; Appendix M demo profile still a checklist to flesh with concrete seed rows |

### Runtime AC posture (carried forward; not re-executed this pass)

Prior documentation-pass results remain the latest informal posture until a new command-backed run is recorded. Highlights:

| Priority | AC | Latest known | Needed next |
|---|---|---|---|
| P0 | AC-CXL-004, AC-CXL-005 | GAP | Implement + test auto-refund (`RMC-SPEC-CXL-001.5`–`.7`) |
| P0 | AC-PAY-002, AC-PAY-004 | PARTIAL/GAP | Webhook truth + idempotency integration proof |
| P0 | AC-INV-004 | PARTIAL/GAP | Hold concurrency / load proof |
| P1 | AC-GUEST-005 | PARTIAL | Success-page must not claim paid from redirect alone |
| P1 | AC-STAFF-001 | PARTIAL | RBAC denial automation |
| P1 | AC-OPS-004 | UNPROVEN | Staging Maya webhook + auto-refund UAT evidence |
| P2 | AC-OBS-* | UNPROVEN | Structured logs / correlation / alerts |

Full historical AC table: see Archive §A (2026-07-21). Treat that table as **pre-v1.0 evidence**, not a claim against every new SPEC ID wording.

### ID mapping note (v0.5 → v1.0)

| Topic | Old ID (historical docs) | Current ID (SPEC v1.0) |
|---|---|---|
| Auto-refund initiate | `RMC-SPEC-CXL-001.6` | `RMC-SPEC-CXL-001.5` |
| Auto-refund amount | `RMC-SPEC-CXL-001.7` | `RMC-SPEC-CXL-001.6` |
| Auto-refund failure | `RMC-SPEC-CXL-001.8` | `RMC-SPEC-CXL-001.7` |
| Non-Maya out of scope | `RMC-SPEC-CXL-001.9` | `RMC-SPEC-CXL-001.8` |
| Guest UI refund messaging | *(implied)* | `RMC-SPEC-CXL-001.9` |

Delivery / DOC-GAP / rebuild-matrix columns are **retired**. Do not reintroduce them in SPEC.

### Next evidence pass should include

- Exact commands (`mvn test`, frontend build/e2e) and commit SHA  
- Staging Maya webhook UAT (`AC-PAY-002`, `AC-OPS-004`)  
- Auto-refund happy path + failure path (`AC-CXL-004`, `AC-CXL-005`)  
- Hold concurrency (`AC-INV-004`)  
- Updated PASS/FAIL bucket counts  

---

# Archive — historical passes (do not use as current SoT)

## A. 2026-07-21 — Baseline documentation pass (SPEC v0.2 era)

**Environment:** Documentation baseline derived from then-current remote app tree. Not a staging/production validation run.

**Verdict then:** PARTIAL — behaviour documented; production-readiness unproven.

### Summary table (acceptance criteria — historical)

| AC | Result | Notes |
|---|---|---|
| AC-GUEST-001 | PASS | Search flow exists in app structure. |
| AC-GUEST-002 | PASS | Server-side quote/rate computation path exists. |
| AC-GUEST-003 | PASS | Checkout captures guest/consent data. |
| AC-GUEST-004 | PASS | Booking lookup by reference exists. |
| AC-GUEST-005 | PARTIAL | Return/success pages exist; webhook-only truth needs stronger proof. |
| AC-GUEST-006 | PARTIAL | Email outbox exists; delivery env-gated. |
| AC-INV-001 | PASS | Inventory holds part of design. |
| AC-INV-002 | PASS | Hold expiry/release scheduling exists. |
| AC-INV-003 | PASS | Availability config inputs exist. |
| AC-INV-004 | PARTIAL | No explicit concurrency/load evidence. |
| AC-PAY-001 | PASS | Maya session creation path + service test. |
| AC-PAY-002 | PARTIAL | Trust rule documented; staged proof absent. |
| AC-PAY-003 | PASS | Maya webhook security service test exists. |
| AC-PAY-004 | PARTIAL | Idempotency intent present; dedicated proof not recorded. |
| AC-PAY-005 | PARTIAL | Status machine documented; transition proof thin. |
| AC-PAY-006 | PASS | Pending approval / pay-later states exist. |
| AC-PAY-007 | PASS | Booking ledger exists. |
| AC-PAY-008 | PASS | Additional charges feature exists. |
| AC-PAY-009 | PARTIAL | Sandbox/prod separation in env example; prod proof absent. |
| AC-CXL-001 | PASS | Refund-policy snapshot entity exists. |
| AC-CXL-002 | PASS | Refund policy service/tests exist. |
| AC-CXL-003 | PASS | Staff refund service/audit path exists. |
| AC-CXL-004 / 005 | *(added later)* | Auto-refund ACs — GAP until implemented |
| AC-STAFF-001 | PARTIAL | RBAC model exists; denial proof thin. |
| AC-STAFF-002 | PASS | Staff ops surfaces implemented. |
| AC-STAFF-003 | PARTIAL | Module restrictions exist; consolidated denial proof thin. |
| AC-STAFF-004 | PASS | Admin/profile/notification/audit surfaces exist. |
| AC-SEC-001 | PASS | JWT-protected staff auth exists. |
| AC-SEC-002 | PASS | Guest booking rate limiter + test. |
| AC-SEC-003 | PARTIAL | Secret discipline implied; CI scan evidence thin. |
| AC-SEC-004 | PARTIAL | PII-safe log policy not formalized/proven. |
| AC-SEC-005 | PARTIAL | Prod webhook verification posture unproven. |
| AC-SEC-006 | PARTIAL | Edge/TLS/header posture not validated. |
| AC-OBS-001 | UNPROVEN | No formal observability baseline. |
| AC-OBS-002 | UNPROVEN | Correlation/tracing evidence absent. |
| AC-OBS-003 | UNPROVEN | Alerting/metrics posture absent. |
| AC-OPS-001 | PASS | Flyway migrations are schema source. |
| AC-OPS-002 | PARTIAL | Staging/prod deploy posture not exercised. |
| AC-OPS-003 | UNPROVEN | Backup/restore and secrets hosting not validated. |
| AC-OPS-004 | PARTIAL | Historical note: local branch lag vs remote at time of pass. |

### Evidence captured then

| # | Finding |
|---|---|
| 1 | App capability inventory from backend/frontend tree |
| 2 | `.env.example` topology |
| 3 | Guest + staff routes in frontend router |
| 4 | Flyway V1–V36 schema history |
| 5 | Service tests for Maya/refunds/lifecycle/rate limit |

Artifacts introduced in that era (since superseded by v1.0): early SPEC/VALIDATION with status columns; AC coverage registry; Cursor rules.

---

## B. 2026-07-22 — SPEC v0.3–v0.5 doc iterations (superseded)

These addenda recorded rebuild-matrix / Delivery+Doc / spec-first framing. **Superseded by SPEC v1.0.** Kept only for audit trail:

- v0.3: Appendices D–L filled; rebuild fidelity matrix; `AC-DOC-001`…`005`  
- v0.4: Delivery + Doc columns on SPEC rows  
- v0.5: Auto-refund Specced as planned (`CXL-001.6`–`.9` then); `AC-CXL-004` / `005` added  

Do not reintroduce Delivery/DOC-GAP/rebuild matrices into current SPEC.
