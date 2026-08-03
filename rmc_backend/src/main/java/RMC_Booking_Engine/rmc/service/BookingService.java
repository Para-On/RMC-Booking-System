package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAdditionalGuest;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.entity.Guest;
import RMC_Booking_Engine.rmc.domain.entity.InventoryHold;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.dto.AdditionalChargeDto;
import RMC_Booking_Engine.rmc.dto.AdditionalGuestRequest;
import RMC_Booking_Engine.rmc.dto.AppliedPromoDto;
import RMC_Booking_Engine.rmc.dto.BookingOccupantDto;
import RMC_Booking_Engine.rmc.dto.BookingResponse;
import RMC_Booking_Engine.rmc.dto.BookingStatusResponse;
import RMC_Booking_Engine.rmc.dto.CreateBookingRequest;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutCreated;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingAdditionalGuestRepository;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.GuestRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final BookingAdditionalGuestRepository bookingAdditionalGuestRepository;
    private final AvailabilityService availabilityService;
    private final PricingService pricingService;
    private final ConfigService configService;
    private final BookingReferenceGenerator referenceGenerator;
    private final MayaCheckoutClient mayaCheckoutClient;
    private final MayaPaymentService mayaPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefundPolicyService refundPolicyService;
    private final BookingRefundPolicySnapshotService bookingRefundPolicySnapshotService;
    private final BookingHoldService bookingHoldService;
    private final BookingCancellationService bookingCancellationService;
    private final RoomCatalogMapper roomCatalogMapper;
    private final RoomExtrasService roomExtrasService;
    private final PromoService promoService;
    private final PromoCodeService promoCodeService;
    private final AdditionalChargeService additionalChargeService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        PaymentMethod paymentMethod = parsePaymentMethod(request.paymentMethod());

        if (paymentMethod == PaymentMethod.ONLINE_MAYA) {
            mayaPaymentService.assertMayaConfigured();
        }

        if (!request.ageConfirmed()) {
            throw new BusinessException("You must confirm you are 18 or older");
        }
        if (!request.dpaConsentAccepted()) {
            throw new BusinessException("DPA consent is required");
        }

        RoomType roomType = roomTypeRepository.findByIdForUpdate(request.roomTypeId())
                .orElseThrow(() -> new BusinessException("Room type not found"));

        RatePlan ratePlan = ratePlanRepository.findByIdWithRoomType(request.ratePlanId())
                .orElseThrow(() -> new BusinessException("Rate plan not found"));

        if (!ratePlan.getRoomType().getId().equals(roomType.getId())) {
            throw new BusinessException("Rate plan does not match room type");
        }
        if (!Boolean.TRUE.equals(ratePlan.getActive())) {
            throw new BusinessException("Rate plan is not active");
        }

        availabilityService.assertAvailable(roomType, request.checkIn(), request.checkOut());
        List<NightlyRateDto> breakdown = pricingService.calculateStayPricing(
                ratePlan.getId(), request.checkIn(), request.checkOut());
        BigDecimal roomTotalBeforePromo = pricingService.sumTaxInclusive(breakdown);
        BigDecimal roomTotal = roomTotalBeforePromo;
        Optional<PromoCodeMatch> promoCodeMatch =
                promoCodeService.tryResolveForRatePlan(
                        ratePlan.getId(),
                        roomTotalBeforePromo,
                        request.promoType(),
                        request.offerCode(),
                        request.organizationCode());
        Optional<PromoMatch> promoMatch = Optional.empty();
        if (promoCodeMatch.isPresent()) {
            roomTotal = promoCodeService.applyAmountOff(
                    roomTotalBeforePromo, promoCodeMatch.get().amountOff());
        } else {
            // Expired / invalid / exhausted sticky codes must not block booking — fall back to
            // automatic public promo (if any) or rack.
            promoMatch = promoService.findBestPromo(roomType.getId(), roomTotalBeforePromo);
            if (promoMatch.isPresent()) {
                roomTotal = promoService.applyAmountOff(roomTotalBeforePromo, promoMatch.get().amountOff());
            }
        }
        BigDecimal quotedTotal = roomTotal;

        Instant now = Instant.now();
        Guest guest = upsertGuest(
                request.fullName(),
                request.email(),
                request.phone(),
                now,
                true);

        String reference = generateUniqueReference(request.checkIn());
        Instant holdExpiry = paymentMethod == PaymentMethod.ONLINE_MAYA
                ? now.plusSeconds(ratePlan.getHoldTtlMinutes() * 60L)
                : null;

        Booking booking = new Booking();
        booking.setRoomType(roomType);
        booking.setRatePlan(ratePlan);
        booking.setGuest(guest);
        booking.setReference(reference);
        booking.setCheckInDate(request.checkIn());
        booking.setCheckOutDate(request.checkOut());
        booking.setPaymentMethod(paymentMethod);
        booking.setQuotedTotal(quotedTotal);
        booking.setCurrency("PHP");
        booking.setCreatedAt(now);
        booking.setExpiresAt(holdExpiry);
        if (promoCodeMatch.isPresent()) {
            PromoCodeMatch match = promoCodeMatch.get();
            booking.setPromoCode(match.promoCode());
            booking.setPromoName(match.promoCode().getName());
            booking.setPromoDiscountAmount(match.amountOff());
            booking.setRoomTotalBeforePromo(roomTotalBeforePromo);
        } else if (promoMatch.isPresent()) {
            PromoMatch match = promoMatch.get();
            booking.setPromo(match.promo());
            booking.setPromoName(match.promo().getName());
            booking.setPromoDiscountAmount(match.amountOff());
            booking.setRoomTotalBeforePromo(roomTotalBeforePromo);
        }

        if (paymentMethod == PaymentMethod.ONLINE_MAYA) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
        } else {
            booking.setStatus(BookingStatus.PENDING_APPROVAL);
            booking.setPaymentMethod(PaymentMethod.PAY_AT_HOTEL);
        }

        booking = bookingRepository.save(booking);

        if (promoCodeMatch.isPresent()) {
            promoCodeService.consumeUsage(promoCodeMatch.get().promoCode());
        }

        saveAdditionalGuests(booking, request.additionalGuests());

        if (paymentMethod != PaymentMethod.ONLINE_MAYA) {
            bookingRefundPolicySnapshotService.attachSnapshotIfAbsent(booking, roomType);
        }

        BigDecimal extrasTotal = roomExtrasService.applyBookingExtras(
                booking, request.serviceAddonIds(), request.itemAddons(), request.customExtrasRequest());
        if (extrasTotal.compareTo(BigDecimal.ZERO) > 0) {
            quotedTotal = roomTotal.add(extrasTotal);
            booking.setQuotedTotal(quotedTotal);
            bookingRepository.save(booking);
        }

        for (LocalDate date = request.checkIn(); date.isBefore(request.checkOut()); date = date.plusDays(1)) {
            InventoryHold hold = new InventoryHold();
            hold.setBooking(booking);
            hold.setRoomType(roomType);
            hold.setHoldDate(date);
            hold.setHeldCount(1);
            hold.setStatus(HoldStatus.ACTIVE);
            hold.setExpiresAt(holdExpiry);
            inventoryHoldRepository.save(hold);
        }

        BookingLedger debit = new BookingLedger();
        debit.setBooking(booking);
        debit.setEntryType(LedgerEntryType.DEBIT);
        debit.setAmount(quotedTotal);
        debit.setIdempotencyKey("debit-" + reference);
        debit.setCreatedAt(now);
        bookingLedgerRepository.save(debit);

        String checkoutRedirectUrl = null;
        if (paymentMethod == PaymentMethod.ONLINE_MAYA) {
            MayaCheckoutCreated checkout = mayaCheckoutClient.createCheckout(
                    reference,
                    quotedTotal,
                    booking.getCurrency(),
                    roomType.getName() + " stay",
                    guest);
            booking.setMayaCheckoutId(checkout.checkoutId());
            bookingRepository.save(booking);
            checkoutRedirectUrl = checkout.redirectUrl();
            writeAuditLog(booking, null, BookingStatus.PENDING_PAYMENT.name(), "GUEST_BOOKING_MAYA", null, null);
            eventPublisher.publishEvent(new BookingPendingEvent(booking.getId()));
        } else {
            writeAuditLog(booking, null, BookingStatus.PENDING_APPROVAL.name(), "GUEST_BOOKING_PAY_LATER", null, null);
            eventPublisher.publishEvent(new BookingPendingApprovalEvent(booking.getId()));
        }

        return toResponse(booking, breakdown, checkoutRedirectUrl);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String reference, String email) {
        Booking booking = findBookingForGuest(reference, email);
        List<NightlyRateDto> breakdown = pricingService.calculateStayPricing(
                booking.getRatePlan().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate());
        return toResponse(booking, breakdown, null);
    }

    @Transactional(readOnly = true)
    public BookingStatusResponse getStatus(String reference) {
        Booking booking = bookingRepository.findByReference(reference)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        return new BookingStatusResponse(
                booking.getReference(),
                booking.getStatus().name(),
                booking.getPaymentMethod().name());
    }

    @Transactional
    public BookingResponse cancelBooking(String reference, String email) {
        Booking booking = findBookingForGuest(reference, email);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking, pricingService.calculateStayPricing(
                    booking.getRatePlan().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()), null);
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL
                && booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.CONFIRMED_PAY_LATER) {
            throw new BusinessException("This booking cannot be cancelled online. Please contact the hotel.");
        }

        if (booking.getCheckedInAt() != null || booking.getCheckedOutAt() != null) {
            throw new BusinessException("Checked-in bookings cannot be cancelled online. Please contact the hotel.");
        }

        bookingCancellationService.cancelBooking(
                booking,
                null,
                null,
                "GUEST_CANCEL",
                "GUEST_CANCEL_REFUND_PENDING");

        return toResponse(booking, pricingService.calculateStayPricing(
                booking.getRatePlan().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()), null);
    }

    private BigDecimal sumLedgerByType(List<BookingLedger> entries, LedgerEntryType type) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(BookingLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void releaseHolds(Booking booking) {
        List<InventoryHold> holds = inventoryHoldRepository.findByBookingIdAndStatus(
                booking.getId(), HoldStatus.ACTIVE);
        for (InventoryHold hold : holds) {
            hold.setStatus(HoldStatus.RELEASED);
        }
        inventoryHoldRepository.saveAll(holds);
    }

    private Booking findBookingForGuest(String reference, String email) {
        Booking booking = bookingRepository.findByReferenceWithDetails(reference)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        if (!booking.getGuest().getEmail().equalsIgnoreCase(email.trim())) {
            throw new BusinessException("Booking not found");
        }
        return booking;
    }

    private String generateUniqueReference(LocalDate checkIn) {
        for (int i = 0; i < 10; i++) {
            String reference = referenceGenerator.generate(checkIn);
            if (!bookingRepository.existsByReference(reference)) {
                return reference;
            }
        }
        throw new BusinessException("Unable to generate booking reference");
    }

    private PaymentMethod parsePaymentMethod(String value) {
        try {
            return PaymentMethod.valueOf(value);
        } catch (Exception ex) {
            throw new BusinessException("Invalid payment method");
        }
    }

    private void writeAuditLog(
            Booking booking,
            String fromStatus,
            String toStatus,
            String trigger,
            Long staffUserId,
            String reason) {
        BookingAuditLog log = new BookingAuditLog();
        log.setBooking(booking);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setTriggerSource(trigger);
        log.setStaffUserId(staffUserId);
        log.setReason(reason);
        log.setCreatedAt(Instant.now());
        bookingAuditLogRepository.save(log);
    }

    private BookingResponse toResponse(Booking booking, List<NightlyRateDto> breakdown, String checkoutRedirectUrl) {
        var policySnapshot = bookingRefundPolicySnapshotService.resolveSnapshot(booking);
        String policyDescription = policySnapshot.description() != null
                ? policySnapshot.description()
                : refundPolicyService.previewCancellation(booking, BigDecimal.ZERO).policySummary();

        String refundPreview = null;
        if (booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.CONFIRMED_PAY_LATER) {
            List<BookingLedger> ledgerEntries =
                    bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
            BigDecimal paidAmount = sumLedgerByType(ledgerEntries, LedgerEntryType.CREDIT);
            var preview = refundPolicyService.previewCancellation(booking, paidAmount);
            if (preview.cancellationAllowedNow()) {
                refundPreview = formatRefundPreview(preview);
            } else if (preview.blockReason() != null) {
                refundPreview = preview.blockReason();
            }
        }

        AppliedPromoDto promoDto = null;
        if (booking.getPromoDiscountAmount() != null
                && booking.getPromoDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && booking.getPromoName() != null) {
            promoDto = new AppliedPromoDto(
                    booking.getPromo() != null ? booking.getPromo().getId() : null,
                    booking.getPromoName(),
                    null,
                    null,
                    null,
                    booking.getPromoDiscountAmount(),
                    booking.getPromoName());
        }

        List<BookingLedger> allLedger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal amountPaid = AdditionalChargeService.sumCredits(allLedger);
        BigDecimal balanceDue = AdditionalChargeService.balanceOf(allLedger);
        boolean canCancel = resolveCanCancel(booking, allLedger);
        List<AdditionalChargeDto> charges = additionalChargeService.listForBooking(booking.getId());

        return new BookingResponse(
                booking.getReference(),
                booking.getStatus().name(),
                booking.getPaymentMethod().name(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getQuotedTotal(),
                booking.getCurrency(),
                booking.getGuest().getFullName(),
                booking.getGuest().getEmail(),
                booking.getRoomType().getName(),
                breakdown,
                checkoutRedirectUrl,
                roomCatalogMapper.toCard(booking.getRoomType()),
                roomExtrasService.getServiceSelectionsForBooking(booking.getId()),
                roomExtrasService.getItemSelectionsForBooking(booking.getId()),
                booking.getCustomExtrasRequest(),
                booking.getCancellationTier() != null ? booking.getCancellationTier().name() : null,
                booking.getRefundStatus() != null ? booking.getRefundStatus().name() : null,
                booking.getRefundEligibleAmount(),
                buildCancellationMessage(booking),
                policyDescription,
                refundPreview,
                booking.getRefundPercentApplied(),
                booking.getDeductionAmount(),
                booking.getCancellationReason(),
                buildOccupants(booking).size(),
                buildOccupants(booking),
                promoDto,
                booking.getRoomTotalBeforePromo(),
                amountPaid,
                balanceDue,
                canCancel,
                charges);
    }

    private Guest upsertGuest(
            String fullName, String email, String phone, Instant now, boolean primaryConsent) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new BusinessException("Email is required");
        }
        String trimmedName = fullName == null ? "" : fullName.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();
        if (trimmedName.isBlank()) {
            throw new BusinessException("Full name is required");
        }
        if (trimmedPhone.isBlank() && primaryConsent) {
            throw new BusinessException("Phone is required");
        }

        Guest guest = guestRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(Guest::new);
        boolean isNew = guest.getId() == null;
        guest.setEmail(normalizedEmail);
        guest.setFullName(trimmedName);
        if (!trimmedPhone.isBlank()) {
            guest.setPhone(trimmedPhone);
        } else if (isNew) {
            guest.setPhone("—");
        }
        if (primaryConsent || isNew) {
            guest.setConsentTimestamp(now);
            guest.setDpaConsentVersion(configService.getDpaConsentVersion());
            guest.setAgeConfirmedAt(now);
        } else if (guest.getConsentTimestamp() == null) {
            guest.setConsentTimestamp(now);
            guest.setDpaConsentVersion(configService.getDpaConsentVersion());
            guest.setAgeConfirmedAt(now);
        }
        return guestRepository.save(guest);
    }

    private void saveAdditionalGuests(Booking booking, List<AdditionalGuestRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        String primaryEmail = normalizeEmail(booking.getGuest().getEmail());
        int adults = booking.getRoomType().getMaxAdults() != null ? booking.getRoomType().getMaxAdults() : 1;
        int children = booking.getRoomType().getMaxChildren() != null ? booking.getRoomType().getMaxChildren() : 0;
        int capacity = Math.max(1, adults + children);
        if (1 + requests.size() > capacity) {
            throw new BusinessException(
                    "This room can accommodate up to " + capacity + " guests");
        }

        int order = 0;
        for (AdditionalGuestRequest request : requests) {
            if (request == null || request.fullName() == null || request.fullName().isBlank()) {
                continue;
            }
            String name = request.fullName().trim();
            String email = normalizeEmail(request.email());
            String phone = request.phone() == null ? null : request.phone().trim();
            if (phone != null && phone.isBlank()) {
                phone = null;
            }
            if (email != null && email.equals(primaryEmail)) {
                throw new BusinessException(
                        "Additional guests must use a different email from the primary guest");
            }

            BookingAdditionalGuest additional = new BookingAdditionalGuest();
            additional.setBooking(booking);
            additional.setGuest(null);
            additional.setFullName(name);
            additional.setEmail(email);
            additional.setPhone(phone);
            additional.setSortOrder(order++);
            bookingAdditionalGuestRepository.save(additional);
        }
    }

    private List<BookingOccupantDto> buildOccupants(Booking booking) {
        List<BookingOccupantDto> occupants = new ArrayList<>();
        Guest primary = booking.getGuest();
        occupants.add(new BookingOccupantDto(
                primary.getId(),
                primary.getFullName(),
                primary.getEmail(),
                primary.getPhone(),
                true));
        for (BookingAdditionalGuest additional :
                bookingAdditionalGuestRepository.findByBookingIdOrderBySortOrderAscIdAsc(booking.getId())) {
            occupants.add(new BookingOccupantDto(
                    additional.getGuest() != null ? additional.getGuest().getId() : null,
                    additional.getFullName(),
                    additional.getEmail(),
                    additional.getPhone(),
                    false));
        }
        return occupants;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean resolveCanCancel(Booking booking, List<BookingLedger> ledgerEntries) {
        if (booking.getCheckedInAt() != null || booking.getCheckedOutAt() != null) {
            return false;
        }
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                || booking.getStatus() == BookingStatus.PENDING_APPROVAL) {
            return true;
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.CONFIRMED_PAY_LATER) {
            return false;
        }
        BigDecimal paidAmount = sumLedgerByType(ledgerEntries, LedgerEntryType.CREDIT);
        return refundPolicyService.evaluateCancellation(booking, paidAmount).allowed();
    }

    private String formatRefundPreview(RMC_Booking_Engine.rmc.dto.RefundPolicyPreviewDto preview) {
        if (preview.refundAmountIfCancelledNow() == null) {
            return preview.policySummary();
        }
        if (preview.refundAmountIfCancelledNow().compareTo(BigDecimal.ZERO) <= 0) {
            return "If you cancel now, no refund will be issued.";
        }
        return "If you cancel now: "
                + preview.refundPercentIfCancelledNow()
                + "% refund ("
                + preview.refundAmountIfCancelledNow()
                + " "
                + preview.tierIfCancelledNow()
                + ").";
    }

    private String buildCancellationMessage(Booking booking) {
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            return null;
        }
        RefundStatus refundStatus = booking.getRefundStatus();
        if (refundStatus == RefundStatus.COMPLETED) {
            return "Your booking is cancelled. Your refund has been processed.";
        }
        if (refundStatus == RefundStatus.PENDING) {
            String amountPart = booking.getRefundEligibleAmount() != null
                    ? " Estimated refund: " + booking.getRefundEligibleAmount() + " " + booking.getCurrency() + "."
                    : "";
            if (booking.getCancellationTier() == CancellationTier.PARTIAL) {
                return "Your booking is cancelled. A partial refund will be processed by our team." + amountPart;
            }
            return "Your booking is cancelled. Your refund will be processed by our team." + amountPart;
        }
        if (refundStatus == RefundStatus.FAILED) {
            return "Your booking is cancelled. Refund processing failed — our team will follow up.";
        }
        if (refundStatus == RefundStatus.NONE || refundStatus == RefundStatus.NOT_APPLICABLE) {
            return "Your booking is cancelled.";
        }
        return "Your booking is cancelled.";
    }
}
