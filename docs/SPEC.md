# RMC Booking System — Production Build Specification

**Version:** 1.0  
**ID scheme:** `RMC-SPEC-<AREA>-<NNN>` · sub-specs `RMC-SPEC-<AREA>-<NNN>.<n>`  
**Research baseline:** `docs/RESEARCH.md` v1.0.9  
**Visual source of truth:** `docs/prototype/rmc-booking.html`  
**Audience:** Engineering (build from this contract)  
**Primary mandate:** Build a single-property hotel booking system: guest search → server-side quote → booking → Maya hosted checkout or pay-at-hotel → staff operations, refunds, promos, branding, and audit.

### Spec-first mandate

| Rule | Meaning |
|---|---|
| **Research first** | Product intent lives in `docs/RESEARCH.md`. Change research, then this SPEC. |
| **SPEC is the build contract** | Describe the system to build (all in-scope behaviour), not a changelog of an existing repo. |
| **IDs on every requirement** | Every normative rule has an `RMC-SPEC-*` ID or sub-ID. |
| **Acceptance elsewhere** | `docs/VALIDATION.md` holds `AC-*` proofs; `docs/VALIDATION_RUN.md` holds dated evidence. |

> **Prime directive:** Guest-facing success pages are UI only. For `ONLINE_MAYA`, verified Maya signals decide whether money is paid. Confirm-payment poll is an allowed secondary path that asks Maya for checkout status. Never invent paid state from the browser redirect alone.

---

## 0. How to use this document

| ID | Requirement |
|---|---|
| **RMC-SPEC-META-001** | This SPEC is the normative build contract for behaviour, security, and operations of the RMC Booking System. |
| RMC-SPEC-META-001.1 | Implement from the owning `RMC-SPEC-*` ID; do not invent payment, inventory, or auth rules. |
| RMC-SPEC-META-001.2 | `docs/VALIDATION.md` defines acceptance criteria mapped to these SPEC IDs; an AC PASS (with coverage owner + VALIDATION_RUN evidence) is required to claim a SPEC requirement is proven. |
| RMC-SPEC-META-001.3 | `docs/VALIDATION_RUN.md` records dated PASS / FAIL / PARTIAL evidence. |
| RMC-SPEC-META-001.4 | Secrets never belong in git; document env **names** and purposes only (Appendix B). |
| RMC-SPEC-META-001.5 | When behaviour changes, update the owning SPEC section/appendix with the change. |
| RMC-SPEC-META-001.6 | Build phase-by-phase (§17). Do not go live before payment truth, holds, and auth controls in this SPEC are satisfied. |
| RMC-SPEC-META-001.7 | Configurable hotel knobs (tax %, cutoffs, branding hex) are settings — do not hardcode them as immutable product law. |

**Sources**

| Source | Role |
|---|---|
| `docs/README.md` | Index of the doc set |
| `docs/RESEARCH.md` | Agreed product baseline (scope, decisions, design direction) |
| `docs/prototype/rmc-booking.html` | **Visual source of truth** — mirrors current `rmc_frontend` IA, density, and live `DEFAULT_BRANDING` tokens; critical UX messaging (payment/refund truth) |
| **This SPEC + appendices** | Normative behaviour, APIs, Maya contract, status machine, pricing, RBAC, schema |
| `docs/VALIDATION.md` | `AC-*` acceptance criteria |
| `docs/AC_COVERAGE.md` | AC ownership map |
| `docs/VALIDATION_RUN.md` | Dated evidence |
| Maya official docs | Wire details for PayMaya not duplicated here |

---

## 1. Non-negotiables

| ID | Requirement |
|---|---|
| **RMC-SPEC-NN-001** | Money, inventory, and staff authorization controls must never be weakened. |
| RMC-SPEC-NN-001.1 | Online payment state advances only from verified Maya signals (webhook preferred; Maya GET poll via confirm-payment allowed). Never from browser redirect alone. |
| RMC-SPEC-NN-001.2 | PHP only; amounts computed server-side from rates, taxes, fees, promos, extras. Never trust client totals. |
| RMC-SPEC-NN-001.3 | Inventory holds must prevent double-selling of the same room-night under concurrency. |
| RMC-SPEC-NN-001.4 | Booking, payment, and refund operations must be auditable and idempotent where applicable. |
| RMC-SPEC-NN-001.5 | Staff actions are RBAC-protected for `FRONT_DESK`, `MANAGER`, and `ADMIN` (module ACL + hard gates). |
| RMC-SPEC-NN-001.6 | Guest consent / privacy capture must be stored as part of the booking flow. |
| RMC-SPEC-NN-001.7 | Hosted checkout only; no raw card PAN or CVV may touch the RMC origin. |
| RMC-SPEC-NN-001.8 | Sandbox and production Maya credentials must stay separated. |
| RMC-SPEC-NN-001.9 | Single-property v1; no multi-property inventory. |
| RMC-SPEC-NN-001.10 | No guest accounts in v1; lookup by booking reference + email. |

---

## 2. Recommended tech stack

| Layer | Choice |
|---|---|
| Frontend | **Vite** + **React** SPA (guest + staff) · **Tailwind CSS v4** · **shadcn/ui** (`radix-nova`, `baseColor: neutral`, CSS variables) · **Lucide** icons · branding CSS variables on `:root` |
| Backend | JVM API service (Spring Boot), port configurable (default 8082) |
| DB | MySQL + Flyway migrations; ORM validate-only against migrations |
| Auth | JWT access + refresh; staff MFA enroll/verify |
| Payments | Maya Business hosted Checkout + webhook |
| Async | Schedulers for booking lifecycle + email outbox |
| Rate limit | In-memory or Redis (Redis required when running multiple API instances) |
| Uploads | Configured upload directory served at `/uploads/**` |
| API docs | OpenAPI UI (e.g. `/api/docs`, `/api/swagger`) |

| ID | Requirement |
|---|---|
| **RMC-SPEC-STACK-001** | Stack table above is normative for the build unless research is revised. |
| RMC-SPEC-STACK-001.1 | Production hosting vendor may be chosen by ops; product assumes managed web + API + MySQL. |
| RMC-SPEC-STACK-001.2 | Redis is optional for single-instance; mandatory for shared guest booking rate limits across instances. |
| RMC-SPEC-STACK-001.3 | Observability minimum: structured logs, correlation across checkout→webhook→booking, error tracking (§12). |
| RMC-SPEC-STACK-001.4 | Guest/staff UI primitives are built from **shadcn/ui** + Tailwind tokens; do not replace with a different component library that changes visual language without updating RESEARCH, SPEC §3, and the visual SoT. |

---

## 3. UI / UX — design system

### 3.1 Visual goals

| Surface | Goal |
|---|---|
| Guest | Calm hospitality booking: photo-led hero, clear search/checkout, confident CTAs |
| Staff | Dense readable ops: tables/forms first; brand primary on actions; light default + optional dark theme (`RMC-SPEC-UX-001.11`) |

**Visual SoT:** `docs/prototype/rmc-booking.html` (`RMC-SPEC-UX-001.10`).

| ID | Requirement |
|---|---|
| **RMC-SPEC-UX-001** | Dual-surface model: spacious guest booking UI + dense staff ops UI sharing one brand system. |
| RMC-SPEC-UX-001.1 | Guest routes in §4 are normative IA. |
| RMC-SPEC-UX-001.2 | Staff routes in §4 are normative IA. |
| RMC-SPEC-UX-001.3 | Success/confirmation copy must follow **server** booking/payment status. |
| RMC-SPEC-UX-001.4 | Guest cancel messaging must not claim a completed refund until refund status is complete. |
| RMC-SPEC-UX-001.5 | Staff modules a role cannot access must be unavailable in both UI and API. |
| RMC-SPEC-UX-001.5a | Staff sidebar uses a **nested, collapsible** module tree: parent groups (e.g. Arrivals, Bookings, Rooms, Settings) expand/collapse; a group auto-opens when a child route is active; leaf items navigate. The staff chrome sidebar itself is collapsible / offcanvas on smaller viewports (shadcn Sidebar pattern). |
| RMC-SPEC-UX-001.6 | Checkout must collect required identity/contact and consent before booking creation. |
| RMC-SPEC-UX-001.7 | Runtime branding (logo, colors, font, footer) is administered by staff and applied via CSS variables. |
| RMC-SPEC-UX-001.8 | Seed branding defaults for new environments are Appendix A. Until Flyway/seed applies Appendix A, runtimes may still show code `DEFAULT_BRANDING` (`#1a1a1a` / `#f4f4f5` / Geist) — the visual SoT currently mirrors that live default. |
| RMC-SPEC-UX-001.9 | Prefer self-hosted fonts in production; honor `prefers-reduced-motion`; visible `:focus-visible` on controls. |
| RMC-SPEC-UX-001.10 | Visual source of truth: `docs/prototype/rmc-booking.html`, kept aligned to current `rmc_frontend`. **SoT fidelity scope:** (a) guest chrome + home/search/carousel/checkout critical UX (incl. `RMC-SPEC-UX-001.17`), (b) staff chrome (sidebar, footer account menu, top bar) + representative interaction patterns (`RMC-SPEC-UX-001.18`–`.19`), (c) live default tokens + payment/refund messaging, (d) theme/motion behaviours below. Staff **module page bodies** may be representative stubs (tables/forms/charts/wizard shape) unless a screen is listed as chrome-critical. Pixel-parity (`AC-UI-001`) applies to scope (a)–(c) at desktop and ~390px. |
| RMC-SPEC-UX-001.11 | Staff supports **light and dark** themes (`LIGHT` / `DARK`), toggled from the **sidebar account dropdown** and/or profile page, persisted as `themePreference`, applied via root `.dark` + CSS variables. Brand primary remains usable for primary actions in both themes. Guest UI does not require dark mode in v1. |
| RMC-SPEC-UX-001.12 | Motion and hover are part of the product look: short transitions on controls/nav (≈150–200ms); primary booking CTAs may use restrained hover lift/brightness; guest hero/reveal restrained; staff tables use row hover without decorative noise. **Room search carousel (fine pointer):** hovered card scales slightly and elevates; **non-hovered** cards show a dark overlay with the brand mark centered when `logoUrl` is set. Honor `prefers-reduced-motion` (`RMC-SPEC-UX-001.9`). |
| RMC-SPEC-UX-001.13 | **Brand mark / logo:** `logoUrl` is optional (DB seed default `NULL`). When set, show `BrandMark` in guest header/footer and staff sidebar header (fixed slot ≈ `h-15 w-20`, object-contain). When unset: guest shows text **“RMC Booking”** (or `companyName`); staff sidebar shows a **Hotel icon** placeholder in the same slot. Carousel inactive overlay uses the logo only when present. SoT may demo with `docs/prototype/assets/ramada-manila-central-logo.png` via toggle (not seed default). |
| RMC-SPEC-UX-001.14 | **Guest chrome:** sticky header with scroll-aware hide/show; Book / Find booking / Staff nav with active underline indicator; footer with brand, footer text, quick links, contact. Home search is a white **compact horizontal filter bar overlaid on the hero** at all breakpoints (Hotel / Check-in / Check-out / Guests / Promo / Search — one row, not a tall stacked card). Hero CTA label **Find rooms**. Default hero image path: `/images/hotel-hero.png` until replaced by product photography. |
| RMC-SPEC-UX-001.15 | **Staff chrome:** sidebar header brand slot (`RMC-SPEC-UX-001.13`); “Navigation” label; Lucide (or equivalent) icons on modules; collapsible groups (`RMC-SPEC-UX-001.5a`); footer **account control** = avatar (image or initials) + name + role + chevron opening a menu: Profile, Light/Dark mode, Log out; top inset bar with sidebar trigger, **global search**, and **notifications**. |
| RMC-SPEC-UX-001.16 | **Demo catalog defaults** for SoT / fresh seed alignment: hotel display “RMC Hotel” (Manila copy per frontend `HOTELS`); Flyway seed room types **Standard Room** (₱2,500/night) and **Deluxe Room** (₱3,800/night) per Appendix M — SoT must not invent conflicting catalog names/prices. |
| RMC-SPEC-UX-001.17 | **Guest interaction fidelity (SoT):** search guests control uses +/- counters (rooms/adults/kids) in a **compact horizontal** filter bar overlaid on the hero (live `BookingFilters` shape, all breakpoints); room cards match stack catalog layout (gallery, MapPin meta, refundable / free-cancellation badges with a small info control that reveals the plan’s refund-policy description, footer price + View details); room carousel uses **lg ⅓ card width** (live `basis-1/3`) with **dots only** (no prev/next on home); “Explore our Rooms” is centered display typography; guest page enter + restrained scroll-reveal; hero uses MapPin location + “Find rooms” CTA + staggered slide-in. **Checkout** is one route with five in-page steps (Room → Extras → Guest → Payment → Confirm); only the active step pane is visible; content sits in guest layout width (~`max-w-6xl` + responsive padding); panel stack + sticky price sidebar with hero image; footer shows **Back** + **Next** until the last step, then **Continue to Maya payment** or **Request pay-at-hotel booking** (never a generic “Place booking” on every step); primary booking CTAs use **h-12 / text-base** sizing; guest content cards use **shadow without stroke border**. |
| RMC-SPEC-UX-001.18 | **Staff interaction fidelity (SoT chrome + representative bodies):** staff top bar scroll-hides with main pane scroll; page body uses short enter transition; dashboard shows multi-chart occupancy/movement/revenue stubs + date filter bar; list pages use denser filter bar; room catalog includes multi-step wizard UI (product fields only — no owned price) and delete actions for room types and room numbers; **Rate plans** staff page lists plans with per-plan sample nightly rate and create/edit (room catalog selector required on create); room operations includes **View bookings** per unit; booking detail shows fuller ops action strip + audit table; **refund policy** staff screen is the **default template for new rate plans** (name, enabled, full cutoff + unit, partial %, check-in time, timezone, **required** description, manual-refund toggle); **General settings** taxes & fees card exposes service charge, VAT, and municipal tax toggles/percents; other cards: system configuration, rate plans (+ daily rate batch / hold TTL), room types, room units, and account MFA enroll/disable. |
| RMC-SPEC-UX-001.19 | **Shared staff modal (`StaffModal`):** create/edit flows that use a dialog (e.g. promos) open a shared modal with fixed header (title + optional description), scrollable body, and footer actions (Cancel / primary Save). SoT demos the pattern on Promos; live implementation is `StaffModal` / `StaffModalContent` over the Dialog primitive. Size variants (sm–xl / wizard) may be used; modal must trap focus and dismiss via Cancel, explicit close, or backdrop per product Dialog behaviour. |
| RMC-SPEC-UX-001.20 | **In-app feedback:** Destructive or irreversible guest/staff actions use an in-app **confirmation dialog** (not the browser `window.confirm` / `prompt`). Blocking network actions show a **loading overlay** (and/or busy button). Success and failure are surfaced via **in-app alerts/toasts** (or existing inline `StaffAlert` / guest `Alert`), never silently. Guest checkout submit confirms the booking request, then shows loading until Maya redirect or confirmation navigation. |

### 3.2 Design tokens

**Live product defaults** (code `DEFAULT_BRANDING` + `index.css` / guest chrome — mirrored in the visual SoT):

| Role | Value |
|---|---|
| Primary | `#1a1a1a` |
| Primary foreground | `#fafafa` (contrasting) |
| Secondary / muted canvas | `#f4f4f5` |
| Guest chrome bg/fg | Secondary-driven when branding applied (fallback slate `#0f172a` / `#f8fafc` in CSS before apply) |
| Default UI font | Geist Variable (allowlist: Geist, Inter, DM Sans, Plus Jakarta Sans) |
| Guest display | Archivo Black for hero headlines only |
| Radius | `--radius: 0.625rem` family |

**Intended hospitality seed** for fresh environments: Appendix A (`#0B4F4A` / sand / Plus Jakarta). Product work should converge seed migration + SoT when Appendix A is applied.

### 3.3 Branding administration

Admins can override primary/secondary, font (allowlist), logo, and footer contact/copyright. Semantic meanings of destructive/success and payment-truth UX rules are **not** overridable by color alone.

---

## 4. Information architecture and routes

### 4.1 Guest

| Path | Purpose |
|---|---|
| `/` | Search / home |
| `/rooms/:roomTypeId` | Room detail |
| `/checkout` | Checkout |
| `/booking/confirmed` | Pay-at-hotel / non-redirect confirmation |
| `/booking/success` | Post-Maya return UI (polls confirm-payment) |
| `/booking/lookup` | Lookup by reference |
| `/booking/:reference` | Booking detail / cancel / pay charges |

Guest nav includes Book, Find booking, and entry to staff login.

### 4.2 Staff

Default sidebar labels/paths match `rmc_frontend` `DEFAULT_STAFF_MODULES` (API may override order/enablement).

**Nav behaviour (`RMC-SPEC-UX-001.5a`):** parents with `path: '#'` are collapsible groups; children are the navigable routes. Groups with an active child stay expanded. Profile lives in the sidebar footer (always allowed when authenticated), not as a top-level module.

| Path | Nav label (default) | Purpose |
|---|---|---|
| `/staff/login` | — | Staff login (+ MFA when required) |
| `/staff/dashboard` | Dashboard | Dashboard KPIs / charts / recent bookings |
| `/staff/arrivals` | Today's arrivals (under Arrivals) | Date-filtered arrivals |
| `/staff/guests`, `/staff/guests/:id` | Guests (under Arrivals) | Guest directory / profile |
| `/staff/bookings`, `/staff/bookings/:id` | All bookings (under Bookings) | Booking list / ops detail |
| `/staff/rooms/config` | Room configuration | Config option values |
| `/staff/rooms/catalog` | Create room | Tabs: room numbers; full catalog (product + units); rate plans (policy + pricing) |
| `/staff/rooms/extras` | Extras | Services / items |
| `/staff/rooms/operations` | View and update room | Daily ops / calendar; view all bookings per unit |
| `/staff/settings` | General | Taxes, system config, rates |
| `/staff/settings/refund-policy` | Refund policy | Named cancel/refund **policies** CRUD (partial % + optional nights deduction) |
| `/staff/settings/promos` | Promos & discounts | Automatic public promo CRUD (no guest code) |
| `/staff/settings/promo-codes` | Promo codes | Access-rate promo code CRUD (special / corporate / agency) |
| `/staff/settings/audit` | Audit logs | Login + activity audit |
| `/staff/branding` | Branding (under Settings) | Logo / colors / font / footer |
| `/staff/users` | Staff users | User administration |
| `/staff/modules` | Sidebar modules | Nav module ACL admin |
| `/staff/profile` | Profile (footer) | Always allowed when authenticated |

---

## 5. Data model (sketch)

Relational MySQL. Schema evolves via Flyway. Money events recorded in a ledger. Normative table list: **Appendix K**.

Core concepts:

- **Guest** — identity/contact for a booking (no login account)  
- **Booking** — stay, status, payment method, quoted totals, Maya refs, refund fields, policy snapshot link  
- **Inventory hold** — per night, ACTIVE/RELEASED/CONSUMED  
- **Room type / unit** — inventory catalog (name + physical units); no owned guest product or price  
- **Rate plan / daily rates** — guest-facing product under a catalog (display fields + price + refund-policy link); inherits catalog units  
- **Promo** — automatic public discount: percent or fixed PHP, window, room-type scope (no guest code)  
- **Promo code** — guest-entered access rate: type, offer/org codes, rate-plan scope, % or fixed, max uses  
- **Extras** — service/item add-ons + booking selections  
- **Additional charge** — post-booking charge with its own payment lifecycle  
- **Named refund policy** + **booking refund policy snapshot** (snapshot from booked rate plan’s linked policy)  
- **Branding config** — logo, colors, font, footer  
- **Staff user** + **refresh token** + **nav modules**  
- **Email outbox**, **staff notifications**, **audit logs**, **system config**

| ID | Requirement |
|---|---|
| **RMC-SPEC-DATA-001** | Persist domain entities required by this SPEC; schema changes via migrations only. |
| RMC-SPEC-DATA-001.1 | `booking_ledger` records money-affecting events. |
| RMC-SPEC-DATA-001.2 | Refund evaluation uses the snapshot attached to the booking. |
| RMC-SPEC-DATA-001.3 | Currency is PHP for all monetary fields. |

---

## 6. Architecture and payment flow

```
Guest checkout (ONLINE_MAYA)
  → server creates booking PENDING_PAYMENT + inventory holds
  → server creates Maya checkout (server-side amount)
  → browser redirects to Maya hosted page
  → Maya → webhook POST /api/payments/maya/webhook  (authoritative push)
  → and/or guest return → POST .../confirm-payment   (poll Maya GET)
  → on success: apply ledger CREDIT + advance status per Appendix E
  → staff approve when required → CONFIRMED
```

| ID | Requirement |
|---|---|
| **RMC-SPEC-ARCH-001** | Webhook is the preferred authoritative push; poll is secondary Maya-truth path. |
| RMC-SPEC-ARCH-001.1 | Browser success URL alone must never mark paid. |
| RMC-SPEC-ARCH-001.2 | Payment updates must be applied idempotently. |
| RMC-SPEC-ARCH-001.3 | Correlate checkout → payment → webhook/poll → booking/audit in logs. v1: echo HTTP `X-Correlation-Id` on every request; put booking reference in log MDC on checkout, webhook/poll, and booking mutations. Maya webhooks correlate by `requestReferenceNumber` (booking reference). |
| RMC-SPEC-ARCH-001.4 | Retain enough webhook/payment evidence to debug and replay safely. v1: persist each webhook and confirm-poll `MayaCheckoutStatus` as redacted JSON plus SHA-256, with booking reference, checkout id, status, amount, source (`MAYA_WEBHOOK` / `MAYA_CONFIRM_POLL`), and correlation id (`maya_payment_event`). Duplicate receipts are stored; applying payment remains idempotent. Evidence failure must not block payment application. |

Full Maya wire rules: **Appendix F**.

---

## 7. Guest booking behaviour

| ID | Requirement |
|---|---|
| **RMC-SPEC-GUEST-001** | Guest can search, view rooms, quote, checkout, look up booking, cancel, and pay additional charges. |
| RMC-SPEC-GUEST-001.1 | Availability search returns only sellable room types for the requested stay (respecting holds and config). For each type, the card shows room-type product media/meta (description, amenities, images, class/view/bed) and **From** = minimum tax-inclusive stay total among active rate plans with complete daily rates (Appendix G). When a valid promo code is supplied, From and plan prices reflect that code’s discount on eligible plans. |
| RMC-SPEC-GUEST-001.2 | Quotes are computed server-side (Appendix G) for a **selected rate plan** (re-validating any applied promo code). |
| RMC-SPEC-GUEST-001.2a | Guest must select an active rate plan for the room type on room detail before checkout; detail shows room-type product and lists plans as **name + price + policy** with the **lowest-priced plan pre-highlighted**. Booking create requires that `ratePlanId` and rejects inactive or mismatched plans. Room media/copy come from the room type; selected-plan badges reflect that plan’s refund policy. |
| RMC-SPEC-GUEST-001.2b | Home search filter bar includes a **promo-type dropdown** (none / special / corporate / agency). Selecting a type reveals the required code field(s) and an **Apply** control. Special rates require offer code only; corporate/agency require organization code + offer code. Apply validates the selected type against the code; invalid/exhausted/mismatch show an **error alert** and leave rack prices unchanged. Successful apply shows a **success alert**. Search uses a previously applied code until cleared. |
| RMC-SPEC-GUEST-001.2c | Guest **Refundable** and **Free cancellation** badges (search cards, room detail, checkout summary) include a small info control when the selected/cheapest plan has a policy description. Activating it reveals that guest-facing description (how refund/cancel works). Cancel and refund share one named policy — the same description is shown for both badges. When plans on a room type differ, the card shows **policies vary** instead of a single description; each plan on room detail has its own badges + info. |
| RMC-SPEC-GUEST-001.3 | Checkout captures guest identity/contact, optional additional guests within capacity, extras, special request, and consent. |
| RMC-SPEC-GUEST-001.4 | Payment choice is `ONLINE_MAYA` or `PAY_AT_HOTEL`. |
| RMC-SPEC-GUEST-001.5 | Lookup by booking reference + email returns booking detail without a guest account. |
| RMC-SPEC-GUEST-001.6 | Success/confirmation pages reflect server truth (status / confirm-payment), not redirect cosmetics alone. |
| RMC-SPEC-GUEST-001.7 | Email outbox items are created for configured lifecycle events (Appendix L). When mail is enabled, a **booking-received** email is queued as soon as the guest booking is created (Maya `PENDING_PAYMENT` or pay-at-hotel `PENDING_APPROVAL`), and **must include the booking reference** plus stay summary so the guest can use Find booking. Staff approval still queues a separate **confirmation** email. |
| RMC-SPEC-GUEST-001.8 | Guest UI presents branding from public branding API. |

---

## 8. Inventory and holds

See **Appendix H**.

| ID | Requirement |
|---|---|
| **RMC-SPEC-INV-001** | Holds reserve inventory for pending bookings. |
| RMC-SPEC-INV-001.1 | ACTIVE holds exclude inventory from availability. |
| RMC-SPEC-INV-001.2 | Expired holds are released by scheduler. |
| RMC-SPEC-INV-001.3 | Availability respects configured buffers/windows and unit status. |
| RMC-SPEC-INV-001.4 | Concurrent checkout attempts must never double-sell the same inventory. |

---

## 9. Payments and booking status

Payment methods: `ONLINE_MAYA`, `PAY_AT_HOTEL`.  
Statuses and transitions: **Appendix E**. Maya: **Appendix F**.

| ID | Requirement |
|---|---|
| **RMC-SPEC-PAY-001** | Both online Maya and pay-at-hotel flows exist. |
| RMC-SPEC-PAY-001.1 | Maya checkout create uses server-derived booking totals. |
| RMC-SPEC-PAY-001.2 | Webhooks are verified (IP / shared-secret / HMAC as configured) before application. |
| RMC-SPEC-PAY-001.3 | Duplicate payment events do not double-write ledger or status. |
| RMC-SPEC-PAY-001.4 | Pay-at-hotel → `PENDING_APPROVAL` then staff → `CONFIRMED_PAY_LATER` (or reject/cancel). |
| RMC-SPEC-PAY-001.5 | Online success advances per Appendix E (typically toward staff approval then `CONFIRMED`). |
| RMC-SPEC-PAY-001.6 | Additional charges support Maya pay and pay-at-hotel / staff recording paths. |
| RMC-SPEC-PAY-001.7 | Sandbox and production Maya secrets/URLs are never mixed. |
| **RMC-SPEC-PAY-002** | Status transitions are service-controlled (Appendix E). |
| RMC-SPEC-PAY-002.1 | Online success cannot be inferred from browser alone. |
| RMC-SPEC-PAY-002.2 | On Maya success, received amount must match booking quoted total (mismatch → do not confirm; log error). |

---

## 10. Cancellation and refunds

Cancel/refund rules live on **named refund policies**. Each rate plan references a policy. At book time the system **snapshots** fields from the **linked policy** onto the booking. Cancel evaluation uses the snapshot.

| ID | Requirement |
|---|---|
| **RMC-SPEC-CXL-001** | Guest and staff cancel paths evaluate the booking refund policy snapshot (FULL / PARTIAL / NONE). |
| RMC-SPEC-CXL-001.1 | FULL / PARTIAL / NONE outcomes follow the snapshot: FULL = 100% refund before cutoff; PARTIAL may apply nights deduction then partial % (see `.1b`); NONE / blocked per policy. |
| RMC-SPEC-CXL-001.1b | **PARTIAL stack:** if nights deduction enabled, retain fee = sum of first `min(N, stayNights)` nights’ room charges; then if partial % enabled apply percent to remaining; else remaining is refundable. FULL window ignores nights/partial. |
| RMC-SPEC-CXL-001.2 | Cancellation/refund evaluation uses the snapshot, not only live mutable policy or rate-plan edits. |
| RMC-SPEC-CXL-001.2a | New booking snapshots copy cancel/refund fields from the **refund policy linked to the booked rate plan** (including refundable and nights fields). |
| RMC-SPEC-CXL-001.3 | Staff Maya refund and (when enabled) manual refund actions are audited and update booking + ledger state. Same-day full reverse may prefer Maya **void**; if Maya rejects void (e.g. `PY0045`) on the payment day, do **not** call refund yet — Maya API refunds are only eligible after 12:00 AM Asia/Manila the next day (`PY0047` otherwise). After that cutoff, fall back to Maya refund. |
| RMC-SPEC-CXL-001.4 | Manual refund methods include at least GCash, bank transfer, cash, other (when manual refunds enabled). |
| RMC-SPEC-CXL-001.5 | **Auto-refund on guest cancel:** Given a paid `ONLINE_MAYA` booking and guest cancel allowed by policy with refund-eligible amount > 0, when cancel succeeds, the server initiates Maya reverse for that amount without requiring staff approval (same void-then-wait-then-refund timing as `.3`), records ledger/refund status, and audits the attempt. `PENDING` auto-refunds are retried after the Manila cutoff. |
| RMC-SPEC-CXL-001.6 | Auto-refund amount equals the policy-evaluated eligible amount (capped by Maya-refundable balance); zero-eligible cancels must not call Maya refund. |
| RMC-SPEC-CXL-001.7 | **Auto-refund failure:** if Maya refund fails after cancel, booking stays cancelled, refund marked failed/pending-retry, staff can complete via refund endpoints, and the failure is audited/logged. |
| RMC-SPEC-CXL-001.8 | Pay-at-hotel / non-Maya paid balances are out of scope for auto Maya refund; they use staff/manual paths. |
| RMC-SPEC-CXL-001.9 | Guest UI must not claim refund completed until refund status is complete. |

---

## 11. Staff operations

| ID | Requirement |
|---|---|
| **RMC-SPEC-STAFF-001** | Staff can operate dashboard, arrivals, bookings, guests, rooms, settings, branding, users, modules, and profile per RBAC. |
| RMC-SPEC-STAFF-001.1 | Arrivals list supports date-filtered expected arrivals. |
| RMC-SPEC-STAFF-001.2 | Booking detail supports approve/reject, check-in (assign unit), transfer, check-out, folio payment recording, additional charges, status override (allowed map), and refunds per role. Detail also shows booking `createdAt`; when approved or rejected, surfaces actor (staff name + email) and timestamp from audit (pay-at-hotel and Maya paths); and includes guest service/item selections plus `customExtrasRequest` notes for prep. |
| RMC-SPEC-STAFF-001.3 | Guest directory and profile show identity and stay history. |
| RMC-SPEC-STAFF-001.4 | Rooms modules cover config options (including amenities vocabulary), Create room page (numbers / full catalog product+units / rate plans tabs), extras, and daily operations/calendar. |
| RMC-SPEC-STAFF-001.4g | Room configuration maintains a reusable **amenities** vocabulary (`AMENITY` options) alongside category / view / bed / status. Room-type create/edit selects amenities from that list (add via dropdown; remove via chip). Room types store selected amenity labels; removing a config amenity is blocked while any room type still lists that label. |
| RMC-SPEC-STAFF-001.4a | Room-type delete: hard-delete when unused by bookings or inventory holds (unlink units, remove owned rate plans/daily rates/images); if bookings or inventory holds reference the type, deactivate (`active=false`) instead and return that outcome (same pattern as extras). |
| RMC-SPEC-STAFF-001.4b | Room operations expose a paged list of all bookings ever assigned to a room unit (past, current, and future). |
| RMC-SPEC-STAFF-001.4c | Room-number (unit) delete: hard-delete when not assigned to an active booking (`checkedOutAt` null). If historical bookings still reference the unit, unlink them (`room_unit_id = null`) before delete. Decrement parent room-type `totalCapacity` when applicable (skip when unit is `OUT_OF_ORDER`). Audit the deletion. |
| RMC-SPEC-STAFF-001.4d | Staff can create/update/deactivate multiple rate plans per room type; each plan owns daily rates and **references** a named refund policy (`refundPolicyId`), plus hold TTL / active. Plans do not own guest product fields. Availability uses the parent room type’s units. Each plan has a **primary nightly rate** (default for upcoming nights) and optional **date-range overrides** that set specific calendar nights without replacing the primary. |
| RMC-SPEC-STAFF-001.4e | Rate plans are managed on the Create room **Rate plans** tab (`/staff/rooms/catalog`) via a short wizard (catalog → refund policy → plan name/hold → pricing). Room-type create/edit uses the full product wizard (numbers, class, details, media, visibility, preview). |
| RMC-SPEC-STAFF-001.4f | Room type create/edit is the source of truth for guest-facing product fields; rate plans configure policy and pricing only. |
| RMC-SPEC-STAFF-001.5 | Settings cover system/tax keys, rates (hold TTL / daily-rate batch), named refund-policy CRUD, automatic promos, **promo codes**, and audit. |
| RMC-SPEC-STAFF-001.6 | Branding admin updates logo, colors, font, footer. |
| RMC-SPEC-STAFF-001.7 | Users and modules administration is ADMIN-scoped; privileged ops are audited. |
| RMC-SPEC-STAFF-001.8 | Global search and in-app notifications are available to authenticated staff (staff top bar). |
| RMC-SPEC-STAFF-001.9 | Staff account menu (sidebar footer avatar dropdown) and profile can toggle light/dark theme per `RMC-SPEC-UX-001.11`. |
| RMC-SPEC-STAFF-001.10 | Staff login supports an MFA verification step when required by policy/user enrollment. |

RBAC detail: **Appendix I**.

---

## 12. Auth, security, and observability

### 12.1 Auth and RBAC

| ID | Requirement |
|---|---|
| **RMC-SPEC-IAM-001** | Staff authenticate with JWT access + refresh tokens; guests use reference + email only. |
| RMC-SPEC-IAM-001.1 | Roles are `FRONT_DESK`, `MANAGER`, `ADMIN`. |
| RMC-SPEC-IAM-001.2 | Protected staff APIs require valid JWT; nav-module ACL gates features; ADMIN may always pass module checks as designed. |
| RMC-SPEC-IAM-001.3 | Refunds require `ADMIN` or `MANAGER`. |
| RMC-SPEC-IAM-001.4 | MFA self-enroll is available; MFA is required for ADMIN (and ideally MANAGER) before production go-live. |
| RMC-SPEC-IAM-001.5 | Login lockout thresholds are configurable. |

### 12.2 Security

| ID | Requirement |
|---|---|
| **RMC-SPEC-SEC-001** | Guest booking create is rate-limited; webhook verification is enabled in production; no secrets in git/client bundle. |
| RMC-SPEC-SEC-001.1 | Unauthenticated calls to protected staff endpoints are denied. |
| RMC-SPEC-SEC-001.2 | PII-safe logging: no secrets, PAN/CVV, or unnecessary sensitive payment tokens in logs. |
| RMC-SPEC-SEC-001.3 | Staging/prod use TLS and sensible security headers at the edge. |

### 12.3 Observability

| ID | Requirement |
|---|---|
| **RMC-SPEC-OBS-001** | Operators can diagnose checkout, webhook, refund, and hold-release failures via structured logs and correlation. |
| RMC-SPEC-OBS-001.1 | Error tracking captures unhandled server failures without logging secrets. v1: log unhandled exceptions server-side with redacted messages; clients receive a generic 500 body. |
| RMC-SPEC-OBS-001.2 | Alertable signals exist for repeated webhook or hold failures (hardening phase may deepen metrics). v1: grepable `ALERT signal=webhook_failure` or `ALERT signal=hold_failure` logs with an incrementing count. |

---

## 13. Configurable settings (not immutable law)

| ID | Requirement |
|---|---|
| **RMC-SPEC-CFG-001** | Hotel-configurable settings exist for taxes/fees toggles and percents (service charge, VAT, municipal tax), **named refund policies**, rates, rooms/extras, automatic promos, **promo codes**, branding, mail on/off, hold TTL / buffers as exposed, and guest booking rate-limit threshold. |
| RMC-SPEC-CFG-001.1 | Changing a live refund policy or which policy a rate plan references must not rewrite historical snapshots on existing bookings. |
| RMC-SPEC-CFG-001.1a | Staff can create multiple named refund policies; deactivating a policy that is still referenced by active rate plans is rejected until plans are reassigned. |
| RMC-SPEC-CFG-001.1b | Create and update of a refund policy require a non-blank guest-facing **description** (how cancel/refund works). That copy is what guests see from the policy info control (`RMC-SPEC-GUEST-001.2c`). |
| RMC-SPEC-CFG-001.2 | Staff module tree labels/order/roles are configurable; module paths must remain valid product routes from §4. |

---

## 13A. Promo codes (access rates)

Distinct from automatic public promos (`promo` table). See RESEARCH **D19**.

| ID | Requirement |
|---|---|
| **RMC-SPEC-PROMO-001** | Guest-entered promo codes unlock negotiated/access rates separately from automatic public promos. |
| RMC-SPEC-PROMO-001.1 | Promo code **types**: `SPECIAL_RATE` (offer code only), `CORPORATE` (organization code + offer code), `AGENCY` (organization code + offer code). Corporate/agency cannot be saved or applied unless both codes are present. Guest apply must send the selected type; type mismatch fails as invalid. |
| RMC-SPEC-PROMO-001.2 | Staff CRUD at `/staff/settings/promo-codes`: name, type, codes, active window, active flag, discount **PERCENT** or **FIXED** PHP, **max uses**, and one or more **rate plans** in scope. |
| RMC-SPEC-PROMO-001.3 | Apply fails (no discount) when codes are missing/wrong, **promo type does not match**, inactive, outside window, exhausted (`used_count >= max_uses`), or the selected rate plan is out of scope. Availability surfaces an error and rack prices; **booking create soft-falls back** to rack (or automatic public promo) instead of rejecting the booking when a previously applied code is no longer usable. |
| RMC-SPEC-PROMO-001.4 | A valid applied code discounts eligible rate-plan stay totals on availability search, room detail, quote, and booking create (Appendix G). One promo code per booking. |
| RMC-SPEC-PROMO-001.5 | When a promo code is applied to a booking/quote, automatic public promos **do not stack** on that booking. |
| RMC-SPEC-PROMO-001.6 | `used_count` increments atomically on booking create when a promo code is attached; when `used_count` would exceed `max_uses`, create is rejected. Usage is released if the booking reaches `FAILED` or is cancelled while still unpaid/unconfirmed (`PENDING_PAYMENT` / `PENDING_APPROVAL`). |
| RMC-SPEC-PROMO-001.7 | Offer/organization codes are matched case-insensitively after trim; amounts remain PHP and server-computed only. |

---

## 14. HTTP API

| ID | Requirement |
|---|---|
| **RMC-SPEC-API-001** | Expose guest, staff, Maya webhook, and health APIs as catalogued in Appendix D. |
| RMC-SPEC-API-001.1 | Public: health, guest, Maya webhook, staff login/refresh/mfa-verify/logout, uploads, OpenAPI. |
| RMC-SPEC-API-001.2 | Other `/api/staff/**` require JWT plus fine-grained authorization. |

---

## 15. Testing and QA

| ID | Requirement |
|---|---|
| **RMC-SPEC-TEST-001** | Automated tests cover booking lifecycle, Maya payment/webhook security, refunds, rate limit, and critical pricing/hold behaviour. |
| RMC-SPEC-TEST-001.1 | Acceptance criteria in `docs/VALIDATION.md` map to SPEC IDs. |
| RMC-SPEC-TEST-001.2 | Browser e2e covers critical guest and staff flows. |
| RMC-SPEC-TEST-001.3 | Concurrency/load proofs cover holds and payment idempotency. |

---

## 16. CI/CD and environments

| ID | Requirement |
|---|---|
| **RMC-SPEC-CICD-001** | `.env.example` documents required variable **names**; real secrets come from env/secret store. |
| RMC-SPEC-CICD-001.1 | Schema is applied via Flyway migrations in deploy pipelines. |
| RMC-SPEC-CICD-001.2 | Staging exists with sandbox Maya and production-like verification posture for UAT. |
| RMC-SPEC-CICD-001.3 | Production uses live Maya keys, webhook verification enabled, separated from sandbox. |

Env catalog: **Appendix B**.

---

## 17. Build phases

| ID | Phase | Deliver |
|---|---|---|
| **RMC-SPEC-PHASE-001** | Phased delivery | Build in order below; gate each phase on matching `AC-*`. |
| RMC-SPEC-PHASE-001.1 | Foundation | Stack, migrations, health, config, branding seed |
| RMC-SPEC-PHASE-001.2 | Catalog & availability | Rooms, rates, extras, availability + holds |
| RMC-SPEC-PHASE-001.3 | Guest booking | Checkout, quotes, consent, pay-at-hotel path |
| RMC-SPEC-PHASE-001.4 | Maya online pay | Create checkout, webhook, confirm-poll, ledger |
| RMC-SPEC-PHASE-001.5 | Staff ops | Auth/RBAC, dashboard, arrivals, booking ops, guests, rooms |
| RMC-SPEC-PHASE-001.6 | Cancel & refunds | Policy snapshot, staff refunds, **auto-refund** |
| RMC-SPEC-PHASE-001.7 | Hardening | Observability, e2e, staging UAT, prod checklist |

---

## 18. Go-live checklist

| ID | Requirement |
|---|---|
| **RMC-SPEC-LIVE-001** | Before production traffic: sandbox/staging Maya webhook UAT passed; prod keys separated; webhook verification on; MFA policy for privileged roles enforced; backups/secret storage defined; no secrets in repo. |
| RMC-SPEC-LIVE-001.1 | Prove end-to-end online pay (webhook path) on staging. |
| RMC-SPEC-LIVE-001.2 | Prove cancel + auto-refund happy path and failure/staff-retry path on staging. |
| RMC-SPEC-LIVE-001.3 | Prove hold expiry and no double-sell under concurrent attempts. |

---

## 19. Out of scope (v1)

| ID | Requirement |
|---|---|
| **RMC-SPEC-BIZ-001** | The following are out of scope unless research is revised: multi-property inventory, guest accounts/loyalty, channel manager/OTA, embedded card fields, non-PHP currency. |
| RMC-SPEC-BIZ-001.1 | Legal hotel naming and final marketing copy are business inputs; branding admin can apply logo/copy when supplied. |

---

# Appendices (normative)

## Appendix A — Seed branding defaults

**Intended** hospitality seed (RESEARCH) for new environments — converge Flyway/seed + code when ready:

```yaml
primaryColor: #0B4F4A
secondaryColor: #F3EFE7
fontFamily: Plus Jakarta Sans
footerText: Book your stay with confidence.
```

**Current DB / entity / code defaults** (`branding_config` + `DEFAULT_BRANDING` — what the visual SoT mirrors):

```yaml
companyName: RMC Booking
staffPortalTitle: RMC Staff
staffPortalSubtitle: Booking portal
logoUrl: null          # optional until admin upload; BrandMark hidden when null
faviconUrl: null
fontFamily: Geist Variable
primaryColor: #1a1a1a
primaryForegroundColor: #fafafa
secondaryColor: #f4f4f5
secondaryForegroundColor: #1a1a1a
# Entity columns also default header/footer chrome to slate (#0f172a / #f8fafc);
# runtime applyBranding may drive guest chrome from secondary via resolveBrandingPalette.
footerText: null       # UI fallback: “Book your stay with confidence.”
footerContactEmail: null
footerContactPhone: null
footerCopyright: null
```

**Default media (not stored in branding_config):**

| Asset | Default |
|---|---|
| Guest home hero | Frontend public file `/images/hotel-hero.png` (`rmc_frontend/public/images/hotel-hero.png`) |
| Room type images | None in Flyway seed — staff catalog / uploads populate `room_type_image` |
| Staff profile image | null until upload |
---

## Appendix B — Environment variables (names + purpose)

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | MySQL |
| `SERVER_PORT` | API port (default 8082) |
| `APP_FRONTEND_BASE_URL` | Guest UI origin; Maya redirect URLs |
| `CORS_ALLOWED_ORIGINS` | CORS allowlist |
| `JWT_SIGNING_SECRET` | JWT HMAC secret (32+ chars) |
| `JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS` | Token TTLs |
| `JWT_LOCKOUT_THRESHOLD`, `JWT_LOCKOUT_MINUTES` | Login lockout |
| `MAYA_PUBLIC_KEY` | Maya checkout create (Basic auth username) |
| `MAYA_SECRET_KEY` | Maya get / refund / void |
| `MAYA_API_BASE_URL` | e.g. sandbox `https://pg-sandbox.paymaya.com` |
| `MAYA_WEBHOOK_IP_VERIFY` | Enable IP allowlist |
| `MAYA_WEBHOOK_ALLOWED_IPS` | Webhook source IPs |
| `MAYA_WEBHOOK_SHARED_SECRET` | Optional `X-Webhook-Token` |
| `MAYA_WEBHOOK_HMAC_SECRET` | Optional `X-Webhook-Signature` HMAC |
| `REDIS_ENABLED`, `REDIS_HOST`, `REDIS_PORT` | Shared rate-limit Redis |
| `GUEST_BOOKING_RATE_LIMIT` | Guest booking POSTs/IP/min (`0` = off) |
| `MAIL_*`, `MAIL_OUTBOX_INTERVAL_MS` | SMTP + outbox |
| `SCHEDULER_INTERVAL_MS` | Lifecycle scheduler interval |
| `APP_UPLOAD_DIR` | Upload root |

**Never commit real secrets.**

---

## Appendix C — Intentionally not invented here

- Multi-property / channel manager  
- Guest user accounts  
- Exact cloud vendor SKUs  
- Pixel prototype screenshot thresholds beyond `docs/prototype/rmc-booking.html` (agree a diff threshold in VALIDATION_RUN when first UI AC pass is recorded)  

---

## Appendix D — HTTP API catalog

**Auth model:**  
`permitAll`: `/api/health`, `/api/guest/**`, `/api/payments/maya/**`, staff login/refresh/mfa-verify/logout, `/uploads/**`, OpenAPI.  
Other `/api/staff/**`: JWT. Fine-grained: nav modules, arrivals/rooms composite access, role checks.

### D.1 Health
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/health` | public | DB (+ Redis if enabled) |

### D.2 Guest
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/guest/pricing-policy` | `PricingPolicyResponse`: `serviceChargeEnabled`, `vatEnabled`, `municipalTaxEnabled` |
| GET | `/api/guest/room-catalog` | Active room products; `fromNightlyRate` = min across active plans |
| GET | `/api/guest/availability` | Search; optional `offerCode` + `organizationCode`; per type: `fromTotalTaxInclusive` + `ratePlans[]` (promo-code discounts when valid) |
| GET | `/api/guest/availability/check` | Stay check for room type + optional `ratePlanId` + optional promo codes |
| POST | `/api/guest/bookings` | Create booking (+ Maya URL if online); may include promo code fields |
| GET | `/api/guest/bookings/{reference}` | Detail |
| GET | `/api/guest/bookings/{reference}/status` | Status |
| POST | `/api/guest/bookings/{reference}/confirm-payment` | Poll Maya |
| POST | `/api/guest/bookings/{reference}/charges/{chargeId}/pay` | Pay additional charge |
| POST | `/api/guest/bookings/{reference}/charges/{chargeId}/confirm-payment` | Confirm charge payment |
| POST | `/api/guest/bookings/{reference}/cancel` | Guest cancel |
| GET | `/api/guest/branding` | Public branding |
| GET | `/api/guest/extras/services` | Service add-ons |
| GET | `/api/guest/extras/items` | Item add-ons |

### D.3 Maya webhook
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/payments/maya/webhook` | Apply Maya payload |

### D.4 Staff auth
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/staff/auth/login` | public | Login |
| POST | `/api/staff/auth/mfa/verify` | public | MFA complete |
| POST | `/api/staff/auth/refresh` | public | Refresh |
| POST | `/api/staff/auth/logout` | public/optional JWT | Logout |
| POST | `/api/staff/auth/mfa/setup\|confirm\|disable` | JWT | MFA manage |

### D.5 Staff bookings / ops (JWT + nav/role gates)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/staff/arrivals`, `/api/staff/bookings`, `/api/staff/bookings/{id}` | Lists/detail |
| POST | `.../check-in`, `check-out`, `transfer-room` | Ops |
| POST | `.../approve`, `reject` | Pending approval |
| POST | `.../charges`, `.../charges/{id}/approve\|reject\|record-payment` | Additional charges |
| POST | `.../record-payment`, `.../override` | Folio / override |
| POST | `.../refund`, `.../manual-refund` | `ADMIN`\|`MANAGER` |

### D.6 Staff rooms / config (JWT + nav gates)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/staff/rooms/daily-status` | Daily unit status page |
| GET | `/api/staff/rooms/{roomUnitId}/calendar` | Unit calendar blocks for date range |
| GET | `/api/staff/rooms/{roomUnitId}/bookings` | Paged bookings ever assigned to unit (`RoomUnitBookingPageResponse`) |
| DELETE | `/api/staff/config/room-types/{id}` | Hard-delete or deactivate room type (`RoomTypeDeleteResult`) |
| DELETE | `/api/staff/config/room-numbers/{id}` | Hard-delete room unit per `RMC-SPEC-STAFF-001.4c` |
| POST | `/api/staff/config/room-types/{id}/rate-plans` | Create rate plan (`refundPolicyId` + seeds daily rates) |
| GET | `/api/staff/refund-policy` | List named refund policies |
| POST | `/api/staff/refund-policy` | Create refund policy |
| PUT | `/api/staff/refund-policy/{id}` | Update refund policy |
| DELETE | `/api/staff/refund-policy/{id}` | Deactivate refund policy |
| PUT | `/api/staff/config/rate-plans/{id}` | Update rate plan (policy, hold TTL, active, name, optional primary nightly rate) |
| PUT | `/api/staff/config/rate-plans/{id}/daily-rates` | Date-range amount override (marks nights as overrides) |
| DELETE | `/api/staff/config/rate-plans/{id}` | Deactivate or hard-delete unused rate plan |

**Response shapes (staff rooms / config):**

| DTO | Fields |
|---|---|
| `RoomTypeDeleteResult` | `deactivated` (bool), `message` (string) |
| `RoomUnitBookingPageResponse` | `roomUnitId`, `roomNumber`, `content[]`, `page`, `size`, `totalElements`, `totalPages` |
| `RoomUnitBookingDto` | `bookingId`, `reference`, `guestName`, `status`, `checkInDate`, `checkOutDate`, `paymentMethod` |

### D.7 Staff other prefixes
`/api/staff/guests`, `/dashboard`, `/search`, `/notifications`, `/profile`, `/rooms/**`, `/config/**`, `/promos`, `/promo-codes`, `/refund-policy`, `/branding`, `/users`, `/nav`, `/audit/**`.

---

## Appendix E — Booking status machine

**Statuses:** `PENDING_PAYMENT`, `PENDING_APPROVAL`, `CONFIRMED`, `CONFIRMED_PAY_LATER`, `CANCELLED`, `FAILED`, `NO_SHOW`

| From | To | Trigger |
|---|---|---|
| *(create ONLINE_MAYA)* | `PENDING_PAYMENT` | Booking create |
| *(create PAY_AT_HOTEL)* | `PENDING_APPROVAL` | Booking create |
| `PENDING_PAYMENT` | `PENDING_APPROVAL` | Maya success (webhook / confirm-poll) |
| `PENDING_PAYMENT` | `FAILED` | Maya fail / hold expiry |
| `PENDING_PAYMENT` | `CANCELLED` | Maya checkout cancelled; unpaid cancel |
| `PENDING_APPROVAL` | `CONFIRMED` | Staff approve + `ONLINE_MAYA` |
| `PENDING_APPROVAL` | `CONFIRMED_PAY_LATER` | Staff approve + `PAY_AT_HOTEL` |
| `PENDING_APPROVAL` | `CANCELLED` | Staff reject/cancel; scheduler past check-in |
| `CONFIRMED` / `CONFIRMED_PAY_LATER` | `CANCELLED` | Guest/staff cancel; pay-later cutoff |
| `CONFIRMED` | `NO_SHOW` | Scheduler mark no-shows |

**Staff override map:**  
`PENDING_APPROVAL` → `CANCELLED`;  
`CONFIRMED_PAY_LATER` → `NO_SHOW`\|`CANCELLED`;  
`CONFIRMED` → `CANCELLED`;  
`NO_SHOW` → back to `CONFIRMED` or `CONFIRMED_PAY_LATER` (by payment method).

Check-in/out update timestamps/room unit; they do **not** change `BookingStatus`.

---

## Appendix F — Maya integration contract

Official Maya docs remain authoritative for wire formats beyond this appendix.

### F.1 Environments
| | Sandbox | Production |
|---|---|---|
| API base | `https://pg-sandbox.paymaya.com` | `https://pg.paymaya.com` |
| Keys | Sandbox keys | Live keys (never mix) |

### F.2 Maya API calls
| Call | Method / path | Auth |
|---|---|---|
| Create checkout | `POST /checkout/v1/checkouts` | Public key Basic |
| Get checkout | `GET /checkout/v1/checkouts/{id}` | Secret key Basic |
| Refund | `POST /checkout/v1/checkouts/{id}/refunds` | Secret |
| Void | `DELETE /checkout/v1/checkouts/{id}` | Secret |

### F.3 Create body (fields sent)
`totalAmount{value,currency}`, `requestReferenceNumber`, `buyer{...}`, `items[{name,quantity,totalAmount}]`, `redirectUrl{success,failure,cancel}`.

### F.4 Redirect URLs
- Success: `{APP_FRONTEND_BASE_URL}/booking/success?reference={bookingRef}[&chargeId=]`  
- Failure: same + `&status=failed`  
- Cancel: same + `&status=cancelled`

### F.5 Webhook
- Endpoint: `POST /api/payments/maya/webhook`  
- Register in Maya Manager on a public HTTPS base  
- Minimum events: `PAYMENT_SUCCESS`, `PAYMENT_FAILED`; also expired/cancel/dropout as applicable  
- Optional verify: IP allowlist, `X-Webhook-Token`, `X-Webhook-Signature`  
- Production: verification enabled  
- Each accepted payload (and each confirm-poll GET body) is retained per `RMC-SPEC-ARCH-001.4` (redacted JSON + SHA-256 in `maya_payment_event`)

### F.6 Status interpretation
| Treat as success | `PAYMENT_SUCCESS` or status `COMPLETED` |
| Treat as failed | `PAYMENT_FAILED`, `PAYMENT_EXPIRED`, `PAYMENT_CANCELLED` |
| Treat as checkout cancelled | `CANCELLED`, `DROPOUT` |

### F.7 `requestReferenceNumber`
| Flow | Value |
|---|---|
| Booking payment | Booking `reference` |
| Additional charge | `AC-{chargeId}` |

### F.8 Poll vs webhook
Both call the same payment application logic. **Neither** may mark paid from redirect query params alone.

### F.9 Amount check
On success, received amount must match booking `quotedTotal` (mismatch → log error, do not confirm).

---

## Appendix G — Pricing algorithm

**Currency:** PHP only.

1. Load `daily_rate` per night.  
2. Per night:  
   - `serviceCharge = base × serviceChargePercent%` (if enabled)  
   - `vat = (base + serviceCharge) × vatPercent%` (if enabled)  
   - `municipalTax = (base + serviceCharge) × municipalTaxPercent%` (if enabled)  
   - `taxInclusive = base + serviceCharge + vat + municipalTax`  
3. Percents/toggles from system config (`serviceCharge*`, `vat*`, `municipalTax*`).  
4. `roomTotalBeforePromo = sum(taxInclusive)`  
5. If a valid **promo code** is applied and the rate plan is in scope → apply that code’s % or fixed discount → `roomTotal` (do not also apply an automatic public promo). Else apply best applicable automatic public promo → `roomTotal`.  
6. Add extras at catalog prices  
7. `quotedTotal = roomTotal + extrasTotal`  

**Guest quote payloads** expose per-night `NightlyRateDto` fields including `municipalTax` when enabled, and stay-level totals that sum the same breakdown for checkout display.

**Client-supplied totals are never trusted.**

---

## Appendix H — Inventory holds

Hold fields: booking, room type, hold date, held count, status (`ACTIVE`\|`RELEASED`\|`CONSUMED`), expires at.

| Rule | Behaviour |
|---|---|
| Create ONLINE_MAYA | ACTIVE holds per night; TTL from rate plan hold TTL |
| Create PAY_AT_HOTEL | Holds per pay-later rules |
| Availability | Counts ACTIVE holds against sellable inventory |
| Maya paid | Keep reservation semantics until approve/reject path completes as designed |
| Fail / cancel / expiry | Release ACTIVE → `RELEASED` |
| Scheduler | Expire holds; may briefly extend while Maya still pending |

---

## Appendix I — RBAC and staff modules

**Roles:** `FRONT_DESK`, `MANAGER`, `ADMIN`.

| Layer | Rule |
|---|---|
| HTTP | JWT required for protected `/api/staff/**` |
| Nav modules | Path allowlists by role; ADMIN always allowed to module checks as designed |
| Sidebar UX | Nested collapsible groups + leaf routes (`RMC-SPEC-UX-001.5a`); shadcn Sidebar chrome |
| Hard gates | Refunds: `ADMIN` or `MANAGER` |

Staff UI modules follow §4 routes and nav config in DB.

---

## Appendix J — Frontend route map

See §4. Dev proxy should forward `/api` and `/uploads` to the API origin.

---

## Appendix K — Domain tables

`system_config`, `room_type`, `room_unit`, `rate_plan`, `rate_plan_image`, `daily_rate`, `guest`, `booking`, `inventory_hold`, `booking_ledger`, `booking_audit_log`, `staff_user`, `refresh_token`, `configuration_audit_log`, `email_outbox`, `staff_nav_module`, `room_type_image`, `room_config_option`, `branding_config`, `room_service_addon`, `room_item_addon`, `booking_service_selection`, `booking_item_selection`, `staff_login_audit_log`, `staff_activity_audit_log`, `refund_policy`, `booking_refund_policy_snapshot`, `staff_notification`, `staff_notification_read`, `booking_additional_guest`, `promo`, `promo_room_type`, `promo_code`, `promo_code_rate_plan`, `booking_additional_charge`, `maya_payment_event`.

Flyway migrations are normative for column-level detail.

---

## Appendix L — Email and staff notifications

**Guest email** (when mail enabled) via outbox:

| Event | Outbox kind |
|---|---|
| Booking created (online pending payment or pay-at-hotel pending approval) | Booking received (includes **booking reference**) |
| Booking confirmed (staff approve) | Confirmation |
| Booking rejected | Rejection |
| Refund completed | Refund processed |

Booking-received is idempotent per booking (`BOOKING_RECEIVED`); Maya payment reaching `PENDING_APPROVAL` must not enqueue a second received email.

**Staff in-app notifications:** booking received, payment received, cancelled (and related ops signals) — separate from guest SMTP.

---

## Appendix M — Seed / demo profile

Fresh environments should seed at minimum:

- Branding defaults (Appendix A) — including `logoUrl: null` until upload  
- Default nav modules for staff roles (`DEFAULT_STAFF_MODULES` / DB)  
- Flyway `V2__seed_data` catalog (normative demo names/prices):  
  - **Standard Room** — queen bed copy; units 101–105; flexible rate; **₱2,500**/night PHP  
  - **Deluxe Room** — king / city view copy; units 201–203; flexible rate; **₱3,800**/night PHP  
- Default refund policy  
- One ADMIN staff user provisioned out-of-band (no real passwords in git)  
- Optional sample extras and promo  
- Guest hero falls back to frontend `/images/hotel-hero.png` (not a DB column)  
- Tax/fee `system_config` keys (Flyway): `serviceChargeEnabled`, `serviceChargePercent`, `vatEnabled`, `vatPercent`, `municipalTaxEnabled` (`false`), `municipalTaxPercent` (`1`) — see `V22__tax_fee_toggles.sql`, `V2__seed_data.sql`, `V37__municipal_tax.sql`

Visual SoT demo catalog must match these room names/nightly rates unless Appendix M is revised.
---

## Document history

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-07-23 | Greenfield rewrite from `docs/RESEARCH.md` v1.0; SPEC IDs retained; Delivery/DOC-GAP spine removed; auto-refund and design seeds included as normative product |
| 1.0.1 | 2026-07-23 | META-001.2 clarified: VALIDATION ACs + coverage + VALIDATION_RUN evidence are required to claim a SPEC requirement proven |
| 1.0.2 | 2026-07-23 | Visual SoT added: `docs/prototype/rmc-booking.html`; `RMC-SPEC-UX-001.10` points at prototype |
| 1.0.3 | 2026-07-23 | Prototype rewritten to mirror live `rmc_frontend` (full staff module tree + live DEFAULT_BRANDING); §4 staff labels aligned; Appendix A documents intended seed vs code fallback |
| 1.0.4 | 2026-07-23 | Stack: Vite/Tailwind/shadcn/Lucide (`RMC-SPEC-STACK-001.4`); staff collapsible sidebar groups (`RMC-SPEC-UX-001.5a`); prototype nav toggles |
| 1.0.5 | 2026-07-23 | Staff light/dark theme (`RMC-SPEC-UX-001.11`, `STAFF-001.9`); motion/hover baseline (`RMC-SPEC-UX-001.12`); ACs `AC-UI-002` / `AC-UI-003` |
| 1.0.6 | 2026-07-23 | SoT gap closure: brand mark/logo defaults, carousel focus-hover, guest/staff chrome (avatar menu, top bar, icons), seed catalog Appendix M, SoT fidelity scope (`UX-001.10`–`.16`) |
| 1.0.7 | 2026-07-23 | Remaining SoT fidelity: guests counters, room gallery/carousel controls, checkout/catalog steppers, guest motion, staff scroll-hide/charts/filters/ops (`UX-001.17`–`.18`) |
| 1.0.8 | 2026-07-23 | Guest home pixel pass vs `rmc_frontend`: hero/filters overlay, Explore ⅓ cards + dots-only, Lucide icons, thin scrollbars, Ramada SoT logo asset (`UX-001.13`/`.17`) |
| 1.0.9 | 2026-07-23 | Checkout + control sizing pass: live checkout panels/sidebar/pay options; booking CTAs `h-12`; guest cards shadow-only (no stroke border) |
| 1.0.10 | 2026-07-23 | SoT: one checkout step at a time; stepper end-spacers; staff refund-policy field set; Find rooms/Search CTA sizing |
| 1.0.11 | 2026-07-23 | Checkout layout padding + step CTAs (Next / Maya / pay-at-hotel); General settings cards; shared StaffModal (`UX-001.19`) |
| 1.0.12 | 2026-07-29 | Municipal tax (`CFG-001`, Appendix G/M); room-type delete (`STAFF-001.4a`); room-unit bookings list (`STAFF-001.4b`); room-number delete with historical unlink (`STAFF-001.4c`); Appendix D response shapes |
| 1.0.13 | 2026-07-30 | Catalog vs rate plans: room type product without owned price; multi-plan From price + guest plan picker (`GUEST-001.1`/`.2a`); cancel policy on rate plan with template Settings (`CXL-001.2a`, `CFG-001.1a`, `STAFF-001.4d`); RESEARCH D17/D18 |
| 1.0.14 | 2026-07-30 | Dedicated Rate plans staff route `/staff/rooms/rate-plans` with catalog selector on create and per-plan sample nightly rate (`STAFF-001.4e`); catalog no longer hosts rate-plan modal |
| 1.0.15 | 2026-07-30 | Rate plans tab on Create room; multi named refund policies + `refundPolicyId`; nights deduction stack (`CXL-001.1b`, `CFG-001.1a`); remove dedicated rate-plans route |
| 1.0.16 | 2026-07-31 | Rate plans own guest product fields; slim catalog = name+units; home From media from cheapest plan; detail lists plans with lowest highlighted (`GUEST-001.1`/`.2a`, `STAFF-001.4d`–`.4f`, RESEARCH D17). Flyway **V41**; local DB wipe recommended after migrate (see SPEC_GUIDE §5). |
| 1.0.17 | 2026-07-31 | Reverse ownership: room type owns guest product; rate plans = policy + price only; home room-type media + From price; detail plan picker name/price/policy (`GUEST-001.1`/`.2a`, `STAFF-001.4d`–`.4f`, RESEARCH D17). V41 columns left unused. |
| 1.0.18 | 2026-07-31 | Promo codes (access rates) distinct from automatic promos (`PROMO-001`, `GUEST-001.2b`, RESEARCH D19). Flyway **V42**. |
| 1.0.19 | 2026-08-04 | Rate plan **primary nightly rate** + optional date-range overrides (`STAFF-001.4d`, `AC-STAFF-012`). Flyway **V43** (`base_nightly_rate`, `daily_rate.is_override`). |
| 1.0.20 | 2026-08-04 | Maya same-day void preference: on `PY0045`, wait until next Manila day for API refund (`PY0047` same-day); after cutoff, fall back to refund (`CXL-001.3`). |
| 1.0.21 | 2026-08-05 | Room config **amenities** vocabulary + room-type dropdown add/remove (`STAFF-001.4` / `.4g`, `AC-STAFF-013`). |
| 1.0.22 | 2026-08-12 | Guest promo Apply shows success/failure alerts (`GUEST-001.2b`, `AC-GUEST-011`). |
| 1.0.23 | 2026-08-13 | Guest **booking-received** email on create with booking reference (`GUEST-001.7`, Appendix L, `AC-GUEST-006`). |
| 1.0.24 | 2026-08-13 | In-app confirmation dialogs, loading overlay, and toasts (`UX-001.20`, `AC-UI-005`). |
| 1.0.25 | 2026-08-13 | Required refund-policy description; guest Refundable / Free cancellation info control (`CFG-001.1b`, `GUEST-001.2c`, RESEARCH D20). |
| 1.0.26 | 2026-08-13 | Guest search filter is a compact horizontal bar at all breakpoints (`UX-001.14` / `.17`). |
| 1.0.27 | 2026-08-13 | Observability minimum: unhandled-error logs + PII-safe redaction, `X-Correlation-Id` / booking-reference MDC, alertable webhook/hold signals (`OBS-001`, `ARCH-001.3`, `SEC-001.2`). |
| 1.0.28 | 2026-08-14 | Guest cancel of paid `ONLINE_MAYA` auto-initiates Maya void/refund (`CXL-001.5`–`.9`); scheduler retries `PENDING` after Manila cutoff. |
| 1.0.29 | 2026-08-14 | Maya webhook/poll evidence store (`ARCH-001.4`, `maya_payment_event`, `AC-PAY-011`). |
