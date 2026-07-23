# RMC Booking System — Production Build Specification

**Version:** 1.0  
**ID scheme:** `RMC-SPEC-<AREA>-<NNN>` · sub-specs `RMC-SPEC-<AREA>-<NNN>.<n>`  
**Research baseline:** `docs/RESEARCH.md` v1.0.3  
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
| RMC-SPEC-UX-001.14 | **Guest chrome:** sticky header with scroll-aware hide/show; Book / Find booking / Staff nav with active underline indicator; footer with brand, footer text, quick links, contact. Home search is a white **filter bar overlaid on the hero** (Hotel / Check-in / Check-out / Guests / Search). Hero CTA label **Find rooms**. Default hero image path: `/images/hotel-hero.png` until replaced by product photography. |
| RMC-SPEC-UX-001.15 | **Staff chrome:** sidebar header brand slot (`RMC-SPEC-UX-001.13`); “Navigation” label; Lucide (or equivalent) icons on modules; collapsible groups (`RMC-SPEC-UX-001.5a`); footer **account control** = avatar (image or initials) + name + role + chevron opening a menu: Profile, Light/Dark mode, Log out; top inset bar with sidebar trigger, **global search**, and **notifications**. |
| RMC-SPEC-UX-001.16 | **Demo catalog defaults** for SoT / fresh seed alignment: hotel display “RMC Hotel” (Manila copy per frontend `HOTELS`); Flyway seed room types **Standard Room** (₱2,500/night) and **Deluxe Room** (₱3,800/night) per Appendix M — SoT must not invent conflicting catalog names/prices. |
| RMC-SPEC-UX-001.17 | **Guest interaction fidelity (SoT):** search guests control uses +/- counters (rooms/adults/kids) in a filter-cell bar overlaid on the hero (live `BookingFilters` shape); room cards match stack catalog layout (gallery, MapPin meta, refundable badges, footer price + View details); room carousel uses **lg ⅓ card width** (live `basis-1/3`) with **dots only** (no prev/next on home); “Explore our Rooms” is centered display typography; guest page enter + restrained scroll-reveal; hero uses MapPin location + “Find rooms” CTA + staggered slide-in. **Checkout** is one route with five in-page steps (Room → Extras → Guest → Payment → Confirm); only the active step pane is visible; content sits in guest layout width (~`max-w-6xl` + responsive padding); panel stack + sticky price sidebar with hero image; footer shows **Back** + **Next** until the last step, then **Continue to Maya payment** or **Request pay-at-hotel booking** (never a generic “Place booking” on every step); primary booking CTAs use **h-12 / text-base** sizing; guest content cards use **shadow without stroke border**. |
| RMC-SPEC-UX-001.18 | **Staff interaction fidelity (SoT chrome + representative bodies):** staff top bar scroll-hides with main pane scroll; page body uses short enter transition; dashboard shows multi-chart occupancy/movement/revenue stubs + date filter bar; list pages use denser filter bar; room catalog includes multi-step wizard UI; booking detail shows fuller ops action strip + audit table; **refund policy** staff screen exposes the live field set (name, enabled, full cutoff + unit, partial %, check-in time, timezone, description, manual-refund toggle) as a representative form (not legal copy); **General settings** exposes representative cards for taxes & fees, system configuration, rate plans (+ daily rate batch), room types, room units, and account MFA enroll/disable. |
| RMC-SPEC-UX-001.19 | **Shared staff modal (`StaffModal`):** create/edit flows that use a dialog (e.g. promos) open a shared modal with fixed header (title + optional description), scrollable body, and footer actions (Cancel / primary Save). SoT demos the pattern on Promos; live implementation is `StaffModal` / `StaffModalContent` over the Dialog primitive. Size variants (sm–xl / wizard) may be used; modal must trap focus and dismiss via Cancel, explicit close, or backdrop per product Dialog behaviour. |

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
| `/staff/rooms/catalog` | Create room | Units + room-type catalog |
| `/staff/rooms/extras` | Extras | Services / items |
| `/staff/rooms/operations` | View and update room | Daily ops / availability |
| `/staff/settings` | General | Taxes, system config, rates |
| `/staff/settings/refund-policy` | Refund policy | Cancellation / refund rules |
| `/staff/settings/promos` | Promos & discounts | Promo CRUD |
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
- **Room type / unit / rates / daily rates** — sellable inventory and pricing inputs  
- **Promo** — percent or fixed PHP, window, room-type scope  
- **Extras** — service/item add-ons + booking selections  
- **Additional charge** — post-booking charge with its own payment lifecycle  
- **Refund policy** + **booking refund policy snapshot**  
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
| RMC-SPEC-ARCH-001.3 | Correlate checkout → payment → webhook/poll → booking/audit in logs. |
| RMC-SPEC-ARCH-001.4 | Retain enough webhook/payment evidence to debug and replay safely (raw or hashed payload references as implemented). |

Full Maya wire rules: **Appendix F**.

---

## 7. Guest booking behaviour

| ID | Requirement |
|---|---|
| **RMC-SPEC-GUEST-001** | Guest can search, view rooms, quote, checkout, look up booking, cancel, and pay additional charges. |
| RMC-SPEC-GUEST-001.1 | Availability search returns only sellable room types for the requested stay (respecting holds and config). |
| RMC-SPEC-GUEST-001.2 | Quotes are computed server-side (Appendix G). |
| RMC-SPEC-GUEST-001.3 | Checkout captures guest identity/contact, optional additional guests within capacity, extras, special request, and consent. |
| RMC-SPEC-GUEST-001.4 | Payment choice is `ONLINE_MAYA` or `PAY_AT_HOTEL`. |
| RMC-SPEC-GUEST-001.5 | Lookup by booking reference + email returns booking detail without a guest account. |
| RMC-SPEC-GUEST-001.6 | Success/confirmation pages reflect server truth (status / confirm-payment), not redirect cosmetics alone. |
| RMC-SPEC-GUEST-001.7 | Email outbox items are created for configured lifecycle events (Appendix L). |
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

Refund policy is hotel-configurable (cutoffs, partial %, check-in time, timezone, manual-refund toggle). At the appropriate lifecycle point, the system **snapshots** policy onto the booking. Cancel evaluation uses the snapshot.

| ID | Requirement |
|---|---|
| **RMC-SPEC-CXL-001** | Guest and staff cancel paths evaluate the booking refund policy snapshot (FULL / PARTIAL / NONE). |
| RMC-SPEC-CXL-001.1 | FULL / PARTIAL / NONE (and percent) outcomes follow the snapshot policy. |
| RMC-SPEC-CXL-001.2 | Cancellation/refund evaluation uses the snapshot, not only live mutable config. |
| RMC-SPEC-CXL-001.3 | Staff Maya refund and (when enabled) manual refund actions are audited and update booking + ledger state. |
| RMC-SPEC-CXL-001.4 | Manual refund methods include at least GCash, bank transfer, cash, other (when manual refunds enabled). |
| RMC-SPEC-CXL-001.5 | **Auto-refund on guest cancel:** Given a paid `ONLINE_MAYA` booking and guest cancel allowed by policy with refund-eligible amount > 0, when cancel succeeds, the server initiates Maya refund for that amount without requiring staff approval, records ledger/refund status, and audits the attempt. |
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
| RMC-SPEC-STAFF-001.2 | Booking detail supports approve/reject, check-in (assign unit), transfer, check-out, folio payment recording, additional charges, status override (allowed map), and refunds per role. |
| RMC-SPEC-STAFF-001.3 | Guest directory and profile show identity and stay history. |
| RMC-SPEC-STAFF-001.4 | Rooms modules cover config options, catalog/units, extras, and daily operations/calendar. |
| RMC-SPEC-STAFF-001.5 | Settings cover system/tax keys, rates, refund policy, promos, and audit. |
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
| RMC-SPEC-OBS-001.1 | Error tracking captures unhandled server failures without logging secrets. |
| RMC-SPEC-OBS-001.2 | Alertable signals exist for repeated webhook or hold failures (hardening phase may deepen metrics). |

---

## 13. Configurable settings (not immutable law)

| ID | Requirement |
|---|---|
| **RMC-SPEC-CFG-001** | Hotel-configurable settings exist for taxes/fees toggles and percents, refund policy, rates, rooms/extras, promos, branding, mail on/off, hold TTL / buffers as exposed, and guest booking rate-limit threshold. |
| RMC-SPEC-CFG-001.1 | Changing live refund policy must not rewrite historical snapshots on existing bookings. |
| RMC-SPEC-CFG-001.2 | Staff module tree labels/order/roles are configurable; module paths must remain valid product routes from §4. |

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
| GET | `/api/guest/pricing-policy` | VAT/SC toggles |
| GET | `/api/guest/room-catalog` | Active rooms |
| GET | `/api/guest/availability` | Search |
| GET | `/api/guest/availability/check` | Stay check |
| POST | `/api/guest/bookings` | Create booking (+ Maya URL if online) |
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

### D.6 Staff other prefixes
`/api/staff/guests`, `/dashboard`, `/search`, `/notifications`, `/profile`, `/rooms/**`, `/config/**`, `/promos`, `/refund-policy`, `/branding`, `/users`, `/nav`, `/audit/**`.

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
   - `taxInclusive = base + serviceCharge + vat`  
3. Percents/toggles from system config.  
4. `roomTotalBeforePromo = sum(taxInclusive)`  
5. Apply best applicable promo → `roomTotal`  
6. Add extras at catalog prices  
7. `quotedTotal = roomTotal + extrasTotal`  

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

`system_config`, `room_type`, `room_unit`, `rate_plan`, `daily_rate`, `guest`, `booking`, `inventory_hold`, `booking_ledger`, `booking_audit_log`, `staff_user`, `refresh_token`, `configuration_audit_log`, `email_outbox`, `staff_nav_module`, `room_type_image`, `room_config_option`, `branding_config`, `room_service_addon`, `room_item_addon`, `booking_service_selection`, `booking_item_selection`, `staff_login_audit_log`, `staff_activity_audit_log`, `refund_policy`, `booking_refund_policy_snapshot`, `staff_notification`, `staff_notification_read`, `booking_additional_guest`, `promo`, `promo_room_type`, `booking_additional_charge`.

Flyway migrations are normative for column-level detail.

---

## Appendix L — Email and staff notifications

**Guest email** (when mail enabled) via outbox:

| Event | Outbox kind |
|---|---|
| Booking confirmed | Confirmation |
| Booking rejected | Rejection |
| Refund completed | Refund processed |

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
