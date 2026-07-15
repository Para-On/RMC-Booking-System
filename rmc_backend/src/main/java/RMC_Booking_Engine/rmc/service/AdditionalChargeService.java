package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAdditionalCharge;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.AdditionalChargeStatus;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.AdditionalChargeDto;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutCreated;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingAdditionalChargeRepository;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalChargeService {

    public static final String MAYA_REQUEST_PREFIX = "AC-";

    private static final Set<BookingStatus> CHARGEABLE_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private static final Set<AdditionalChargeStatus> MAYA_PAYABLE = EnumSet.of(
            AdditionalChargeStatus.AWAITING_PAYMENT,
            AdditionalChargeStatus.APPROVED_UNPAID,
            AdditionalChargeStatus.PENDING_MAYA,
            AdditionalChargeStatus.PENDING_APPROVAL);

    private static final Set<AdditionalChargeStatus> HOTEL_PAYABLE = EnumSet.of(
            AdditionalChargeStatus.AWAITING_PAYMENT,
            AdditionalChargeStatus.APPROVED_UNPAID,
            AdditionalChargeStatus.PENDING_APPROVAL);

    private final BookingAdditionalChargeRepository chargeRepository;
    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final MayaCheckoutClient mayaCheckoutClient;

    @Transactional
    public AdditionalChargeDto createCharge(Long bookingId, String description, BigDecimal amount, StaffPrincipal staff) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        if (!CHARGEABLE_STATUSES.contains(booking.getStatus())) {
            throw new BusinessException("Additional charges can only be added to approved bookings");
        }
        if (booking.getCheckedOutAt() != null) {
            throw new BusinessException("Cannot add charges after check-out");
        }

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Charge amount must be greater than zero");
        }
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isBlank()) {
            throw new BusinessException("Description is required");
        }

        Instant now = Instant.now();
        BookingAdditionalCharge charge = new BookingAdditionalCharge();
        charge.setBooking(booking);
        charge.setDescription(trimmed);
        charge.setAmount(normalized);
        charge.setCurrency(booking.getCurrency());
        charge.setStatus(AdditionalChargeStatus.APPROVED_UNPAID);
        charge.setPaymentMethod(PaymentMethod.PAY_AT_HOTEL);
        charge.setCreatedByStaffId(staff.id());
        charge.setApprovedByStaffId(staff.id());
        charge.setApprovedAt(now);
        charge.setCreatedAt(now);
        charge.setUpdatedAt(now);
        charge = chargeRepository.save(charge);

        postDebitIfAbsent(booking, charge);
        bumpQuotedTotal(booking, charge.getAmount());

        writeAudit(booking, "STAFF_CREATE_ADDITIONAL_CHARGE", staff.id(),
                trimmed + " " + normalized.toPlainString() + " " + booking.getCurrency());
        return toDto(charge, null);
    }

    @Transactional(readOnly = true)
    public List<AdditionalChargeDto> listForBooking(Long bookingId) {
        return chargeRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(c -> toDto(c, null))
                .toList();
    }

    @Transactional
    public AdditionalChargeDto guestPay(String reference, Long chargeId, String email, String paymentMethodRaw) {
        Booking booking = findBookingForGuest(reference, email);
        BookingAdditionalCharge charge = chargeRepository.findByIdAndBookingId(chargeId, booking.getId())
                .orElseThrow(() -> new BusinessException("Charge not found"));

        PaymentMethod method = parsePaymentMethod(paymentMethodRaw);
        if (method == PaymentMethod.ONLINE_MAYA) {
            return startMayaPayment(booking, charge);
        }
        throw new BusinessException(
                "Pay-at-hotel for additional charges is handled by hotel staff. Use Maya to pay online.");
    }

    @Transactional
    public AdditionalChargeDto approvePayAtHotel(Long bookingId, Long chargeId, StaffPrincipal staff) {
        // Legacy: older pending requests — approve by posting debit if needed.
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        BookingAdditionalCharge charge = chargeRepository.findByIdAndBookingId(chargeId, bookingId)
                .orElseThrow(() -> new BusinessException("Charge not found"));
        if (charge.getStatus() != AdditionalChargeStatus.PENDING_APPROVAL
                && charge.getStatus() != AdditionalChargeStatus.AWAITING_PAYMENT) {
            throw new BusinessException("This charge does not require approval");
        }

        Instant now = Instant.now();
        String debitKey = "debit-charge-" + charge.getId();
        boolean debitAlreadyPosted = bookingLedgerRepository.existsByIdempotencyKey(debitKey);
        postDebitIfAbsent(booking, charge);
        if (!debitAlreadyPosted) {
            bumpQuotedTotal(booking, charge.getAmount());
        }
        charge.setStatus(AdditionalChargeStatus.APPROVED_UNPAID);
        charge.setPaymentMethod(PaymentMethod.PAY_AT_HOTEL);
        charge.setApprovedByStaffId(staff.id());
        charge.setApprovedAt(now);
        charge.setUpdatedAt(now);
        chargeRepository.save(charge);
        writeAudit(booking, "STAFF_APPROVE_ADDITIONAL_CHARGE", staff.id(),
                charge.getDescription() + " " + charge.getAmount().toPlainString());
        return toDto(charge, null);
    }

    @Transactional
    public AdditionalChargeDto rejectPayAtHotel(Long bookingId, Long chargeId, String reason, StaffPrincipal staff) {
        BookingAdditionalCharge charge = chargeRepository.findByIdAndBookingId(chargeId, bookingId)
                .orElseThrow(() -> new BusinessException("Charge not found"));
        if (charge.getStatus() != AdditionalChargeStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending pay-at-hotel charges can be rejected");
        }
        Instant now = Instant.now();
        charge.setStatus(AdditionalChargeStatus.REJECTED);
        charge.setRejectedAt(now);
        charge.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Rejected by staff");
        charge.setUpdatedAt(now);
        chargeRepository.save(charge);
        writeAudit(charge.getBooking(), "STAFF_REJECT_ADDITIONAL_CHARGE", staff.id(), charge.getRejectionReason());
        return toDto(charge, null);
    }

    /**
     * Records hotel cash/card collection for an approved unpaid charge — posts CREDIT (revenue).
     */
    @Transactional
    public AdditionalChargeDto recordHotelPayment(Long bookingId, Long chargeId, StaffPrincipal staff) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        BookingAdditionalCharge charge = chargeRepository.findByIdAndBookingId(chargeId, bookingId)
                .orElseThrow(() -> new BusinessException("Charge not found"));
        if (!HOTEL_PAYABLE.contains(charge.getStatus())) {
            throw new BusinessException("Only unpaid charges can be marked paid at hotel");
        }

        String debitKey = "debit-charge-" + charge.getId();
        boolean debitAlreadyPosted = bookingLedgerRepository.existsByIdempotencyKey(debitKey);
        postDebitIfAbsent(booking, charge);
        if (!debitAlreadyPosted) {
            bumpQuotedTotal(booking, charge.getAmount());
        }

        postCreditIfAbsent(booking, charge, "hotel-pay-charge-" + charge.getId(), null);
        Instant now = Instant.now();
        charge.setStatus(AdditionalChargeStatus.PAID);
        charge.setPaymentMethod(PaymentMethod.PAY_AT_HOTEL);
        charge.setPaidAt(now);
        charge.setUpdatedAt(now);
        chargeRepository.save(charge);
        writeAudit(booking, "STAFF_RECORD_CHARGE_PAYMENT", staff.id(),
                charge.getDescription() + " " + charge.getAmount().toPlainString());
        return toDto(charge, null);
    }

    /**
     * Records payment for any open folio balance (room pay-later + unpaid approved charges).
     */
    @Transactional
    public BigDecimal recordOutstandingPayment(Long bookingId, StaffPrincipal staff) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        List<BookingLedger> entries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal outstanding = balanceOf(entries);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No outstanding balance to record");
        }

        String creditKey = "staff-folio-credit-" + booking.getReference() + "-" + Instant.now().toEpochMilli();
        BookingLedger credit = new BookingLedger();
        credit.setBooking(booking);
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(outstanding);
        credit.setIdempotencyKey(creditKey);
        credit.setCreatedAt(Instant.now());
        bookingLedgerRepository.save(credit);

        Instant now = Instant.now();
        for (BookingAdditionalCharge charge : chargeRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId())) {
            if (HOTEL_PAYABLE.contains(charge.getStatus())
                    || charge.getStatus() == AdditionalChargeStatus.PENDING_MAYA) {
                charge.setStatus(AdditionalChargeStatus.PAID);
                charge.setPaidAt(now);
                charge.setUpdatedAt(now);
                chargeRepository.save(charge);
            }
        }

        writeAudit(booking, "STAFF_RECORD_FOLIO_PAYMENT", staff.id(),
                outstanding.toPlainString() + " " + booking.getCurrency());
        return outstanding;
    }

    @Transactional
    public void confirmMayaPaymentByChargeId(Long chargeId) {
        BookingAdditionalCharge charge = chargeRepository.findByIdWithBooking(chargeId)
                .orElseThrow(() -> new BusinessException("Charge not found"));
        if (charge.getStatus() == AdditionalChargeStatus.PAID) {
            return;
        }
        if (charge.getMayaCheckoutId() == null || charge.getMayaCheckoutId().isBlank()) {
            throw new BusinessException("No Maya checkout associated with this charge");
        }
        MayaCheckoutStatus checkout = mayaCheckoutClient.getCheckout(charge.getMayaCheckoutId());
        settleMayaPayment(charge, checkout, "MAYA_CHARGE_CONFIRM_POLL");
    }

    @Transactional
    public boolean tryHandleMayaPayload(MayaCheckoutStatus payload, String trigger) {
        if (payload == null) {
            return false;
        }
        String requestRef = payload.requestReferenceNumber();
        BookingAdditionalCharge charge = null;
        if (requestRef != null && requestRef.startsWith(MAYA_REQUEST_PREFIX)) {
            charge = chargeRepository.findByMayaRequestRef(requestRef).orElse(null);
        }
        if (charge == null && payload.resolvedId() != null) {
            charge = chargeRepository.findByMayaCheckoutId(payload.resolvedId()).orElse(null);
        }
        if (charge == null) {
            return false;
        }
        charge = chargeRepository.findByIdWithBooking(charge.getId()).orElse(charge);
        settleMayaPayment(charge, payload, trigger);
        return true;
    }

    private void settleMayaPayment(BookingAdditionalCharge charge, MayaCheckoutStatus payload, String trigger) {
        if (charge.getStatus() == AdditionalChargeStatus.PAID) {
            return;
        }
        if (!payload.isPaymentSuccessful()) {
            log.info("Additional charge {} Maya payment not successful yet: {}", charge.getId(), payload.status());
            return;
        }

        BigDecimal received = payload.resolvedAmount();
        if (received != null && received.compareTo(charge.getAmount()) != 0) {
            log.error("Additional charge {} Maya amount mismatch: expected {} got {}",
                    charge.getId(), charge.getAmount(), received);
            return;
        }

        Booking booking = charge.getBooking();
        boolean needDebit = charge.getStatus() == AdditionalChargeStatus.PENDING_MAYA
                || charge.getStatus() == AdditionalChargeStatus.AWAITING_PAYMENT
                || charge.getStatus() == AdditionalChargeStatus.APPROVED_UNPAID;

        if (needDebit && charge.getStatus() != AdditionalChargeStatus.APPROVED_UNPAID) {
            // AWAITING / PENDING_MAYA: debit not yet on folio
            postDebitIfAbsent(booking, charge);
            bumpQuotedTotal(booking, charge.getAmount());
        } else if (charge.getStatus() == AdditionalChargeStatus.APPROVED_UNPAID) {
            // Debit already posted on staff approve
            postDebitIfAbsent(booking, charge);
        }

        String checkoutId = payload.id() != null ? payload.id() : charge.getMayaCheckoutId();
        postCreditIfAbsent(booking, charge, "maya-charge-" + charge.getId(), checkoutId);

        Instant now = Instant.now();
        charge.setStatus(AdditionalChargeStatus.PAID);
        charge.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        charge.setPaidAt(now);
        charge.setUpdatedAt(now);
        if (checkoutId != null) {
            charge.setMayaCheckoutId(checkoutId);
        }
        chargeRepository.save(charge);
        writeAudit(booking, trigger, null, "Additional charge paid via Maya: " + charge.getDescription());
        log.info("Additional charge {} paid via Maya for booking {}", charge.getId(), booking.getReference());
    }

    private AdditionalChargeDto startMayaPayment(Booking booking, BookingAdditionalCharge charge) {
        if (!MAYA_PAYABLE.contains(charge.getStatus())) {
            throw new BusinessException("This charge cannot be paid with Maya in its current status");
        }
        if (charge.getStatus() == AdditionalChargeStatus.PAID) {
            throw new BusinessException("This charge is already paid");
        }

        Instant now = Instant.now();
        if (charge.getMayaRequestRef() == null) {
            charge.setMayaRequestRef(MAYA_REQUEST_PREFIX + charge.getId());
        }
        charge.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        if (charge.getStatus() != AdditionalChargeStatus.APPROVED_UNPAID) {
            charge.setStatus(AdditionalChargeStatus.PENDING_MAYA);
        }
        charge.setUpdatedAt(now);
        chargeRepository.save(charge);

        MayaCheckoutCreated checkout = mayaCheckoutClient.createCheckout(
                charge.getMayaRequestRef(),
                booking.getReference(),
                charge.getAmount(),
                charge.getCurrency(),
                "Additional charge: " + charge.getDescription(),
                booking.getGuest(),
                charge.getId());
        charge.setMayaCheckoutId(checkout.checkoutId());
        chargeRepository.save(charge);
        writeAudit(booking, "GUEST_CHARGE_MAYA", null, charge.getDescription());
        return toDto(charge, checkout.redirectUrl());
    }

    private void postDebitIfAbsent(Booking booking, BookingAdditionalCharge charge) {
        String key = "debit-charge-" + charge.getId();
        if (bookingLedgerRepository.existsByIdempotencyKey(key)) {
            return;
        }
        BookingLedger debit = new BookingLedger();
        debit.setBooking(booking);
        debit.setEntryType(LedgerEntryType.DEBIT);
        debit.setAmount(charge.getAmount());
        debit.setIdempotencyKey(key);
        debit.setCreatedAt(Instant.now());
        try {
            bookingLedgerRepository.save(debit);
        } catch (DataIntegrityViolationException ex) {
            log.info("Idempotent skip duplicate charge debit {}", key);
        }
    }

    private void postCreditIfAbsent(Booking booking, BookingAdditionalCharge charge, String key, String mayaRef) {
        if (bookingLedgerRepository.existsByIdempotencyKey(key)) {
            return;
        }
        BookingLedger credit = new BookingLedger();
        credit.setBooking(booking);
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(charge.getAmount());
        credit.setIdempotencyKey(key);
        credit.setMayaReference(mayaRef);
        credit.setCreatedAt(Instant.now());
        try {
            bookingLedgerRepository.save(credit);
        } catch (DataIntegrityViolationException ex) {
            log.info("Idempotent skip duplicate charge credit {}", key);
        }
    }

    private void bumpQuotedTotal(Booking booking, BigDecimal amount) {
        BigDecimal current = booking.getQuotedTotal() != null ? booking.getQuotedTotal() : BigDecimal.ZERO;
        booking.setQuotedTotal(current.add(amount));
        bookingRepository.save(booking);
    }

    private Booking findBookingForGuest(String reference, String email) {
        Booking booking = bookingRepository.findByReferenceWithDetails(reference.trim())
                .orElseThrow(() -> new BusinessException("Booking not found"));
        if (!booking.getGuest().getEmail().equalsIgnoreCase(email.trim())) {
            throw new BusinessException("Booking not found");
        }
        return booking;
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Payment method is required");
        }
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid payment method");
        }
    }

    private void writeAudit(Booking booking, String trigger, Long staffId, String reason) {
        BookingAuditLog logEntry = new BookingAuditLog();
        logEntry.setBooking(booking);
        logEntry.setFromStatus(booking.getStatus().name());
        logEntry.setToStatus(booking.getStatus().name());
        logEntry.setTriggerSource(trigger);
        logEntry.setStaffUserId(staffId);
        logEntry.setReason(reason);
        logEntry.setCreatedAt(Instant.now());
        bookingAuditLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public AdditionalChargeDto getChargeDto(Long chargeId) {
        BookingAdditionalCharge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new BusinessException("Charge not found"));
        return toDto(charge, null);
    }

    public AdditionalChargeDto toDto(BookingAdditionalCharge charge, String redirectUrl) {
        AdditionalChargeStatus status = charge.getStatus();
        boolean canPayMaya = MAYA_PAYABLE.contains(status) && status != AdditionalChargeStatus.PAID;
        return new AdditionalChargeDto(
                charge.getId(),
                charge.getDescription(),
                charge.getAmount(),
                charge.getCurrency(),
                status.name(),
                charge.getPaymentMethod() != null ? charge.getPaymentMethod().name() : null,
                charge.getCreatedAt(),
                charge.getPaidAt(),
                charge.getApprovedAt(),
                charge.getRejectionReason(),
                canPayMaya,
                false,
                redirectUrl);
    }

    public static BigDecimal balanceOf(List<BookingLedger> entries) {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        for (BookingLedger entry : entries) {
            if (entry.getEntryType() == LedgerEntryType.DEBIT) {
                debits = debits.add(entry.getAmount());
            } else if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                credits = credits.add(entry.getAmount());
            } else if (entry.getEntryType() == LedgerEntryType.REFUND
                    || entry.getEntryType() == LedgerEntryType.MANUAL_REFUND) {
                refunds = refunds.add(entry.getAmount());
            }
        }
        return debits.subtract(credits).subtract(refunds);
    }

    public static BigDecimal sumCredits(List<BookingLedger> entries) {
        return entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.CREDIT)
                .map(BookingLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
