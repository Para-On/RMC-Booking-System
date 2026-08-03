# RMC Booking System — Product Research

**Version:** 1.0.5  
**Status:** Baseline for the production build specification (`docs/SPEC.md`)  
**Audience:** Product, engineering, and stakeholders agreeing scope before SPEC freeze  
**Date:** 2026-07-30  

---

## 1. Purpose and method

### 1.1 Purpose

This document is the **agreed product research baseline** for the RMC Booking System. It answers:

- What we are building  
- Why those choices were made  
- What is in scope vs out of scope for v1  
- Which rules are fixed product behaviour vs hotel-configurable settings  
- What default visual direction the system should ship with  

It is **not** a changelog of an existing codebase, and it is **not** the build specification. The SPEC will turn this research into normative requirements with stable IDs (`RMC-SPEC-<AREA>-<NNN>` and sub-IDs `RMC-SPEC-<AREA>-<NNN>.<n>`).

### 1.2 Method

| Input | Use |
|---|---|
| Product goals for a single-property hotel booking + staff operations product | Scope and journeys |
| Capability inventory of guest flows, staff modules, and booking engine domains | Feature spine |
| Stakeholder decision recommendations (this document) | Locked baseline unless explicitly revised |
| Hospitality UX and accessibility considerations | Design direction defaults |

### 1.3 Handoff to SPEC

| Research section | Becomes in SPEC |
|---|---|
| Product definition, scope, journeys | Non-negotiables, feature specs, build phases |
| Feature inventory | Guest / staff / payments / inventory / cancel requirements with SPEC IDs |
| Decisions log | Normative product rules (not “open” unless revised here first) |
| Config vs fixed | Settings model vs hard behavioural rules |
| Glossary | Shared vocabulary in SPEC and acceptance criteria |
| Design direction | UX defaults + branding model; visual SoT = `docs/prototype/rmc-booking.html` |
| IA snapshot | Route / information architecture section |

**Rule:** If product intent changes, update this research first (or in the same change set), then update the SPEC.

---

## 2. Product definition

### 2.1 One-liner

RMC is a **single-property hotel booking system**: guests search availability, receive a server-priced quote, book a stay, and pay either online via Maya hosted checkout or at the hotel; staff operate a property-management-style console for approvals, arrivals, rooms, folio charges, refunds, promos, branding, users, and audit.

### 2.2 Users

| Actor | Access model | Primary jobs |
|---|---|---|
| **Guest** | No account; booking reference + email for lookup | Search, book, pay, view booking, cancel, pay additional charges |
| **Front desk** | Staff JWT; module-gated nav | Arrivals, bookings, check-in/out, day-to-day stay ops |
| **Manager** | Staff JWT; broader modules | Ops above + refunds and selected admin actions per policy |
| **Admin** | Staff JWT; full modules | Users, modules, branding, rates, refund policy, promos, audit, system config |

### 2.3 Success outcomes

1. A guest can complete a booking without trusting client-supplied prices.  
2. Online payment state reflects **verified payment provider truth**, not a browser redirect alone.  
3. Inventory holds prevent double-selling the same room-night under concurrency.  
4. Staff can run the stay lifecycle (approve, assign room, check-in/out, charges, refunds).  
5. Cancellation and refund outcomes follow a policy snapshot attached to the booking.  
6. Sensitive staff actions are authorized and auditable.

---

## 3. In scope and out of scope (v1)

### 3.1 In scope

**Guest**

- Availability search by dates and occupancy  
- Room type detail and checkout  
- Extras (service/item add-ons), additional guests, special requests  
- DPA / consent capture at checkout  
- Payment methods: `ONLINE_MAYA`, `PAY_AT_HOTEL`  
- Confirmation / success UI driven by server booking/payment status  
- Lookup by booking reference + email  
- Guest cancel with policy evaluation  
- Pay additional charges (Maya or pay-at-hotel)  
- Guest-visible branding (logo, colors, footer/contact)  
- Transactional email via outbox for key lifecycle events  

**Staff**

- Login, refresh tokens, optional MFA enroll; production MFA policy per decisions log  
- Dashboard metrics and booking overview  
- Arrivals, bookings list/detail, guest directory/profile  
- Room config options, catalog/units, extras catalog, daily room operations / calendar  
- System settings (taxes/fees keys, rates), refund policy, promos, audit logs  
- Branding administration  
- Staff user administration and configurable sidebar modules  
- Profile (including light/dark preference)  
- Global search and in-app notifications  

**Engine**

- Server-side pricing in **PHP only**  
- Inventory holds with TTL, expiry/release, and concurrency safety  
- Booking status machine and ledger for money events  
- Maya hosted checkout create, webhook (preferred authoritative push), confirm-payment poll (secondary)  
- Staff Maya refund / void and optional manual refund paths  
- **Auto Maya refund on guest cancel** when policy entitles a refundable amount for paid online bookings (target product behaviour)  
- Schedulers: expire unpaid pending payments, overdue pay-later handling, no-shows, email outbox drain  
- RBAC via roles + nav-module allowlists  

### 3.2 Out of scope (v1)

- Multi-property / multi-hotel inventory and true property switching  
- Guest user accounts, loyalty, or authenticated guest portal beyond reference lookup  
- Channel manager / OTA connectivity  
- Embedded card fields or raw PAN/CVV on the RMC origin  
- Non-PHP currencies  
- Inventing a second information architecture unrelated to the guest book + staff PMS model below  

Items in 3.2 require a new research revision and new SPEC IDs before implementation.

---

## 4. Feature inventory

### 4.1 Guest features

| Feature | Description |
|---|---|
| Home / search | Date and occupancy search; available room types from the server |
| Room detail | Gallery, amenities, capacity, promo/tax presentation, proceed to checkout |
| Checkout | Multi-step: room summary → extras → guest details/consent → payment → confirm |
| Online pay | Server creates Maya hosted checkout; browser redirects to Maya |
| Pay at hotel | Booking enters pending-approval path for staff |
| Confirmation | Messaging for confirmed / pending approval / payment pending — based on server state |
| Maya return | Success page polls/confirm-payment; must not claim paid from redirect alone |
| Lookup | Reference + email → booking detail |
| Manage booking | View status and policy text; cancel; pay additional charges |
| Branding chrome | Header/footer/logo/colors from branding config |

### 4.2 Staff features

| Module | Description |
|---|---|
| Auth | Email/password login; MFA verify when enabled; refresh/logout; lockout behaviour |
| Dashboard | Occupancy, movements, revenue signals, booking list |
| Arrivals | Date-filtered expected arrivals |
| Bookings | Search/filter list; detail ops (approve/reject, check-in/out, transfer, override, charges, refunds) |
| Guests | Directory and profile with stay history |
| Rooms — config | Category, view, bed, status option lists |
| Rooms — catalog | Create room page tabs: room numbers; full room catalog (product + units); **rate plans** tab (catalog + refund policy + pricing) |
| Rooms — extras | Service and item add-ons for guest checkout |
| Rooms — operations | Daily status grid and unit calendar; staff can list all bookings ever assigned to each room unit |
| Settings — general | Service charge / VAT / municipal tax and system keys; rate-plan hold TTL / daily-rate batch / units as supported |
| Settings — refund policy | **Named multi-policy CRUD** (Add policy): cutoffs, partial %, optional nights deduction, check-in time, timezone, description, refundable; rate plans **select** a policy; edits must not rewrite booking snapshots |
| Settings — promos | **Automatic public promos:** percent or fixed PHP; windows; room-type scope (no guest code) |
| Settings — promo codes | **Access / negotiated rates:** type (special / corporate / agency), offer code (+ org code when required), rate-plan scope, % or fixed, max uses |
| Settings — audit | Login and activity audit |
| Branding | Logo, primary/secondary colors, font allowlist, footer/contact |
| Users | Create/activate/deactivate staff; roles; password reset |
| Modules | Sidebar tree: labels, paths, role allowlists, order, enable/disable |
| Profile | Name, photo, password, theme preference |
| Chrome | Global search; notification bell |

### 4.3 Engine / domain features

| Domain | Description |
|---|---|
| Pricing | Rates, taxes/fees, automatic promos, **promo codes**, extras — computed server-side |
| Holds | Per-night holds; ACTIVE excludes inventory; TTL; scheduler release |
| Availability | Capacity, buffers/windows, unit status, active holds |
| Payments | Maya create/get/refund/void; webhook application; ledger credits/debits |
| Status | Controlled transitions (see glossary) |
| Cancel / refund | Policy snapshot; FULL / PARTIAL / NONE; staff refunds; auto-refund for eligible online cancels |
| Additional charges | Staff-created; guest pay Maya or at hotel; approve/reject/record as designed |
| Email | Outbox + scheduler; confirmation, rejection, refund-processed kinds |
| Audit | Booking transitions; staff login/activity; configuration changes |
| Uploads | Room/extras/branding/avatar media served from configured upload root |

---

## 5. Primary journeys

### 5.1 Guest — book and pay online

1. Guest searches dates and occupancy; optionally applies a **promo code** beside the home filters (offer only, or org + offer for corporate/agency). Results show **one card per room type** with room-type media/meta and **From ₱X** (minimum tax-inclusive among active rate plans; discounted when a valid code applies to that plan).  
2. Guest opens room detail (room-type gallery/copy), sees **rate plans** as price/policy options (lowest pre-highlighted; code discounts reflected), **selects a plan**, then enters checkout.  
3. Server re-quotes for the selected plan (re-validates promo code if present); guest selects extras and enters identity/contact/consent.  
4. Guest chooses `ONLINE_MAYA`.  
5. Server creates booking in `PENDING_PAYMENT`, places inventory holds, creates Maya checkout with **server** amount.  
6. Guest pays on Maya hosted page.  
7. Maya webhook (preferred) and/or confirm-payment poll (secondary) advances payment/booking state.  
8. Staff may still approve per status rules before final confirmed stay state.  
9. Guest sees confirmation/success UI that reflects **server** status only.

### 5.2 Guest — pay at hotel

1. Same search → checkout through consent.  
2. Guest chooses `PAY_AT_HOTEL`.  
3. Booking enters `PENDING_APPROVAL` (or equivalent pending staff path).  
4. Staff approve → `CONFIRMED_PAY_LATER` (or reject/cancel per rules).  
5. Payment collection continues via folio / staff recording as designed.

### 5.3 Guest — lookup, cancel, extra charges

1. Guest looks up by reference + email.  
2. Guest views status, refund policy presentation, and outstanding charges.  
3. Guest may cancel; system evaluates the **snapshot** policy.  
4. If cancel entitles a Maya refund on a paid online booking, system **initiates auto-refund** (see decisions).  
5. If auto-refund fails, booking remains cancelled; refund is failed/retryable; staff can complete.  
6. Guest may pay additional charges via Maya or pay-at-hotel paths.

### 5.4 Staff — stay operations

1. Staff authenticates (MFA when required).  
2. Reviews dashboard / arrivals / bookings.  
3. Approves or rejects pending bookings.  
4. Checks in (assigns unit), may transfer room, checks out.  
5. Adds/records additional charges and folio payments.  
6. Processes refunds (Maya and/or manual when enabled); all sensitive actions audited.

### 5.5 Payment truth (non-negotiable journey rule)

- Browser return URLs are **UI only**.  
- Preferred authoritative push: verified Maya webhook.  
- Allowed secondary path: server polls Maya checkout status via confirm-payment.  
- The system must **never** mark paid solely because the browser landed on a success page.

---

## 6. Decisions log (baseline)

These recommendations are the **research baseline**. SPEC should treat them as decided unless this log is revised.

| ID | Topic | Decision | Rationale |
|---|---|---|---|
| D1 | Property model | **Single-property for v1** | Matches the product; avoids multi-tenant inventory complexity. Multi-property is a later epic. |
| D2 | Guest identity | **No guest accounts in v1**; lookup by **booking reference + email** | Sufficient for stay self-service; accounts add auth and PII scope without clear v1 need. |
| D3 | Online payment truth | **Webhook preferred**; confirm-payment poll secondary; **never redirect-alone paid** | Prevents false “paid” states and charge disputes. |
| D4 | Amounts | **PHP only**; **server-side** totals from rates/taxes/fees/promos/extras | Client totals are not trusted. |
| D5 | Card data | **Hosted checkout only**; no PAN/CVV on RMC origin | Keeps PCI scope at hosted-redirect posture. |
| D6 | Inventory | Holds must **block double-sell**; expire/release correctly | Core booking integrity. |
| D7 | Cancel refund — baseline ops | Staff can complete Maya and (when enabled) manual refunds; actions audited | Needed for pay-at-hotel and failure/override cases. |
| D8 | Cancel refund — target product | On guest cancel of paid `ONLINE_MAYA`, if snapshot policy eligible amount > 0 → **auto-initiate Maya refund** (no staff approval step). Zero-eligible → no Maya refund call. Pay-at-hotel / non-Maya balances stay staff/manual. On Maya failure → stay cancelled; refund failed/retryable; staff can finish; audit/log failure. Guest UI must not claim “refunded” until server status says so. | Correct guest expectation for online payments; staff remains safety net. |
| D9 | MFA | Self-enroll available to staff; **required for ADMIN** (and ideally **MANAGER**) before production go-live; FRONT_DESK optional until policy tightens | Privileged risk vs front-desk friction. |
| D10 | Branding runtime | Staff Branding settings are the **runtime** visual SoT for logo/colors/font/footer | Hotels rebrand without code deploys. |
| D11 | Branding defaults | Research §9 defines **intended hospitality seed** (Appendix A); live code `DEFAULT_BRANDING` is neutral black/Geist until seed aligns — visual SoT mirrors live | Builders see current UI; seed work converges to Appendix A |
| D12 | Visual fidelity | Visual SoT = `docs/prototype/rmc-booking.html`, kept aligned to current `rmc_frontend` IA + live tokens | Docs and UI share one picture of “what we have now” |
| D13 | Hosting | Exact cloud vendor left to ops; product assumes managed web app + API + MySQL | Product SPEC should not invent a fake topology. |
| D14 | Redis | **Optional** for single-instance; **mandatory** if multiple API instances need shared guest booking rate limits | Matches scalability reality. |
| D15 | Observability minimum | Structured logs, correlation across checkout→webhook→booking, error tracking; richer metrics/alerts in hardening | Operators must debug payment incidents. |
| D16 | Roles | `FRONT_DESK`, `MANAGER`, `ADMIN` with **module-based** nav ACL | Flexible IA without hard-coding every route to a single role. |
| D17 | Catalog vs rate plans | **Room type** owns guest-facing product (description, class/view/bed, capacity, amenities, images) plus assigned room numbers. **Rate plans** under a type own daily rates / base nightly and a named refund policy (plus hold TTL / active). Search: one card per room type (type media) with **From** = cheapest plan price. Detail shows type product and a plan picker (name/price/policy), lowest pre-highlighted. | One product description per type; multiple price/policy options share inventory. |
| D18 | Refund policy ownership | Cancel/refund rules live on **named refund policies** (multi-CRUD). Each rate plan **references** a policy (`refund_policy_id`). Booking snapshots copy fields from the linked policy at book time (incl. optional nights deduction). Live policy edits must not rewrite snapshots. PARTIAL cancel may stack nights fee then partial %. | Reusable policies across plans; clear hotel fee rules. |
| D19 | Promo codes vs automatic promos | **Automatic promos** (Settings → Promos) remain public, code-less, room-type scoped, best-savings auto-apply. **Promo codes** are a separate product: guest-entered access rates (special / corporate / agency), scoped to **rate plans**, % or fixed PHP, optional date window, **max uses**. Guest picks type in the home filter dropdown, then Applies codes (special = offer only; corporate/agency = organization + offer). Selected type must match the stored promo type. When a valid promo code is applied, it discounts eligible plans and **does not stack** with an automatic promo on that booking. Usage increments on booking create; exhausted codes cannot be used; usage releases if the booking fails or is cancelled while still unpaid/unconfirmed. One promo code per booking. Offer codes remain globally unique. | Negotiated/corporate rates without replacing public seasonal promos. |

---

## 7. Config vs fixed product rules

### 7.1 Fixed (product rules — SPEC-normative)

- Payment truth model (D3)  
- PHP + server-side pricing (D4)  
- Hosted checkout only (D5)  
- Hold / no double-sell (D6)  
- Status and payment-method meanings (glossary)  
- Auto-refund rules for eligible online guest cancel (D8)  
- Role set and requirement that protected mutations are authorized (D16)  
- Secrets never in git or client bundles  
- Sandbox vs production Maya credentials separated  

### 7.2 Configurable (hotel / admin settings)

- Service charge %, VAT %, municipal tax %, related system toggles  
- Named refund policies (cutoffs, partial %, nights deduction, etc.) selected by rate plans  
- Rate plans (per room type): daily rates, hold TTL, refund-policy link, active flag  
- Room types (product + units), extras catalog  
- Automatic promos (percent/fixed, windows, room-type scope)  
- Promo codes (type, codes, rate-plan scope, discount, max uses, windows)  
- Branding: logo, primary/secondary colors, font (allowlist), footer text/contact/copyright  
- Hold TTL / overbooking buffer / advance windows **as exposed by settings**  
- Mail enabled/disabled and SMTP settings  
- Guest booking rate-limit numeric threshold  
- Staff module tree labels/order/role allowlists (paths must remain valid product routes)  

**Research rule:** Do not freeze tax percentages or cutoff hours in the SPEC as immutable product law; freeze that they **exist as configuration** and that bookings **snapshot** refund policy at the appropriate lifecycle point.

---

## 8. Domain glossary

| Term | Meaning |
|---|---|
| `ONLINE_MAYA` | Guest pays via Maya hosted checkout |
| `PAY_AT_HOTEL` | Guest will pay at property; staff approval path |
| `PENDING_PAYMENT` | Online booking awaiting successful Maya payment |
| `PENDING_APPROVAL` | Awaiting staff approve/reject |
| `CONFIRMED` | Confirmed stay after online path / approval rules |
| `CONFIRMED_PAY_LATER` | Confirmed pay-at-hotel path |
| `CANCELLED` | Booking cancelled |
| `FAILED` | Payment/booking failed terminal state as designed |
| `NO_SHOW` | Marked no-show by lifecycle rules |
| Inventory hold | Reservation of room-night inventory for a booking; ACTIVE excludes availability |
| Ledger | Append-only money event records (credit/debit/refund/etc.) |
| Room catalog / room type | Guest product + inventory: name, description, class/view/bed, capacity, amenities, images, room numbers |
| Rate plan | Price/policy option under a room type: daily rates + refund-policy link + hold TTL; guest must select one before checkout |
| From price | Minimum tax-inclusive stay total among active rate plans for searched dates; home card media comes from the room type |
| Automatic promo | Public discount auto-applied to matching room types (no guest code); largest savings wins |
| Promo code | Guest-entered access rate (special / corporate / agency); offer code (+ org code when required); scoped to rate plans; max uses |
| Named refund policy | Reusable cancel/refund rules selected by rate plans; snapshotted onto bookings |
| Refund policy snapshot | Copy of the **booked rate plan’s** policy terms attached to the booking for cancel evaluation |
| `FULL` / `PARTIAL` / `NONE` | Cancellation refund tiers from policy evaluation |
| Additional charge | Post-booking charge line guest may pay online or at hotel |
| Nav module | Configurable staff sidebar entry with role allowlist; parents may be collapsible groups with navigable children |
| `FRONT_DESK` / `MANAGER` / `ADMIN` | Staff roles |

---

## 9. Design direction (UI theme research)

### 9.1 Visual goals

| Surface | Goal |
|---|---|
| **Guest** | Calm, trustworthy hospitality booking: clear search and checkout, photo-led hero, confident CTAs |
| **Staff** | Dense, readable operations UI: tables and forms first; brand color for primary actions; optional dark theme |

One brand system, two densities — not two unrelated products.

### 9.2 As-is defaults (engine shell)

Current seed defaults are neutral product UI:

| Token | Default | Character |
|---|---|---|
| Primary | `#1A1A1A` | Near-black |
| Secondary | `#F4F4F5` | Cool gray |
| Guest chrome (header/footer) | `#0F172A` / `#F8FAFC` | Slate |
| Body font | Geist Variable | Modern UI |
| Display | Archivo Black | Guest headlines |
| Radius | ~0.625rem | Soft, not pill-heavy |

**Assessment:** Safe for a generic app shell; weak as a hospitality brand story. Research recommends stronger hospitality defaults while keeping admin rebranding.

### 9.3 Options considered

| Option | Verdict | Why |
|---|---|---|
| Keep pure black / zinc forever as the only seed | Reject as sole direction | Under-communicates hotel product |
| Purple / indigo gradient “SaaS” look | Reject | Generic; weak hospitality signal |
| Warm cream + terracotta + display serif cliché | Reject | Overused pattern; conflicts with clear ops UI |
| Heavy gold / black “luxury” | Reject for v1 defaults | Accessibility and staff density suffer |
| Deep teal–green primary + warm sand surfaces + charcoal ink | **Accept** | Hospitality calm, distinct CTAs, works for guest and staff |

### 9.4 Recommended seed palette

| Role | Recommended hex | Use |
|---|---|---|
| Primary | `#0B4F4A` | Buttons, links, key accents |
| Primary foreground | `#F8FAF7` | Text on primary |
| Secondary / canvas | `#F3EFE7` | Guest page warmth / secondary surfaces |
| Ink | `#1C1917` | Body text |
| Guest header/footer bg | `#0F2F2C` | Chrome anchored to brand |
| Guest header/footer fg | `#F5F7F6` | Chrome text |
| Destructive | System red (e.g. `#DC2626`) | Errors, destructive actions — unambiguous |
| Success | Muted green (not neon) | Confirmed / paid indicators |

Staff light theme shares primary; staff dark theme keeps neutrals with branded primary on primary actions only.

### 9.5 Typography

| Role | Recommendation | Why |
|---|---|---|
| Default UI font (seed) | **Plus Jakarta Sans** | Warmer hospitality reading than pure geometric mono-brand stacks; still excellent for forms/tables |
| Allowlist alternatives | Geist Variable, Inter, DM Sans | Already suitable for branding admin |
| Guest display / hero | Keep a strong display face (e.g. Archivo Black or equivalent) | One expressive headline voice; never use display in dense staff tables |
| Production font loading | Prefer **self-hosted** fonts | Privacy, performance, simpler CSP; CDN fonts acceptable only for early prototype |

### 9.6 Shape, motion, imagery

- **Radius:** medium (~10px / 0.625rem family) — approachable, not pill-clustered  
- **Cards:** light elevation on guest marketing moments; staff prefers flat table/workspace surfaces  
- **Hero / search layout:** full-viewport hero; filter bar absolutely overlaid at bottom (negative margin into rooms section); location uses MapPin; CTA **Find rooms**  
- **Explore rooms:** centered Archivo/display title; carousel cards `lg` **⅓ width** (seed still Standard + Deluxe only); dots only (no home prev/next)  
- **Guests control:** +/- counters for rooms / adults / kids (not free-text only)  
- **Room cards:** multi-image gallery dots, amenity chips, refundable/promo badges, footer price + View details  
- **Checkout / catalog wizards:** numbered circle stepper with connectors (guest checkout + staff create-room catalog)  
- **Room search carousel:** on fine pointer, hovered card scales slightly; non-hovered cards darken with brand logo overlay when `logoUrl` is set  
- **Icons:** Lucide (or equivalent stroke icons) for guest filters and staff nav/top bar — not emoji substitutes  
- **Scrollbars:** thin (~6px) themed thumbs; guest shell may hide page scrollbars like live  
- **Staff chrome motion:** top bar scroll-hides with main pane scroll; denser filter bars on list pages; dashboard multi-chart stubs; booking ops action strip  
- **Reduced motion:** honor `prefers-reduced-motion` — disable or minimize non-essential motion  
- **Hero imagery:** default frontend asset `/images/hotel-hero.png` until replaced; photography is the primary visual  
- **Logo:** optional (`logoUrl` null in seed); guest text fallback “RMC Booking”; staff Hotel icon placeholder in logo slot; SoT may demo a hotel logo asset via toggle  
- **Focus:** visible `:focus-visible` on interactive controls (accessibility baseline)

### 9.7 Guest vs staff theme rules

| Rule | Guest | Staff |
|---|---|---|
| Canvas | Warm sand / soft paper (seed) or branded secondary | Neutral workspace (light default) |
| Primary CTA | Brand primary | Same primary for primary actions |
| Density | Spacious booking steps | Compact tables and filters |
| Dark mode | Not required for v1 guest | Required capability via **sidebar account dropdown** + profile (`LIGHT`/`DARK`) |
| Chrome | Scroll-aware header; nav underline; search on hero; guests stepper; gallery/carousel; checkout stepper | Logo slot, nav icons, avatar menu, global search + notifications; scroll-hide top bar; charts/filters/wizard/ops stubs |
| Branding overrides | Apply logo/colors/font/footer | Sidebar primary accents follow branding |

### 9.8 Locked vs configurable (design)

| Locked by product | Configurable by admin |
|---|---|
| Dual-surface model (guest vs staff denseness) | Primary / secondary hex |
| Semantic meaning of destructive/success | Logo upload |
| Payment UI must not invent “paid” from redirect | Font from allowlist |
| Accessibility contrast expectations on CTAs and text | Footer copy / contact / copyright |
| No redesign of IA via color alone | Header/footer chrome colors when exposed |

### 9.9 Why this direction

1. Signals **hotel booking**, not a generic admin template.  
2. Keeps **staff productivity** (contrast, density, optional dark).  
3. Preserves the existing **branding engine** (admins can still recolor).  
4. Avoids fragile luxury palettes and generic gradient trends.  
5. Seeded in `docs/prototype/rmc-booking.html` as the visual SoT for pixel-parity.

---

## 10. Information architecture snapshot

### 10.1 Guest routes

| Path | Purpose |
|---|---|
| `/` | Search / home |
| `/rooms/:roomTypeId` | Room detail |
| `/checkout` | Checkout |
| `/booking/confirmed` | Non-Maya / pay-at-hotel style confirmation |
| `/booking/success` | Post-Maya return UI (server-truth polling) |
| `/booking/lookup` | Find booking |
| `/booking/:reference` | Booking detail / cancel / pay charges |

Legacy or dead routes (e.g. unused date-only steps) should not be treated as product requirements unless explicitly revived.

### 10.2 Staff routes

| Path | Purpose |
|---|---|
| `/staff/login` | Staff login |
| `/staff/dashboard` | Dashboard |
| `/staff/arrivals` | Today's arrivals (Arrivals group) |
| `/staff/guests`, `/staff/guests/:id` | Guests (Arrivals group) |
| `/staff/bookings`, `/staff/bookings/:id` | All bookings |
| `/staff/rooms/config` | Room configuration |
| `/staff/rooms/catalog` | Create room (numbers / catalog / rate plans tabs) |
| `/staff/rooms/extras` | Extras |
| `/staff/rooms/operations` | View and update room |
| `/staff/settings` | General |
| `/staff/settings/refund-policy` | Refund policy |
| `/staff/settings/promos` | Promos & discounts |
| `/staff/settings/promo-codes` | Promo codes |
| `/staff/settings/audit` | Audit logs |
| `/staff/branding` | Branding (Settings group) |
| `/staff/users` | Staff users |
| `/staff/modules` | Sidebar modules |
| `/staff/profile` | Profile |

### 10.3 Critical UX product rules (not visual polish)

1. Success/confirmation copy must follow **server** booking/payment status.  
2. Guest cancel messaging must not claim a completed refund until refund status is complete (especially while refund is pending, failed, or staff-mediated).  
3. Staff modules the role cannot access must be unavailable in both UI and API.  
4. Checkout must collect required identity/contact and consent before booking creation.  

Detailed tokens and screenshot parity are owned by SPEC §3 / `RMC-SPEC-UX-001.10` and `docs/prototype/rmc-booking.html`.

---

## 11. Risks and residual opens

| Item | Severity | Notes |
|---|---|---|
| Final legal hotel name, locked marketing copy, official logo assets | Medium | Business must supply; branding admin can upload logo once available |
| Exact production hosting vendor | Low for product SPEC | Ops decision; does not change feature scope |
| Timing of dedicated visual prototype | Resolved | `docs/prototype/rmc-booking.html` is the visual SoT (`RMC-SPEC-UX-001.10`) |
| Multi-instance rate limiting without Redis | Low | Mitigated by D14 |
| Auto-refund edge cases (partial captures, chargebacks) | Medium | SPEC must detail failure/retry and staff override; ops runbooks later |

No residual open should silently expand v1 into multi-property or guest accounts.

---

## 12. Recommended stack posture (research-level)

This is orientation for SPEC authors — not a deployment runbook.

| Layer | Posture |
|---|---|
| Frontend | Vite + React SPA (guest + staff) · Tailwind CSS · **shadcn/ui** (radix-nova / neutral tokens) · Lucide · branding CSS variables |
| Backend | JVM API service with relational schema migrations |
| Database | MySQL (or compatible) with migration-first schema |
| Auth | Staff JWT access + refresh; optional/required MFA per D9 |
| Payments | Maya Business hosted Checkout + webhook verification |
| Async | Schedulers for lifecycle + email outbox; durable enough for retries |
| Files | Local or mounted upload directory for images/logo |
| Rate limit | In-memory or Redis per D14 |
| Staff nav UX | Nested **collapsible** sidebar groups (parents expand/collapse; active child keeps group open) |

Exact versions and hosting topology are finalized in SPEC / ops docs without changing the product definition here.

---

## 13. Research → SPEC checklist

When writing the SPEC from this research:

1. Use a **greenfield** voice: describe the system to build (current + target behaviour such as auto-refund), not delivery status tables.  
2. Assign **`RMC-SPEC-*` and sub-IDs** to every normative requirement.  
3. Map each major journey in §5 to requirements + acceptance criteria.  
4. Encode §6 decisions as non-negotiables or feature rules.  
5. Encode §7 as settings capabilities vs hard rules.  
6. Use §8 glossary consistently.  
7. Put §9 seed palette/fonts into UX/branding defaults; keep admin override.  
8. Use §10 as normative IA unless a revised research version changes routes.  
9. Keep §3.2 out of scope unless research is revised.  
10. Do not treat configurable numbers (tax %, cutoffs) as immutable product law.

---

## 14. Document control

| Field | Value |
|---|---|
| Document | `docs/RESEARCH.md` |
| Version | 1.0.5 |
| Role | Product research baseline for SPEC |
| Companion | `docs/SPEC.md` v1.0+ · `docs/VALIDATION.md` · `docs/prototype/rmc-booking.html` · `docs/AC_COVERAGE.md` · `docs/VALIDATION_RUN.md` · `docs/README.md` |

### Change policy

- Product scope or decision changes → update this file and bump version.  
- SPEC must not contradict this baseline without an explicit research revision.  

---

## Appendix A — Feature spine summary (quick reference)

**Guest:** Search → Room → Checkout (extras, guests, consent, pay) → Confirm/Success → Lookup → Cancel / pay charges  

**Staff:** Login → Dashboard / Arrivals (Today's arrivals, Guests) / Bookings / Rooms (config, create, extras, ops) / Settings (general, refund, promos, promo codes, audit, branding) / Staff users / Sidebar modules / Profile  

**Money:** Server quote (+ optional promo code) → Hold → Maya or pay-at-hotel → Webhook/poll truth → Ledger → Policy cancel → Auto or staff refund  

**Trust:** No client prices · No redirect-alone paid · No PAN on origin · RBAC + audit on sensitive actions  

---

## Appendix B — Seed branding values (for environments)

Use as default seed when creating a fresh environment (admins may change afterward):

```text
company / product label: RMC Booking (until legal name provided)
fontFamily: Plus Jakarta Sans
primaryColor: #0B4F4A
secondaryColor: #F3EFE7
headerBackgroundColor: #0F2F2C
headerForegroundColor: #F5F7F6
footerBackgroundColor: #0F2F2C
footerForegroundColor: #F5F7F6
```

Footer contact and copyright remain empty or placeholder until business supplies final copy.
