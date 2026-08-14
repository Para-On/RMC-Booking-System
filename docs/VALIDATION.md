# RMC Booking System — Validation Report

**Companion to:** `docs/SPEC.md` (v1.0) · `docs/RESEARCH.md` (v1.0.3)  
**Spec ID scheme:** `RMC-SPEC-<AREA>-<NNN>` · sub-IDs `RMC-SPEC-<AREA>-<NNN>.<n>`  
**Validations defined:** (1) Requirements Traceability, (2) Buildability Walkthrough, (3) Testability Conversion  
**Behavioural source of truth:** `docs/SPEC.md` v1.0 (greenfield build contract)  
**Design baseline:** SPEC §3 + `docs/prototype/rmc-booking.html` (visual SoT); runtime branding via staff settings; theme/motion/interaction fidelity per `RMC-SPEC-UX-001.11` … `.19`  

> **Traceability** finds gaps.  
> **Buildability** finds places a builder would guess.  
> **Testability** converts requirements into acceptance criteria (`AC-*`) that prove SPEC.

### Pass rule (how SPEC is proven)

A `RMC-SPEC-*` requirement is **PASS** only when:

1. It exists in `docs/SPEC.md`  
2. At least one `AC-*` below maps to it  
3. `docs/AC_COVERAGE.md` / `ac-coverage.tsv` names an owner  
4. `docs/VALIDATION_RUN.md` records PASS (or MANUAL PASS) with dated evidence  

`AC-*` prove behaviour. They do not track implementation pipeline status.

---

## Part 0 — Document control

| Field | Value |
|---|---|
| SPEC version | 1.0 |
| Research version | 1.0.3 |
| Validation version | 1.7 |
| Behavioural SoT | SPEC v1.0 + appendices |
| Design baseline | SPEC §3 + `docs/prototype/rmc-booking.html` |
| Doc index | `docs/README.md` |
| Alignment note | v1.7 Maya payment evidence (`ARCH-001.4` / `AC-PAY-011`); v1.6 checkout CTAs/settings/StaffModal (`UX-001.17`–`.19`); v1.5 interaction fidelity; v1.4 chrome/brand/seed; v1.3 theme/motion; v1.2 visual SoT |

---

## Part 1 — Requirements traceability

### 1.1 Product surfaces → Spec

| Feature / flow | Spec ID(s) | ✓ |
|---|---|---|
| Guest search | `RMC-SPEC-GUEST-001.1`, §4 | ✓ |
| Room detail + checkout entry | `RMC-SPEC-GUEST-001`, §4 | ✓ |
| Guest checkout | `RMC-SPEC-GUEST-001.3`, `RMC-SPEC-GUEST-001.4`, `RMC-SPEC-PAY-001` | ✓ |
| Booking confirmation / success pages | `RMC-SPEC-UX-001.3`, `RMC-SPEC-GUEST-001.6`, `RMC-SPEC-ARCH-001` | ✓ |
| Booking lookup by reference | `RMC-SPEC-GUEST-001.5`, `RMC-SPEC-NN-001.10` | ✓ |
| Branding on guest UI | `RMC-SPEC-GUEST-001.8`, `RMC-SPEC-UX-001.7` | ✓ |
| Inventory hold + expiry | `RMC-SPEC-INV-001.1`, `RMC-SPEC-INV-001.2` | ✓ |
| Maya online payment | `RMC-SPEC-PAY-001.1`, `RMC-SPEC-PAY-001.2`, `RMC-SPEC-PAY-002.2` | ✓ |
| Pay-at-hotel / pending approval | `RMC-SPEC-PAY-001.4`, `RMC-SPEC-PAY-001.5` | ✓ |
| Refund policy + staff refunds | `RMC-SPEC-CXL-001` … `.4` | ✓ |
| Auto-refund on guest cancel | `RMC-SPEC-CXL-001.5` … `.9` | ✓ |
| Additional charges | `RMC-SPEC-PAY-001.6` | ✓ |
| Staff dashboard / arrivals / bookings / guests / rooms | `RMC-SPEC-STAFF-001.*` | ✓ |
| Settings / promos / branding | `RMC-SPEC-STAFF-001.5`, `.6`, `RMC-SPEC-CFG-001` | ✓ |
| Users / modules / notifications / audit / profile | `RMC-SPEC-STAFF-001.7`, `.8` | ✓ |
| JWT + refresh auth + MFA | `RMC-SPEC-IAM-001`, `RMC-SPEC-SEC-001.1` | ✓ |
| Guest booking rate limit | `RMC-SPEC-SEC-001` | ✓ |
| Hosted checkout only (no PAN) | `RMC-SPEC-NN-001.7` | ✓ |
| Flyway migrations | `RMC-SPEC-DATA-001`, `RMC-SPEC-CICD-001.1` | ✓ |

**Result:** Research feature spine and SPEC v1.0 cover the in-scope product; Part 3 ACs are the proof gate.

### 1.2 Spec → justified production additions

| Spec area | Class | Justification |
|---|---|---|
| Guest / staff / UX IA | Core product | Required booking + PMS surfaces |
| Payments / Maya / ledger | Core product | Money integrity |
| Observability | Production hardening | Incident diagnosis |
| CI/CD / live checklist | Operations | Safe deploy |
| Auto-refund | Core product | RESEARCH D8 / SPEC CXL-001.5+ |

### 1.3 Spec ID ↔ acceptance-criteria map

| Spec ID | AC ID(s) |
|---|---|
| `RMC-SPEC-GUEST-001.*`, `RMC-SPEC-UX-001.3`, `.5`, `.5a`, `.6`, `.7` | `AC-GUEST-001` … `AC-GUEST-012`, `AC-STAFF-003` |
| `RMC-SPEC-PROMO-001.*` | `AC-PROMO-001` … `AC-PROMO-005` |
| `RMC-SPEC-INV-001.*`, `RMC-SPEC-NN-001.3` | `AC-INV-001` … `AC-INV-004` |
| `RMC-SPEC-PAY-001.*`, `RMC-SPEC-PAY-002.*`, `RMC-SPEC-ARCH-001.*`, `RMC-SPEC-NN-001.1` | `AC-PAY-001` … `AC-PAY-011` |
| `RMC-SPEC-CXL-001.*`, `RMC-SPEC-UX-001.4`, `RMC-SPEC-CFG-001.1`, `.1a`, `.1b` | `AC-CXL-001` … `AC-CXL-009`, `AC-CFG-001` … `AC-CFG-004` |
| `RMC-SPEC-IAM-001.*`, `RMC-SPEC-SEC-001.*`, `RMC-SPEC-NN-001.7`, `RMC-SPEC-META-001.4` | `AC-SEC-001` … `AC-SEC-008` |
| `RMC-SPEC-STAFF-001.*` | `AC-STAFF-001` … `AC-STAFF-013` |
| `RMC-SPEC-OBS-001.*`, `RMC-SPEC-ARCH-001.3` | `AC-OBS-001` … `AC-OBS-003` |
| `RMC-SPEC-CICD-001.*`, `RMC-SPEC-LIVE-001.*`, `RMC-SPEC-DATA-001` | `AC-OPS-001` … `AC-OPS-004` |
| Appendices A–M, `RMC-SPEC-API-001`, `RMC-SPEC-UX-001.8` | `AC-DOC-001` … `AC-DOC-004` |
| `RMC-SPEC-UX-001.10` | `AC-UI-001` |
| `RMC-SPEC-UX-001.11`, `RMC-SPEC-STAFF-001.9` | `AC-UI-002` |
| `RMC-SPEC-UX-001.9`, `.12` | `AC-UI-003` |
| `RMC-SPEC-UX-001.13` … `.18`, `RMC-SPEC-STAFF-001.8`, `.10` | `AC-UI-004` |
| `RMC-SPEC-UX-001.20` | `AC-UI-005` |

---

## Part 2 — Buildability walkthrough

| # | Sev | Spec ID | Builder risk | Resolution in SPEC v1.0 |
|---|---|---|---|---|
| B1 | High | `RMC-SPEC-ARCH-001` | Browser success means paid? | Webhook preferred; poll secondary; redirect never alone |
| B2 | High | `RMC-SPEC-INV-001.4` | Double-sell under concurrency | Required; `AC-INV-004` |
| B3 | High | `RMC-SPEC-PAY-001.2` | Webhook verify optional in prod? | Production verification required |
| B4 | Med | `RMC-SPEC-STACK-001.1` | Exact host vendor | Ops choice; product assumes web+API+MySQL |
| B5 | Med | `RMC-SPEC-BIZ-001` | Multi-property? | Out of scope v1 (single-property) |
| B6 | Med | `RMC-SPEC-OBS-001` | What to log/alert | Minimum correlation + structured logs + errors |
| B7 | Med | `RMC-SPEC-SEC-001.2` | PII in logs | PII-safe logging required |
| B8 | Med | `RMC-SPEC-UX-001.10` | Pixel prototype timing | Visual SoT = `docs/prototype/rmc-booking.html` |
| B9 | Low | `RMC-SPEC-TEST-001.2` | Browser regression | E2E required |
| B10 | High | `RMC-SPEC-CXL-001.5` | Auto-refund vs staff-only | Auto-refund normative for eligible online cancels |
| B11 | Med | Appendix D | Exact DTO shapes | Paths/auth normative; OpenAPI export recommended for builders |
| B12 | High | `RMC-SPEC-PAY-002.2` | Amount mismatch on Maya success | Must not confirm; `AC-PAY-010` |
| B13 | Med | `RMC-SPEC-CXL-001.9` | Guest UI claims refunded early | Forbidden; `AC-CXL-006` |

### Residual inputs (not product-scope blockers)

1. Final legal hotel name / logo assets (branding admin can apply)  
2. Exact production hosting vendor  
3. Screenshot-diff threshold when first recording `AC-UI-001` PASS in `VALIDATION_RUN.md`
---

## Part 3 — Testability conversion (acceptance criteria)

Every critical requirement below is **Given / When / Then** and maps to automated or human-owned proof. These ACs are the **acceptance gate** for SPEC v1.0.

### 3.1 Guest booking flow

- **AC-GUEST-001** — *Given* the guest search form, *when* a valid date/occupancy query is submitted, *then* only available room types are returned, each card titled with the **room type name**, with a **From** tax-inclusive total equal to the minimum among active rate plans with complete daily rates, and card media/meta from the **room type**.  
  - Spec: `RMC-SPEC-GUEST-001.1`

- **AC-GUEST-002** — *Given* a room type detail / checkout path with a **selected rate plan**, *when* the guest proceeds with dates, *then* the server computes the quote from that plan’s rates, taxes, fees, promos/promo codes, and selected extras; no client-supplied total is trusted; currency is PHP.  
  - Spec: `RMC-SPEC-GUEST-001.2`, `RMC-SPEC-NN-001.2`, `RMC-SPEC-DATA-001.3`

- **AC-GUEST-009** — *Given* a room type with multiple active rate plans, *when* the guest opens room detail, *then* room-type product is shown and plans are listed as name/price/policy with the lowest-priced plan pre-highlighted; they must select a rate plan before checkout; booking create with inactive or mismatched `ratePlanId` is rejected.  
  - Spec: `RMC-SPEC-GUEST-001.2a`

- **AC-GUEST-010** — *Given* search results for a room type with multiple plans, *when* the home/search card is shown, *then* media/meta come from the **room type** and the price is **From** the cheapest plan.
  - Spec: `RMC-SPEC-GUEST-001.1`, `RMC-SPEC-GUEST-001.2a`

- **AC-GUEST-011** — *Given* the home filter bar promo-type dropdown, *when* the guest selects a type, enters codes, and Applies (special: offer only; corporate/agency: org + offer), *then* availability returns discounted plan prices for in-scope rate plans when type matches and a **success alert** is shown; type mismatch or invalid/expired codes leave rack prices, show a **failure alert**, and clear the applied session promo so the guest can continue at rack.
  - Spec: `RMC-SPEC-GUEST-001.2b`, `RMC-SPEC-PROMO-001.1`, `.3`, `.4`

- **AC-GUEST-012** — *Given* a sellable room with a refund-policy description on the displayed plan, *when* the guest views search cards, room detail, or checkout summary, *then* Refundable and Free cancellation badges include an info control that reveals that description; when plans differ, the card shows policies vary and each plan on detail has its own info control.
  - Spec: `RMC-SPEC-GUEST-001.2c`, `RMC-SPEC-UX-001.17`

- **AC-GUEST-003** — *Given* checkout, *when* the guest submits identity/contact/consent information, *then* a booking and hold are created only if required fields and consent rules pass validation.  
  - Spec: `RMC-SPEC-GUEST-001.3`, `RMC-SPEC-UX-001.6`, `RMC-SPEC-NN-001.6`

- **AC-GUEST-004** — *Given* a created booking, *when* the guest looks up by booking reference + email, *then* the booking detail is returned without requiring a guest account.  
  - Spec: `RMC-SPEC-GUEST-001.5`, `RMC-SPEC-IAM-001`, `RMC-SPEC-NN-001.10`

- **AC-GUEST-005** — *Given* the online-payment return page, *when* Maya redirects the browser but payment is not confirmed by webhook/poll, *then* the UI must not claim the booking is paid solely from redirect state.  
  - Spec: `RMC-SPEC-GUEST-001.6`, `RMC-SPEC-UX-001.3`, `RMC-SPEC-ARCH-001`, `RMC-SPEC-ARCH-001.1`, `RMC-SPEC-PAY-002.1`

- **AC-GUEST-006** — *Given* email is enabled, *when* a guest booking is created (online or pay-at-hotel), *then* a `BOOKING_RECEIVED` outbox item is queued whose body includes the **booking reference** and stay summary and is eventually sent; *and* when staff later confirms, a separate `CONFIRMATION` outbox item is queued; *and* when mail is disabled, no outbox rows are created.
  - Spec: `RMC-SPEC-GUEST-001.7`, Appendix L

- **AC-GUEST-007** — *Given* checkout payment step, *when* the guest chooses a method, *then* only `ONLINE_MAYA` or `PAY_AT_HOTEL` are offered and the created booking uses the chosen method.  
  - Spec: `RMC-SPEC-GUEST-001.4`, `RMC-SPEC-PAY-001`

- **AC-GUEST-008** — *Given* staff branding config (logo/colors/font/footer), *when* the guest UI loads, *then* public branding is applied (CSS variables / chrome) without requiring a redeploy.  
  - Spec: `RMC-SPEC-GUEST-001.8`, `RMC-SPEC-UX-001.7`, `RMC-SPEC-STAFF-001.6`

### 3.2 Inventory & holds

- **AC-INV-001** — *Given* an active hold on inventory, *when* another guest attempts to book the same room-night combination, *then* the system denies or excludes that inventory from the new availability result.  
  - Spec: `RMC-SPEC-INV-001.1`

- **AC-INV-002** — *Given* a hold whose TTL has expired, *when* the scheduler runs, *then* the hold is released and the inventory becomes sellable again.  
  - Spec: `RMC-SPEC-INV-001.2`

- **AC-INV-003** — *Given* room availability settings such as overbooking buffer or advance-booking windows, *when* search runs, *then* those settings are reflected in the result.  
  - Spec: `RMC-SPEC-INV-001.3`

- **AC-INV-004** — *Given* concurrent checkout attempts against scarce inventory, *when* they race, *then* the system never double-sells the same inventory.  
  - Spec: `RMC-SPEC-NN-001.3`, `RMC-SPEC-INV-001.4`, `RMC-SPEC-LIVE-001.3`

### 3.3 Payments & booking status

- **AC-PAY-001 (Maya checkout create)** — *Given* a valid online booking checkout, *when* the server creates a Maya session, *then* the request is based on server-derived booking totals and returns the hosted-checkout URL/reference needed for the flow.  
  - Spec: `RMC-SPEC-PAY-001.1`

- **AC-PAY-002 (webhook truth)** — *Given* a spoofed or premature browser success flow without a verified Maya webhook (and without successful confirm-poll), *then* the booking must not transition to paid/confirmed solely because the browser returned.  
  - Spec: `RMC-SPEC-NN-001.1`, `RMC-SPEC-ARCH-001`, `RMC-SPEC-PAY-002.1`

- **AC-PAY-003 (webhook signature / secret / HMAC)** — *Given* a webhook with invalid verification data, *when* it reaches the API, *then* it is rejected and no booking/ledger state changes occur.  
  - Spec: `RMC-SPEC-PAY-001.2`

- **AC-PAY-004 (idempotency)** — *Given* the same webhook or equivalent payment callback arrives more than once, *when* it is processed, *then* booking/payment effects are applied exactly once.  
  - Spec: `RMC-SPEC-PAY-001.3`, `RMC-SPEC-ARCH-001.2`

- **AC-PAY-005 (legal transitions)** — *Given* the booking status machine, *when* a transition is attempted, *then* illegal transitions are rejected and legal transitions are service-controlled.  
  - Spec: `RMC-SPEC-PAY-002`

- **AC-PAY-006 (pay-at-hotel / approval path)** — *Given* a booking created under pay-later rules, *then* the booking lands in the correct pending/confirmed-pay-later path and can be completed by staff according to role permissions.  
  - Spec: `RMC-SPEC-PAY-001.4`, `RMC-SPEC-PAY-001.5`

- **AC-PAY-007 (ledger)** — *Given* a money-affecting event (charge, refund, additional charge payment), *then* a ledger record exists reflecting that event.  
  - Spec: `RMC-SPEC-NN-001.4`, `RMC-SPEC-DATA-001.1`

- **AC-PAY-008 (additional charges)** — *Given* a booking additional charge, *when* staff approves/rejects/pays it, *then* booking state, charge state, and audit/ledger side effects are consistent.  
  - Spec: `RMC-SPEC-PAY-001.6`

- **AC-PAY-009 (sandbox/prod separation)** — *Given* a production deploy, *then* live and sandbox Maya secrets/URLs are not mixed and production verification is enabled.  
  - Spec: `RMC-SPEC-NN-001.8`, `RMC-SPEC-PAY-001.7`, `RMC-SPEC-CICD-001.3`

- **AC-PAY-010 (amount integrity)** — *Given* a Maya success signal whose amount does not match the booking `quotedTotal`, *when* payment application runs, *then* the booking is not confirmed as paid and the mismatch is logged/errored.  
  - Spec: `RMC-SPEC-PAY-002.2`, Appendix F.9

- **AC-PAY-011 (payment evidence)** — *Given* a Maya webhook or confirm-poll payload, *when* it is processed, *then* a `maya_payment_event` row is stored with redacted JSON, SHA-256, booking reference, checkout id, source, and correlation id, sufficient to debug and replay the parsed status; *and* a failure to store evidence does not prevent payment application.  
  - Spec: `RMC-SPEC-ARCH-001.4`

### 3.4 Cancellation & refunds

- **AC-CXL-001** — *Given* a booking subject to refund policy, *when* cancellation/refund is evaluated, *then* the snapshot policy attached to the booking is used rather than mutable current config alone.  
  - Spec: `RMC-SPEC-CXL-001.2`, `RMC-SPEC-DATA-001.2`

- **AC-CXL-002** — *Given* a FULL / PARTIAL / NONE refund rule, *when* a qualifying refund is processed, *then* the amount/status outcome matches the policy.  
  - Spec: `RMC-SPEC-CXL-001.1`

- **AC-CXL-003** — *Given* a staff Maya or (when enabled) manual refund action, *when* it is executed, *then* the action is audited and reflected in refund/booking/ledger state; manual methods include GCash, bank transfer, cash, or other when manual refunds are enabled. *And* when same-day full reverse prefers Maya void but Maya rejects void as unavailable (e.g. `PY0045`), *then* the server does not call same-day refund (Maya `PY0047`); it surfaces a retry-after-midnight Asia/Manila message and keeps staff able to complete later; after that cutoff, void-unavailable falls back to Maya refund.
  - Spec: `RMC-SPEC-CXL-001.3`, `RMC-SPEC-CXL-001.4`

- **AC-CXL-004 (auto-refund on guest cancel)** — *Given* a paid `ONLINE_MAYA` booking with refund-eligible amount > 0 under the snapshot policy, *when* the guest successfully cancels, *then* Maya refund is initiated automatically (no staff approval), ledger/refund status update, and audit exist.  
  - Spec: `RMC-SPEC-CXL-001.5`, `RMC-SPEC-CXL-001.6`

- **AC-CXL-005 (auto-refund failure)** — *Given* auto-refund is attempted after guest cancel, *when* Maya refund fails, *then* the booking remains cancelled, refund is marked failed/retryable or staff-completable, and the failure is audited without silently dropping the entitlement.  
  - Spec: `RMC-SPEC-CXL-001.7`

- **AC-CXL-006 (guest refund messaging)** — *Given* a cancelled booking whose refund is pending, failed, or not yet complete, *when* the guest views confirmation/detail UI, *then* the UI must not claim the refund is completed.  
  - Spec: `RMC-SPEC-CXL-001.9`, `RMC-SPEC-UX-001.4`

- **AC-CXL-007 (no auto Maya refund for pay-at-hotel)** — *Given* a pay-at-hotel / non-Maya paid balance cancel, *when* cancel succeeds, *then* the system does not initiate an automatic Maya refund; staff/manual paths apply.  
  - Spec: `RMC-SPEC-CXL-001.8`

### 3.5 Staff & RBAC

- **AC-STAFF-001** — *Given* a FRONT_DESK user, *when* they attempt an ADMIN-only action (or a refund without MANAGER/ADMIN), *then* the request is denied.  
  - Spec: `RMC-SPEC-IAM-001.1`, `RMC-SPEC-IAM-001.2`, `RMC-SPEC-IAM-001.3`, `RMC-SPEC-NN-001.5`

- **AC-STAFF-002** — *Given* allowed room/booking operational actions, *when* staff performs them, *then* the resulting booking/room state is consistent and auditable.  
  - Spec: `RMC-SPEC-STAFF-001.2`, `RMC-SPEC-STAFF-001.4`

- **AC-STAFF-011** — *Given* a staff booking detail view, *when* it loads, *then* it includes `createdAt`; *and* after approve/reject, it surfaces the acting staff (name + email) and time from audit for both pay-at-hotel and Maya paths; *and* it includes guest service/item add-ons and any `customExtrasRequest` note.  
  - Spec: `RMC-SPEC-STAFF-001.2`

- **AC-STAFF-012** — *Given* a rate plan, *when* staff set a primary nightly rate, *then* that amount is stored on the plan and applied to upcoming non-override nights; *when* staff set a date-range override, *then* those nights use the override amount and remain overrides when the primary rate is later changed.  
  - Spec: `RMC-SPEC-STAFF-001.4d`

- **AC-STAFF-003** — *Given* module-based staff navigation, *when* a role lacks access, *then* the UI and API both prevent use of that module/action. *And* parent nav groups are collapsible (expand/collapse), auto-open when a child route is active, matching `RMC-SPEC-UX-001.5a`.  
  - Spec: `RMC-SPEC-UX-001.5`, `RMC-SPEC-UX-001.5a`, `RMC-SPEC-IAM-001.2`, `RMC-SPEC-CFG-001.2`

- **AC-STAFF-004** — *Given* user/profile/notification/audit administrative surfaces, *then* privileged operations are restricted and auditable.  
  - Spec: `RMC-SPEC-STAFF-001.7`, `RMC-SPEC-STAFF-001.8`

- **AC-STAFF-005** — *Given* a room type with no bookings or inventory holds, *when* staff deletes it, *then* it is hard-deleted (owned rate plans/daily rates removed; units unlinked). *Given* a room type still referenced by bookings or inventory holds, *when* staff deletes it, *then* it is deactivated (`active=false`) instead and the response indicates deactivation.  
  - Spec: `RMC-SPEC-STAFF-001.4a`

- **AC-STAFF-006** — *Given* a room unit with past and/or upcoming assigned bookings, *when* staff opens “View bookings” on room operations, *then* a paged list of all bookings assigned to that unit is returned and staff can open booking detail from a row.  
  - Spec: `RMC-SPEC-STAFF-001.4b`

- **AC-STAFF-007** — *Given* a room unit assigned to an active booking (`checkedOutAt` null), *when* staff deletes the room number, *then* the delete is rejected. *Given* a room unit with only historical booking references, *when* staff deletes the room number, *then* those bookings are unlinked (`room_unit_id` null), parent `totalCapacity` is decremented when applicable, and the unit is hard-deleted.  
  - Spec: `RMC-SPEC-STAFF-001.4c`

- **AC-STAFF-008** — *Given* a room type, *when* staff creates a rate plan with a `refundPolicyId` and base nightly rate, *then* the plan links that named policy, seeds daily rates, and uses the parent type’s units for availability; the plan does not own guest product fields.
  - Spec: `RMC-SPEC-STAFF-001.4d`, `RMC-SPEC-STAFF-001.4`, `RMC-SPEC-STAFF-001.4f`
- **AC-STAFF-009** — *Given* staff open Create room → Rate plans tab, *when* they create a rate plan via the short wizard, *then* they must select a room type and a refund policy and set pricing, and the new plan is listed with its own sample nightly rate.
  - Spec: `RMC-SPEC-STAFF-001.4e`, `RMC-SPEC-STAFF-001.4d`
- **AC-STAFF-010** — *Given* staff create or edit a room type, *when* the full catalog wizard runs, *then* guest-facing product fields (class, details, media) plus room-number assignment are configured on the room type.
  - Spec: `RMC-SPEC-STAFF-001.4f`, `RMC-SPEC-STAFF-001.4`

- **AC-STAFF-013** — *Given* Room configuration amenities, *when* staff add/rename/remove amenity options, *then* those values appear in the room-type amenities dropdown; *when* staff select an amenity on create/edit, *then* it is added to the room type and can be removed before save; *when* an amenity label is still used by a room type, *then* config delete is rejected.
  - Spec: `RMC-SPEC-STAFF-001.4g`, `RMC-SPEC-STAFF-001.4`

### 3.6 Security

- **AC-SEC-001 (JWT auth)** — *Given* a protected staff endpoint, *when* no valid access token is supplied, *then* access is denied.  
  - Spec: `RMC-SPEC-SEC-001.1`, `RMC-SPEC-IAM-001`

- **AC-SEC-002 (rate limit)** — *Given* repeated guest booking attempts beyond the configured limit, *when* the threshold is crossed, *then* the request is rejected with rate-limit behaviour.  
  - Spec: `RMC-SPEC-SEC-001`

- **AC-SEC-003 (secret hygiene)** — *Then* no real secrets are committed to the repo; production secrets come only from env/secret store.  
  - Spec: `RMC-SPEC-META-001.4`, `RMC-SPEC-CICD-001`

- **AC-SEC-004 (PII-safe logs)** — *Given* booking/payment/staff operations, *then* logs should not expose secrets or unnecessary sensitive guest/payment data.  
  - Spec: `RMC-SPEC-SEC-001.2`

- **AC-SEC-005 (webhook production hardening)** — *Given* production mode, *then* webhook verification controls required by policy are enabled, not left in a relaxed local posture.  
  - Spec: `RMC-SPEC-PAY-001.2`, `RMC-SPEC-CICD-001.3`

- **AC-SEC-006 (edge security posture)** — *Given* staging/prod, *then* TLS, security headers, and edge protections are configured for the public app/API.  
  - Spec: `RMC-SPEC-SEC-001.3`

- **AC-SEC-007 (privileged MFA)** — *Given* production go-live posture, *when* an ADMIN (and ideally MANAGER) authenticates, *then* MFA is required per policy; self-enroll remains available to staff.  
  - Spec: `RMC-SPEC-IAM-001.4`, `RMC-SPEC-LIVE-001`

- **AC-SEC-008 (hosted checkout only)** — *Given* online card/e-wallet payment, *when* checkout runs, *then* no raw card PAN or CVV is collected or stored on the RMC origin; payment uses Maya hosted checkout.  
  - Spec: `RMC-SPEC-NN-001.7`

### 3.7 Observability

- **AC-OBS-001** — *Given* booking/payment flows, *then* logs/metrics should let operators identify failures in checkout, webhook processing, refunds, and hold release.  
  - Spec: `RMC-SPEC-OBS-001`, `RMC-SPEC-STACK-001.3`

- **AC-OBS-002** — *Given* an online-payment incident, *then* operators should be able to trace the booking from checkout attempt through webhook application and resulting state.  
  - Spec: `RMC-SPEC-OBS-001`, `RMC-SPEC-ARCH-001.3`

- **AC-OBS-003** — *Given* repeated webhook or hold failures, *then* alertable signals should exist.  
  - Spec: `RMC-SPEC-OBS-001.2`

### 3.8 Config integrity

- **AC-CFG-001** — *Given* an existing booking with a refund-policy snapshot, *when* a live refund policy or rate-plan policy link is changed, *then* the historical snapshot on that booking is not rewritten.  
  - Spec: `RMC-SPEC-CFG-001.1`, `RMC-SPEC-CXL-001.2`

- **AC-CFG-002** — *Given* named refund policies referenced by active rate plans, *when* staff tries to deactivate a still-referenced policy, *then* the request is rejected until plans are reassigned.  
  - Spec: `RMC-SPEC-CFG-001.1a`

- **AC-CFG-003** — *Given* Settings → Refund policy, *when* staff creates multiple named policies including nights-deduction fields, *then* each policy is independently editable and selectable on rate plans.
  - Spec: `RMC-SPEC-CFG-001`, `RMC-SPEC-STAFF-001.5`

- **AC-CFG-004** — *Given* Settings → Refund policy create or update, *when* the guest-facing description is blank, *then* the save is rejected; a non-blank description is persisted and copied onto linked rate plans for guest display.
  - Spec: `RMC-SPEC-CFG-001.1b`

- **AC-CXL-008** — *Given* a booking under a rate plan linked to a specific refund policy, *when* the refund snapshot is attached, *then* snapshot fields (incl. nights deduction) match that linked policy.  
  - Spec: `RMC-SPEC-CXL-001.2a`

- **AC-CXL-009** — *Given* a PARTIAL cancel with nights deduction N and/or partial %, *when* evaluated, *then* nights fee uses `min(N, stayNights)` then percent applies to remaining; FULL window ignores nights/partial.
  - Spec: `RMC-SPEC-CXL-001.1`, `RMC-SPEC-CXL-001.1b`

### 3.8a Promo codes (access rates)

- **AC-PROMO-001** — *Given* staff open Settings → Promo codes, *when* they create a `SPECIAL_RATE` with offer code, rate plans, discount, and max uses, *then* the promo code is listed and editable; corporate/agency require both organization and offer codes.
  - Spec: `RMC-SPEC-PROMO-001.1`, `.2`, `RMC-SPEC-STAFF-001.5`

- **AC-PROMO-002** — *Given* an active promo code scoped to specific rate plans, *when* availability/quote runs with matching codes, *then* only those plans are discounted; out-of-scope plans stay at rack; automatic public promo does not stack on the same booking. *Given* a previously applied code that is later inactive/expired/exhausted, *when* the guest books, *then* create succeeds at rack (or automatic public promo) instead of failing.
  - Spec: `RMC-SPEC-PROMO-001.3`, `.4`, `.5`, Appendix G

- **AC-PROMO-003** — *Given* a promo code with `max_uses` reached, *when* a guest applies it or creates a booking with it, *then* the code is rejected.
  - Spec: `RMC-SPEC-PROMO-001.3`, `.6`

- **AC-PROMO-004** — *Given* a booking created with a promo code, *when* create succeeds, *then* `used_count` increments; *when* that booking fails or is cancelled while still `PENDING_PAYMENT`/`PENDING_APPROVAL`, *then* usage is released.
  - Spec: `RMC-SPEC-PROMO-001.6`

- **AC-PROMO-005** — *Given* corporate/agency type, *when* guest submits with only one of the two codes, *then* apply is rejected client- and server-side.
  - Spec: `RMC-SPEC-PROMO-001.1`, `RMC-SPEC-GUEST-001.2b`

### 3.9 Ops / delivery

- **AC-OPS-001** — *Given* a fresh environment, *when* Flyway runs, *then* the schema is created from migration history without manual DB edits.  
  - Spec: `RMC-SPEC-CICD-001.1`, `RMC-SPEC-DATA-001`

- **AC-OPS-002** — *Given* a staging deployment, *then* the system can run against real env vars without depending on local-only defaults.  
  - Spec: `RMC-SPEC-CICD-001.2`

- **AC-OPS-003** — *Given* launch preparation, *then* backup/restore and secret-storage posture are defined.  
  - Spec: `RMC-SPEC-LIVE-001`

- **AC-OPS-004** — *Given* go-live readiness, *when* staging UAT for Maya webhook and auto-refund paths is reviewed, *then* evidence is recorded in `docs/VALIDATION_RUN.md`.  
  - Spec: `RMC-SPEC-LIVE-001.1`, `RMC-SPEC-LIVE-001.2`

### 3.10 Documentation completeness (builder gates)

- **AC-DOC-001 (Maya contract)** — *Given* Appendix F, *then* a builder can implement create-checkout, redirects, webhook path, status mapping, `requestReferenceNumber` rules, and poll vs webhook without inventing Maya URLs or auth.  
  - Spec: `RMC-SPEC-PAY-001`, Appendix F

- **AC-DOC-002 (status + pricing + holds)** — *Given* Appendices E, G, H, *then* booking transitions, quote formula, and hold TTL/release rules are specified without guessing.  
  - Spec: Appendices E, G, H

- **AC-DOC-003 (API + RBAC + routes)** — *Given* Appendices D, I, J and §4, *then* HTTP paths/auth, roles, and guest/staff IA are specified.  
  - Spec: `RMC-SPEC-API-001`, Appendices D, I, J

- **AC-DOC-004 (seed branding + demo profile)** — *Given* Appendices A and M, *then* a fresh environment can be seeded with branding defaults and minimum demo catalog/staff/nav data without inventing product look.  
  - Spec: `RMC-SPEC-UX-001.8`, Appendix A, Appendix M

### 3.11 Visual fidelity

- **AC-UI-001** — *Given* `docs/prototype/rmc-booking.html` as visual SoT, *when* guest and staff **chrome + critical guest flows** are reviewed for fidelity, *then* they match the prototype at desktop and ~390px within the SoT scope in `RMC-SPEC-UX-001.10` (staff module bodies may be representative stubs).  
  - Spec: `RMC-SPEC-UX-001.10`

- **AC-UI-002 (staff theme)** — *Given* an authenticated staff user, *when* they toggle light/dark from the **sidebar account dropdown** or profile, *then* the UI switches between `LIGHT` and `DARK`, brand primary remains usable, and `themePreference` persists. Guest UI does not require dark mode.  
  - Spec: `RMC-SPEC-UX-001.11`, `RMC-SPEC-STAFF-001.9`

- **AC-UI-003 (motion / hover / reduced-motion)** — *Given* guest and staff UIs, *when* interacting with CTAs, nav, tables, and the room search carousel, *then* short transitions and defined hovers are present — including carousel sibling darken + logo overlay + hover scale when a logo exists — and `prefers-reduced-motion: reduce` disables/minimizes non-essential motion.  
  - Spec: `RMC-SPEC-UX-001.9`, `RMC-SPEC-UX-001.12`

- **AC-UI-004 (chrome, brand mark, seed catalog, interaction fidelity)** — *Given* the live product / SoT, *then*: brand mark rules (`RMC-SPEC-UX-001.13`); guest chrome + media (`RMC-SPEC-UX-001.14`); staff chrome (`RMC-SPEC-UX-001.15`); seed catalog (`RMC-SPEC-UX-001.16`); guest home + checkout fidelity — filter overlay, Explore ⅓ carousel + dots-only, Lucide icons, thin scrollbars, guests-stepper / gallery / one-step checkout + Next vs Maya/pay-at-hotel CTAs / page motion (`RMC-SPEC-UX-001.17`); staff scroll-hide top bar, enter transition, dashboard charts, filter bars, catalog wizard, booking ops strip, refund-policy fields, General settings cards (`RMC-SPEC-UX-001.18`); shared StaffModal create/edit pattern (`RMC-SPEC-UX-001.19`); MFA step when required (`RMC-SPEC-STAFF-001.10`).  
  - Spec: `RMC-SPEC-UX-001.13` … `.19`, `RMC-SPEC-STAFF-001.8`, `RMC-SPEC-STAFF-001.10`

- **AC-UI-005 (confirmation + loading feedback)** — *Given* a destructive or irreversible action (guest cancel, checkout submit, staff delete/approve/reject), *when* the guest or staff triggers it, *then* an in-app confirmation dialog is shown (not `window.confirm`); *and* while the network request runs, a loading overlay or busy control is visible; *and* success/failure is shown via toast or inline alert.
  - Spec: `RMC-SPEC-UX-001.20`

---

## Part 4 — Coverage map (which test type owns which ACs)

| Test type | Owns | Typical location |
|---|---|---|
| Backend unit/service tests | `AC-PAY-001`, `AC-PAY-003`, `AC-PAY-010`, `AC-PAY-011`, `AC-CXL-*`, `AC-CFG-001`, some `AC-SEC-*` | `rmc_backend/src/test/java/...` |
| Backend integration tests | `AC-PAY-002`, `AC-PAY-004`, `AC-PAY-005`, `AC-INV-*`, `AC-STAFF-*`, auto-refund | `rmc_backend/src/test/java/...` |
| Frontend/browser e2e | `AC-GUEST-*`, `AC-CXL-006`, `AC-UI-002`, `AC-UI-003`, role/path smoke | `rmc_frontend/tests/e2e/` (as added) |
| CI / static scan | `AC-SEC-003`, `AC-SEC-008` (architecture review), `AC-OPS-001` | CI workflows + scripts |
| Manual staging verification | `AC-PAY-009`, `AC-SEC-006`, `AC-SEC-007`, `AC-OPS-002`, `AC-OPS-003`, `AC-OPS-004`, `AC-UI-001` … `AC-UI-005` | `docs/VALIDATION_RUN.md` |
| Spec/doc review | `AC-DOC-001` … `AC-DOC-004` | RESEARCH + SPEC + prototype review |

**Coverage registry:**
- Human map: `docs/AC_COVERAGE.md`
- Machine registry: `rmc_backend/src/test/resources/ac-coverage.tsv`
- Guard test: `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/AcCoverageMatrixTest.java`

---

## Summary

- **Traceability:** RESEARCH v1.0 → SPEC v1.0; VALIDATION v1.1 ACs are the pass/fail gate.  
- **Buildability:** High-risk payment/inventory/cancel rules are pinned; residual inputs are brand assets / host vendor / UI screenshot threshold.  
- **Testability:** Critical SPEC rows (including auto-refund UX, amount match, MFA, branding, CFG snapshot, staff theme, motion/hover) now have Given/When/Then ACs.  
- **Product completeness:** Auto-refund is normative (`RMC-SPEC-CXL-001.5`–`.9`), proven by `AC-CXL-004` … `AC-CXL-007`.

**Recommended proof order:** Maya webhook UAT → amount-match + auto-refund tests → hold concurrency → guest/staff e2e (incl. refund messaging) → MFA/staging go-live evidence in `VALIDATION_RUN.md`.
