# RMC AC Coverage Matrix

**Companion to:** `docs/VALIDATION.md` Part 3 (v1.6) · `docs/SPEC.md` v1.0.11 · `docs/RESEARCH.md` v1.0.3  
**Purpose:** Map every `AC-*` to owning proof (automated test, CI check, doc review, or manual gate).  
**Baseline:** SPEC v1.0; VALIDATION is the pass/fail gate for SPEC; visual SoT = `docs/prototype/rmc-booking.html`

## Status legend

| Status | Meaning |
|---|---|
| **COVERED** | Existing automated artifact or complete doc appendix proof |
| **PARTIAL** | Some proof exists; gaps remain |
| **GAP** | No automated owner yet — planned path documented |
| **MANUAL** | Staging / ops / human gate only |

---

## Matrix

| AC | Spec ID(s) | Status | Owner(s) |
|---|---|---|---|
| AC-GUEST-001 | `RMC-SPEC-GUEST-001.1` | PARTIAL | Availability mapper From-plan media + guest UI; e2e planned |
| AC-GUEST-002 | `RMC-SPEC-GUEST-001.2`, `RMC-SPEC-NN-001.2`, `RMC-SPEC-DATA-001.3` | PARTIAL | `rmc_backend/.../BookingLifecycleServiceTest.java` + planned e2e quote assertion |
| AC-GUEST-003 | `RMC-SPEC-GUEST-001.3`, `RMC-SPEC-UX-001.6`, `RMC-SPEC-NN-001.6` | PARTIAL | `rmc_backend/.../BookingLifecycleServiceTest.java` |
| AC-GUEST-004 | `RMC-SPEC-GUEST-001.5`, `RMC-SPEC-IAM-001`, `RMC-SPEC-NN-001.10` | GAP | `rmc_frontend/tests/e2e/guest-lookup.spec.ts` *(planned)* |
| AC-GUEST-005 | `RMC-SPEC-GUEST-001.6`, `RMC-SPEC-UX-001.3`, `RMC-SPEC-ARCH-001`, `RMC-SPEC-PAY-002.1` | GAP | `rmc_backend/.../integration/WebhookTruthIntegrationTest.java` *(planned)* |
| AC-GUEST-006 | `RMC-SPEC-GUEST-001.7` | GAP | `rmc_backend/.../EmailOutboxServiceTest.java` *(planned)* |
| AC-GUEST-007 | `RMC-SPEC-GUEST-001.4`, `RMC-SPEC-PAY-001` | GAP | `rmc_frontend/tests/e2e/guest-checkout-payment-method.spec.ts` *(planned)* |
| AC-GUEST-008 | `RMC-SPEC-GUEST-001.8`, `RMC-SPEC-UX-001.7`, `RMC-SPEC-STAFF-001.6` | GAP | `rmc_frontend/tests/e2e/guest-branding.spec.ts` *(planned)* |
| AC-GUEST-009 | `RMC-SPEC-GUEST-001.2a` | COVERED | `rmc_backend/.../AvailabilityServiceMultiPlanTest.java` + room detail picker |
| AC-GUEST-010 | `RMC-SPEC-GUEST-001.1`, `.2a` | COVERED | `RoomAvailabilityMapperFromPlanTest` — room-type media + From price |
| AC-GUEST-011 | `RMC-SPEC-GUEST-001.2b`, `PROMO-001.1`, `.3`, `.4` | COVERED | `PromoCodeServiceTest` + home filter apply |
| AC-INV-001 | `RMC-SPEC-INV-001.1` | PARTIAL | `rmc_backend/.../BookingLifecycleServiceTest.java` |
| AC-INV-002 | `RMC-SPEC-INV-001.2` | PARTIAL | `rmc_backend/.../BookingLifecycleServiceTest.java` |
| AC-INV-003 | `RMC-SPEC-INV-001.3` | GAP | `rmc_backend/.../AvailabilityServiceTest.java` *(planned)* |
| AC-INV-004 | `RMC-SPEC-NN-001.3`, `RMC-SPEC-INV-001.4`, `RMC-SPEC-LIVE-001.3` | GAP | `rmc_backend/.../InventoryHoldConcurrencyTest.java` *(planned)* + load suite |
| AC-PAY-001 | `RMC-SPEC-PAY-001.1` | COVERED | `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/service/MayaPaymentServiceTest.java` |
| AC-PAY-002 | `RMC-SPEC-NN-001.1`, `RMC-SPEC-ARCH-001`, `RMC-SPEC-PAY-002.1` | GAP | `rmc_backend/.../integration/WebhookTruthIntegrationTest.java` *(planned)* |
| AC-PAY-003 | `RMC-SPEC-PAY-001.2` | COVERED | `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/service/MayaWebhookSecurityServiceTest.java` |
| AC-PAY-004 | `RMC-SPEC-PAY-001.3`, `RMC-SPEC-ARCH-001.2` | GAP | `rmc_backend/.../MayaWebhookIdempotencyTest.java` *(planned)* |
| AC-PAY-005 | `RMC-SPEC-PAY-002` | PARTIAL | `rmc_backend/.../BookingLifecycleServiceTest.java` |
| AC-PAY-006 | `RMC-SPEC-PAY-001.4`, `RMC-SPEC-PAY-001.5` | PARTIAL | `rmc_backend/.../BookingLifecycleServiceTest.java` |
| AC-PAY-007 | `RMC-SPEC-NN-001.4`, `RMC-SPEC-DATA-001.1` | PARTIAL | `rmc_backend/.../MayaPaymentServiceTest.java`, refund service tests |
| AC-PAY-008 | `RMC-SPEC-PAY-001.6` | GAP | `rmc_backend/.../BookingAdditionalChargeServiceTest.java` *(planned)* |
| AC-PAY-009 | `RMC-SPEC-NN-001.8`, `RMC-SPEC-PAY-001.7`, `RMC-SPEC-CICD-001.3` | MANUAL | Staging/prod env checklist (`docs/VALIDATION_RUN.md`) |
| AC-PAY-010 | `RMC-SPEC-PAY-002.2`, Appendix F.9 | GAP | `rmc_backend/.../MayaAmountMismatchTest.java` *(planned)* |
| AC-CXL-001 | `RMC-SPEC-CXL-001.2`, `RMC-SPEC-DATA-001.2` | COVERED | `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/service/RefundPolicyServiceTest.java` |
| AC-CXL-002 | `RMC-SPEC-CXL-001.1` | COVERED | `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/service/RefundPolicyServiceTest.java` |
| AC-CXL-003 | `RMC-SPEC-CXL-001.3`, `RMC-SPEC-CXL-001.4` | COVERED | `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/service/StaffRefundServiceTest.java`, `MayaRefundServiceTest.java` |
| AC-CXL-004 | `RMC-SPEC-CXL-001.5`, `RMC-SPEC-CXL-001.6` | GAP | `GuestCancelAutoRefundTest.java` *(planned — auto-refund not built)* |
| AC-CXL-005 | `RMC-SPEC-CXL-001.7` | GAP | `GuestCancelAutoRefundFailureTest.java` *(planned)* |
| AC-CXL-006 | `RMC-SPEC-CXL-001.9`, `RMC-SPEC-UX-001.4` | GAP | `rmc_frontend/tests/e2e/guest-cancel-refund-messaging.spec.ts` *(planned)* |
| AC-CXL-007 | `RMC-SPEC-CXL-001.8` | GAP | `GuestCancelNoAutoRefundPayAtHotelTest.java` *(planned)* |
| AC-CXL-008 | `RMC-SPEC-CXL-001.2a` | COVERED | `rmc_backend/.../BookingRefundPolicySnapshotServiceTest.java` |
| AC-CXL-009 | `RMC-SPEC-CXL-001.1`, `.1b` | COVERED | `rmc_backend/.../RefundPolicyServiceTest.java` |
| AC-STAFF-001 | `RMC-SPEC-IAM-001.1`–`.3`, `RMC-SPEC-NN-001.5` | GAP | `rmc_backend/.../StaffRbacDenialTest.java` *(planned)* |
| AC-STAFF-002 | `RMC-SPEC-STAFF-001.2`, `.4` | GAP | `rmc_backend/.../StaffBookingOpsTest.java` *(planned)* |
| AC-STAFF-003 | `RMC-SPEC-UX-001.5`, `.5a`, `RMC-SPEC-IAM-001.2`, `RMC-SPEC-CFG-001.2` | GAP | `rmc_frontend/tests/e2e/staff-modules.spec.ts` *(planned)* — RBAC hide + collapsible groups |
| AC-STAFF-004 | `RMC-SPEC-STAFF-001.7`, `.8` | GAP | `rmc_backend/.../StaffAdminAuditTest.java` *(planned)* |
| AC-STAFF-005 | `RMC-SPEC-STAFF-001.4a` | COVERED | `rmc_backend/.../StaffConfigServiceRoomTypeDeleteTest.java` |
| AC-STAFF-006 | `RMC-SPEC-STAFF-001.4b` | COVERED | `rmc_backend/.../StaffRoomOccupancyServiceBookingsTest.java` + ops UI |
| AC-STAFF-007 | `RMC-SPEC-STAFF-001.4c` | COVERED | `rmc_backend/.../StaffConfigServiceRoomUnitDeleteTest.java` |
| AC-STAFF-008 | `RMC-SPEC-STAFF-001.4d`, `.4`, `.4f` | COVERED | `rmc_backend/.../StaffConfigServiceRatePlanCreateTest.java` |
| AC-STAFF-009 | `RMC-SPEC-STAFF-001.4e`, `.4d` | COVERED | `StaffConfigServiceRatePlanCreateTest` + slim rate-plan wizard |
| AC-STAFF-010 | `RMC-SPEC-STAFF-001.4f`, `.4` | COVERED | Full catalog wizard (`RoomTypeFormWizard` + `StaffRoomsCatalogPage`) |
| AC-SEC-001 | `RMC-SPEC-SEC-001.1`, `RMC-SPEC-IAM-001` | GAP | `rmc_backend/.../StaffAuthSecurityTest.java` *(planned)* |
| AC-SEC-002 | `RMC-SPEC-SEC-001` | COVERED | `rmc_backend/src/test/java/RMC_Booking_Engine/rmc/security/InMemoryGuestBookingRateLimiterTest.java` |
| AC-SEC-003 | `RMC-SPEC-META-001.4`, `RMC-SPEC-CICD-001` | PARTIAL | CI secret/dep scan *(to formalize)*; `.env.example` only in git |
| AC-SEC-004 | `RMC-SPEC-SEC-001.2` | GAP | Logging redaction tests *(planned)* |
| AC-SEC-005 | `RMC-SPEC-PAY-001.2`, `RMC-SPEC-CICD-001.3` | PARTIAL | `MayaWebhookSecurityServiceTest.java` + MANUAL staging verify-ON |
| AC-SEC-006 | `RMC-SPEC-SEC-001.3` | MANUAL | Edge/TLS/headers deploy checklist |
| AC-SEC-007 | `RMC-SPEC-IAM-001.4`, `RMC-SPEC-LIVE-001` | MANUAL | Staging/prod MFA policy checklist |
| AC-SEC-008 | `RMC-SPEC-NN-001.7` | PARTIAL | Architecture/doc review + checkout flow (no card fields on origin) |
| AC-OBS-001 | `RMC-SPEC-OBS-001`, `RMC-SPEC-STACK-001.3` | GAP | Observability suite *(planned)* |
| AC-OBS-002 | `RMC-SPEC-OBS-001`, `RMC-SPEC-ARCH-001.3` | GAP | Correlation-ID suite *(planned)* |
| AC-OBS-003 | `RMC-SPEC-OBS-001.2` | GAP | Alerts/metrics suite *(planned)* |
| AC-CFG-001 | `RMC-SPEC-CFG-001.1`, `RMC-SPEC-CXL-001.2` | COVERED | `rmc_backend/.../BookingRefundPolicySnapshotServiceTest.java` |
| AC-CFG-002 | `RMC-SPEC-CFG-001.1a` | COVERED | `rmc_backend/.../RefundPolicyConfigServiceTest.java` |
| AC-CFG-003 | `RMC-SPEC-CFG-001`, `RMC-SPEC-STAFF-001.5` | COVERED | `rmc_backend/.../RefundPolicyConfigServiceTest.java` |
| AC-PROMO-001 | `RMC-SPEC-PROMO-001.1`, `.2`, `STAFF-001.5` | COVERED | `PromoCodeServiceTest` + staff promo-codes page |
| AC-PROMO-002 | `RMC-SPEC-PROMO-001.3`–`.5`, Appendix G | COVERED | `PromoCodeServiceTest` |
| AC-PROMO-003 | `RMC-SPEC-PROMO-001.3`, `.6` | COVERED | `PromoCodeServiceTest` (max uses) |
| AC-PROMO-004 | `RMC-SPEC-PROMO-001.6` | COVERED | `PromoCodeServiceTest` (consume/release) |
| AC-PROMO-005 | `RMC-SPEC-PROMO-001.1`, `GUEST-001.2b` | COVERED | `PromoCodeServiceTest` (dual-code required) |
| AC-OPS-001 | `RMC-SPEC-CICD-001.1`, `RMC-SPEC-DATA-001` | PARTIAL | Flyway migrations under `rmc_backend/src/main/resources/db/migration/` + CI migrate step |
| AC-OPS-002 | `RMC-SPEC-CICD-001.2` | MANUAL | Staging deploy smoke |
| AC-OPS-003 | `RMC-SPEC-LIVE-001` | MANUAL | Secrets store + backup/restore posture |
| AC-OPS-004 | `RMC-SPEC-LIVE-001.1`, `.2` | MANUAL | Staging UAT evidence in `docs/VALIDATION_RUN.md` |
| AC-DOC-001 | Appendix F | COVERED | `docs/SPEC.md` Appendix F |
| AC-DOC-002 | Appendices E/G/H | COVERED | `docs/SPEC.md` Appendices E, G, H |
| AC-DOC-003 | Appendices D/I/J, §4 | COVERED | `docs/SPEC.md` API/RBAC/routes |
| AC-DOC-004 | Appendices A/M | PARTIAL | Seed branding specified; demo profile checklist in Appendix M |
| AC-UI-001 | `RMC-SPEC-UX-001.10` | MANUAL | Screenshot review vs SoT (chrome + critical guest); evidence in `docs/VALIDATION_RUN.md` |
| AC-UI-002 | `RMC-SPEC-UX-001.11`, `RMC-SPEC-STAFF-001.9` | GAP | `rmc_frontend/tests/e2e/staff-theme.spec.ts` *(planned)* |
| AC-UI-003 | `RMC-SPEC-UX-001.9`, `.12` | MANUAL | Carousel focus-hover + reduced-motion vs SoT / live |
| AC-UI-004 | `RMC-SPEC-UX-001.13`…`.19`, `STAFF-001.8`, `.10` | MANUAL | Brand/chrome/seed + checkout CTAs + General settings + StaffModal + guests/gallery/charts/filters/wizard/ops |

---

## Existing automated artifacts

| Path | Helps prove |
|---|---|
| `rmc_backend/src/test/java/.../MayaPaymentServiceTest.java` | AC-PAY-001, AC-PAY-007 |
| `rmc_backend/src/test/java/.../MayaWebhookSecurityServiceTest.java` | AC-PAY-003, AC-SEC-005 |
| `rmc_backend/src/test/java/.../MayaRefundServiceTest.java` | AC-CXL-003 |
| `rmc_backend/src/test/java/.../RefundPolicyServiceTest.java` | AC-CXL-001, AC-CXL-002 |
| `rmc_backend/src/test/java/.../StaffRefundServiceTest.java` | AC-CXL-003 |
| `rmc_backend/src/test/java/.../BookingLifecycleServiceTest.java` | AC-GUEST-002/003, AC-INV-001/002, AC-PAY-005/006 |
| `rmc_backend/src/test/java/.../InMemoryGuestBookingRateLimiterTest.java` | AC-SEC-002 |
| `rmc_backend/src/test/java/.../MayaCheckoutStatusTest.java` | Maya DTO/status helpers |
| `rmc_backend/src/test/java/.../RmcApplicationTests.java` | App context smoke |

---

## Scoreboard

| Status | Count (approx.) |
|---|---:|
| COVERED | 10 |
| PARTIAL | 12 |
| GAP | 25 |
| MANUAL | 10 |

**Priority gaps — product hardening:** AC-PAY-002, AC-PAY-004, AC-PAY-010, AC-INV-004, AC-STAFF-001, AC-GUEST-005.  
**Priority gaps — product requirements:** AC-CXL-004 … AC-CXL-007 (auto-refund + messaging + pay-at-hotel exclusion), AC-CFG-001.  

---

## How to use

1. When adding a test, update this matrix and `rmc_backend/src/test/resources/ac-coverage.tsv`.  
2. When claiming an AC PASS in `VALIDATION_RUN.md`, the owner here must be COVERED or MANUAL with dated evidence.  
3. Do not mark COVERED for planned paths that do not exist on the branch under test.  
4. A SPEC requirement is not PASS until its mapped AC(s) pass (see VALIDATION pass rule).
