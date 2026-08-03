# RMC Spec structure guide

**Purpose:** Quick reference for future development — how `docs/SPEC.md` is organized, how IDs work, and where to look/change things.  
**Not a substitute for:** `docs/SPEC.md` (normative rules) or `docs/VALIDATION.md` (acceptance criteria).

---

## 1. Doc set at a glance

| Document | Role |
|---|---|
| [`RESEARCH.md`](./RESEARCH.md) | Product intent, scope, decisions |
| [`SPEC.md`](./SPEC.md) | Build contract — `RMC-SPEC-*` requirements + appendices |
| [`prototype/rmc-booking.html`](./prototype/rmc-booking.html) | Visual source of truth (guest + staff UI) |
| [`VALIDATION.md`](./VALIDATION.md) | `AC-*` acceptance criteria mapped to Spec IDs |
| [`AC_COVERAGE.md`](./AC_COVERAGE.md) | Which test/manual/doc gate owns each AC |
| [`VALIDATION_RUN.md`](./VALIDATION_RUN.md) | Dated PASS / FAIL / PARTIAL evidence only |
| **This guide** | Map of Spec structure for builders |

```text
RESEARCH  →  SPEC  →  build  →  VALIDATION (AC-*)  →  VALIDATION_RUN (evidence)
                ↑
         prototype (visual SoT)
```

**Rules of thumb**

1. Change product intent in RESEARCH first, then SPEC.  
2. Add/update Spec ID + AC before implementing.  
3. “Done” = relevant Spec satisfied **and** mapped AC(s) pass with evidence — Spec itself has no Delivery status column.  
4. Payment truth, holds, and RBAC are non-negotiable; tax % / branding hex are settings.

---

## 2. Spec ID format

| Form | Example | Meaning |
|---|---|---|
| Full ID (in docs/code comments) | `RMC-SPEC-GUEST-001.2` | Canonical ID |
| Short ID (Monday / chat) | `GUEST-001.2` | Same ID without `RMC-SPEC-` |
| Parent | `GUEST-001` | Whole area capability |
| Child | `GUEST-001.2` | One concrete rule under that parent |

**Pattern:** `RMC-SPEC-<AREA>-<NNN>` · sub-specs `RMC-SPEC-<AREA>-<NNN>.<n>`

| AREA | Topic |
|---|---|
| META | How to use Spec / governance |
| NN | Non-negotiables (money, holds, auth, hosted checkout) |
| STACK | Tech stack |
| UX | UI/UX, chrome, SoT fidelity |
| DATA | Persistence / ledger / currency |
| ARCH | Payment architecture (webhook vs poll) |
| GUEST | Guest booking behaviour |
| INV | Inventory holds |
| PAY | Payments + status control |
| CXL | Cancel & refunds |
| STAFF | Staff operations |
| IAM | Auth & roles |
| SEC | Security |
| OBS | Observability |
| PROMO | Promo codes (access rates; distinct from automatic promos) |
| CFG | Configurable hotel settings |
| API | HTTP API surface |
| TEST | Testing expectations |
| CICD | CI/CD & environments |
| PHASE | Build phases |
| LIVE | Go-live checklist |
| BIZ | Out of scope / business inputs |

---

## 3. SPEC.md section map

Read **only** the section + matching appendix for the task you are doing.

| § | Title | Owning IDs (prefix) | Go here when… |
|---|---|---|---|
| 0 | How to use this document | META-001 | Changing how Spec/VALIDATION relate |
| 1 | Non-negotiables | NN-001 | Touching money, holds, RBAC, Maya truth, secrets |
| 2 | Recommended tech stack | STACK-001 | Adding frameworks, Redis, observability baseline |
| 3 | UI / UX — design system | UX-001 | Guest/staff look, branding, SoT fidelity, theme/motion |
| 4 | IA and routes | (UX-001.1 / .2 + §4 tables) | Adding/renaming pages or nav |
| 5 | Data model (sketch) | DATA-001 | Entities, ledger, PHP money fields |
| 6 | Architecture and payment flow | ARCH-001 | Webhook vs poll, payment apply path |
| 7 | Guest booking behaviour | GUEST-001 | Search, quote, checkout, lookup, emails |
| 8 | Inventory and holds | INV-001 | Holds, expiry, concurrency |
| 9 | Payments and booking status | PAY-001, PAY-002 | Maya create, webhook, pay-at-hotel, status machine |
| 10 | Cancellation and refunds | CXL-001 | Cancel, staff refund, **auto-refund** |
| 11 | Staff operations | STAFF-001 | Dashboard, arrivals, booking ops, settings, MFA UI |
| 12 | Auth, security, observability | IAM-001, SEC-001, OBS-001 | JWT, roles, rate limit, logs, alerts |
| 13 | Configurable settings | CFG-001 | Taxes, rates, refund policy, modules — not immutable law |
| 13A | Promo codes | PROMO-001 | Access-rate codes vs automatic promos |
| 14 | HTTP API | API-001 | Public vs JWT staff surfaces |
| 15 | Testing and QA | TEST-001 | What must be covered by tests/e2e |
| 16 | CI/CD and environments | CICD-001 | Env names, Flyway, staging/prod Maya |
| 17 | Build phases | PHASE-001 | Delivery order / phase gates |
| 18 | Go-live checklist | LIVE-001 | Staging UAT and prod readiness |
| 19 | Out of scope (v1) | BIZ-001 | What we intentionally do **not** build |

---

## 4. Appendices (normative detail)

Behaviour IDs in the body often defer wire/schema detail to appendices.

| Appendix | Contents | Typical Spec owners |
|---|---|---|
| **A** | Seed branding defaults (intended hospitality vs live code defaults) | UX-001.8, CFG branding |
| **B** | Environment variable **names** + purpose (no secrets) | META-001.4, CICD-001, SEC |
| **C** | Intentionally not invented here | BIZ / scope hygiene |
| **D** | HTTP API catalog (paths, auth) | API-001 |
| **E** | Booking status machine | PAY-002, GUEST success copy |
| **F** | Maya integration contract (URLs, webhook, amount check) | PAY-001, ARCH-001, NN-001.1 |
| **G** | Pricing algorithm (server-side PHP quote) | GUEST-001.2, NN-001.2 |
| **H** | Inventory holds | INV-001, NN-001.3 |
| **I** | RBAC and staff modules | IAM-001, STAFF-001, NN-001.5 |
| **J** | Frontend route map | UX-001.1 / .2, §4 |
| **K** | Domain tables | DATA-001 |
| **L** | Email and staff notifications | GUEST-001.7 |
| **M** | Seed / demo profile (rooms, rates, demo staff) | UX-001.16 |

---

## 5. “Where do I change X?” cheat sheet

| You are changing… | Read first | Also update |
|---|---|---|
| Guest UI look / checkout steps | SPEC §3 (UX-001), prototype | `AC-UI-*`, SoT HTML |
| Staff UI chrome / theme / modal | UX-001.11–.19, StaffModal | `AC-UI-002`…`004`, prototype |
| New guest or staff route | §4 + Appendix J | RESEARCH if IA changes; frontend router |
| Quote / taxes / total | Appendix G + NN-001.2 | GUEST / CFG as needed; tests |
| Maya pay / webhook / “paid” rules | §6 ARCH + §9 PAY + Appendix F | NN-001.1; `AC-PAY-*` |
| Room availability / double-sell | §8 INV + Appendix H | `AC-INV-*` |
| Cancel / refund / auto-refund | §10 CXL-001 | `AC-CXL-*` (auto-refund still a product gap) |
| Staff booking ops | §11 STAFF + Appendix I | `AC-STAFF-*` |
| Roles / MFA / JWT | §12.1 IAM | SEC / LIVE as needed |
| New API endpoint | Appendix D + §14 API | OpenAPI; RBAC gates |
| New DB table/column | Appendix K + DATA-001 | Flyway migration only |
| Local schema reset after major migrations (e.g. V41 rate-plan product) | — | **Local only:** `DROP`/`CREATE` database `rmc_booking_system`, restart backend so Flyway + seed re-apply. Do not wipe hosted DBs. |
| Env / secrets / staging | Appendix B + §16 CICD | Never commit real secrets |
| Go-live readiness | §18 LIVE-001 | VALIDATION_RUN evidence |

---

## 6. Development workflow (spec-first)

```text
1. Intent change?     → edit RESEARCH.md
2. Behaviour change?  → add/update RMC-SPEC-* in SPEC.md (+ appendix if wire detail)
3. Proof needed?      → add/update AC-* in VALIDATION.md + AC_COVERAGE.md
4. UI change?         → update docs/prototype/rmc-booking.html to match
5. Implement          → one Spec slice; read only that section + appendix
6. Prove              → tests / MANUAL; record in VALIDATION_RUN.md
```

**Definition of done for a Spec slice**

- [ ] Owning `RMC-SPEC-*` text matches what you shipped  
- [ ] Mapped `AC-*` pass (or MANUAL PASS with evidence)  
- [ ] Coverage registry updated if you added tests/ACs  
- [ ] No payment / hold / RBAC / secret control weakened  
- [ ] Visual SoT updated if guest/staff chrome or critical UX changed  

---

## 7. Visual SoT (UX)

- Path: `docs/prototype/rmc-booking.html`  
- Owning Spec: `UX-001.10` (scope), `.17` guest fidelity, `.18`–`.19` staff patterns / StaffModal  
- Live app: `rmc_frontend` should stay aligned; SoT mirrors current product defaults (`#1a1a1a` / `#f4f4f5`) until Appendix A hospitality seed is applied in Flyway  

**Do not** invent major UX/IA only in code — update SPEC §3–§4 + prototype first.

---

## 8. Monday / delivery boards (optional tracking)

SPEC has **no** Delivery column. Track delivery outside Spec (e.g. Monday) using short IDs:

| Board | Meaning |
|---|---|
| **Done** | Built in product |
| **In Review (QA)** | Built/documented; needs QA or AC evidence |
| **In Progress** | Actively being built/proven |
| **Backlog** | Not started (e.g. auto-refund `CXL-001.5`–`.8`) |

Suggested Monday columns: **Item** (short Spec ID) · **Priority** · **Timeline** · **Notes** (what the Spec does).

---

## 9. Non-negotiables (never weaken)

1. Online **paid** = verified Maya webhook (preferred) or confirm-payment poll — **never** browser redirect alone.  
2. **PHP** only; **server-side** totals (Appendix G).  
3. Holds prevent **double-sell** and expire correctly (Appendix H).  
4. Staff mutations are **RBAC**-gated (Appendix I).  
5. **Hosted checkout** only — no PAN/CVV on RMC origin.  
6. Sandbox ≠ production Maya credentials (Appendix F).  
7. No secrets in git or client bundle (Appendix B).  
8. Guest cancel of paid `ONLINE_MAYA` with eligible amount → **auto Maya refund** (`CXL-001.5`–`.7`); UI must not claim refund complete until server says so.  
9. Single-property v1; no guest accounts (reference + email lookup).

---

## 10. Quick parent Spec index

| Parent ID | One-line purpose |
|---|---|
| META-001 | Spec is the build contract; use VALIDATION to prove it |
| NN-001 | Money / holds / auth / hosted checkout guards |
| STACK-001 | Normative tech stack |
| UX-001 | Dual-surface UI + visual SoT + branding |
| DATA-001 | Persist domain + ledger + PHP |
| ARCH-001 | Webhook-preferred payment architecture |
| GUEST-001 | Guest search → book → lookup → cancel |
| INV-001 | Inventory holds |
| PAY-001 / PAY-002 | Maya + pay-at-hotel; status machine |
| CXL-001 | Cancel, staff refund, auto-refund |
| STAFF-001 | Staff operations portal |
| IAM-001 | JWT + roles + MFA |
| SEC-001 | Rate limit, webhook verify, safe logging |
| OBS-001 | Logs / correlation / alerts |
| CFG-001 | Hotel-configurable settings |
| API-001 | HTTP API surface |
| TEST-001 | Automated + e2e + concurrency expectations |
| CICD-001 | Env, Flyway, staging/prod |
| PHASE-001 | Phased delivery order |
| LIVE-001 | Go-live proofs |
| BIZ-001 | Out of scope for v1 |

---

## 11. Related reading order for a new engineer

1. This guide  
2. `docs/README.md`  
3. `docs/RESEARCH.md` (product baseline)  
4. `docs/SPEC.md` §1 Non-negotiables + §6–§10 (pay / holds / cancel)  
5. Open `docs/prototype/rmc-booking.html` in a browser  
6. `docs/VALIDATION.md` Part 3 for the AC of your task  

---

*Companion to SPEC v1.0+ · Keep this guide structural; put normative behaviour only in SPEC.md.*
