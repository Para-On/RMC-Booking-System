package RMC_Booking_Engine.rmc.service;



import RMC_Booking_Engine.rmc.domain.entity.Booking;

import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;

import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;

import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;

import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;

import RMC_Booking_Engine.rmc.domain.enums.ManualRefundMethod;

import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;

import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;

import RMC_Booking_Engine.rmc.dto.RefundResponse;

import RMC_Booking_Engine.rmc.exception.BusinessException;

import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;

import RMC_Booking_Engine.rmc.repository.BookingRepository;

import RMC_Booking_Engine.rmc.security.StaffPrincipal;

import RMC_Booking_Engine.rmc.service.MayaRefundService.MayaRefundTiming;

import RMC_Booking_Engine.rmc.service.MayaRefundService.RefundExecution;

import java.math.BigDecimal;

import java.util.List;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



@Service

@RequiredArgsConstructor

@Slf4j

public class StaffRefundService {



    private final BookingRepository bookingRepository;

    private final BookingLedgerRepository bookingLedgerRepository;

    private final BookingHoldService bookingHoldService;

    private final RefundPolicyService refundPolicyService;

    private final MayaRefundService mayaRefundService;

    private final ConfigService configService;

    private final EmailOutboxService emailOutboxService;



    @Transactional

    public RefundResponse processRefund(Long bookingId, BigDecimal requestedAmount, String reason, StaffPrincipal staff) {

        Booking booking = lockBooking(bookingId);

        validateMayaRefundBooking(booking);



        List<BookingLedger> ledgerEntries = loadLedger(booking.getId());

        BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(ledgerEntries);

        if (mayaRefundable.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessException("No refundable Maya payment remains on this booking");

        }



        BigDecimal refundCap = resolveRefundCap(booking, ledgerEntries, mayaRefundable);

        if (refundCap.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessException("Nothing remaining to refund on this booking");

        }



        BigDecimal refundAmount = requestedAmount != null ? requestedAmount : refundCap;

        if (refundAmount.compareTo(refundCap) > 0) {

            throw new BusinessException("Refund amount exceeds allowed refund of " + refundCap);

        }

        if (refundAmount.compareTo(mayaRefundable) > 0) {

            throw new BusinessException("Refund amount exceeds Maya refundable balance of " + mayaRefundable);

        }



        if (booking.getStatus() == BookingStatus.CANCELLED) {

            return processCancelledBookingMayaRefund(booking, ledgerEntries, refundAmount, reason, staff);

        }



        if (booking.getStatus() != BookingStatus.CONFIRMED) {

            throw new BusinessException("Only confirmed or cancelled bookings awaiting refund can be refunded");

        }

        if (!refundPolicyService.isWithinRefundWindow(booking)) {

            throw new BusinessException("Refund window has passed for this booking");

        }



        boolean preferVoid = booking.getCancellationTier() == null

                && refundAmount.compareTo(mayaRefundable) >= 0;

        RefundExecution refund = mayaRefundService.reversePayment(

                booking, refundAmount, reason, preferVoid);



        boolean fullRefund = refund.amount().compareTo(mayaRefundable) >= 0;



        if (fullRefund) {

            BookingStatus previous = booking.getStatus();

            booking.setStatus(BookingStatus.CANCELLED);

            booking.setCancelledAt(booking.getCancelledAt() != null ? booking.getCancelledAt() : java.time.Instant.now());

            booking.setCancellationTier(CancellationTier.FULL);

            booking.setRefundEligibleAmount(refund.amount());

            booking.setRefundStatus(RefundStatus.COMPLETED);

            bookingRepository.save(booking);

            bookingHoldService.releaseActiveHolds(booking);

            bookingHoldService.writeAuditLog(

                    booking, previous.name(), BookingStatus.CANCELLED.name(),

                    refund.method() == MayaRefundService.ReversalMethod.VOID

                            ? "STAFF_VOID"

                            : "STAFF_REFUND",

                    staff.id(), reason.trim());

            emailOutboxService.enqueueRefundProcessed(booking.getId(), refund.amount());

        } else {

            bookingHoldService.writeAuditLog(

                    booking, booking.getStatus().name(), booking.getStatus().name(),

                    "STAFF_PARTIAL_REFUND", staff.id(), reason.trim());

        }



        log.info("Maya refund {} for booking {} by staff {}", refund.amount(), booking.getReference(), staff.email());

        return buildRefundResponse(booking, refund.amount(), refund.mayaReferenceId());

    }



    @Transactional

    public RefundResponse processManualRefund(

            Long bookingId,

            BigDecimal requestedAmount,

            String reason,

            String externalReference,

            String method,

            StaffPrincipal staff) {

        if (!configService.isManualRefundEnabled()) {

            throw new BusinessException("Manual refunds are disabled for this organization");

        }



        Booking booking = lockBooking(bookingId);

        validateMayaRefundBooking(booking);

        ManualRefundMethod refundMethod = ManualRefundMethod.fromString(method);

        String normalizedReference = externalReference.trim();

        if (normalizedReference.isBlank()) {

            throw new BusinessException("External payment reference is required");

        }



        if (booking.getStatus() != BookingStatus.CANCELLED) {

            throw new BusinessException("Manual refunds are only available for cancelled bookings");

        }

        if (booking.getRefundStatus() != RefundStatus.PENDING

                && booking.getRefundStatus() != RefundStatus.FAILED) {

            throw new BusinessException("This booking does not have a pending refund");

        }



        List<BookingLedger> ledgerEntries = loadLedger(booking.getId());

        BigDecimal refundCap = resolveRefundCap(booking, ledgerEntries, mayaRefundService.mayaRefundableAmount(ledgerEntries));

        if (refundCap.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessException("Nothing remaining to refund on this booking");

        }



        BigDecimal refundAmount = requestedAmount != null ? requestedAmount : refundCap;

        if (refundAmount.compareTo(refundCap) > 0) {

            throw new BusinessException("Refund amount exceeds allowed refund of " + refundCap);

        }



        String idempotencySuffix = normalizedReference.replaceAll("[^a-zA-Z0-9._-]", "_");

        mayaRefundService.recordManualRefund(booking, refundAmount, normalizedReference, idempotencySuffix);



        BigDecimal remaining = mayaRefundService.remainingRefundDue(booking, loadLedger(booking.getId()));

        boolean completed = remaining.compareTo(BigDecimal.ZERO) <= 0;

        booking.setRefundStatus(completed ? RefundStatus.COMPLETED : RefundStatus.PENDING);

        bookingRepository.save(booking);



        String auditReason = reason.trim()

                + " [" + refundMethod.name() + " ref: " + normalizedReference + "]";

        bookingHoldService.writeAuditLog(

                booking,

                BookingStatus.CANCELLED.name(),

                BookingStatus.CANCELLED.name(),

                "STAFF_MANUAL_REFUND",

                staff.id(),

                auditReason);



        if (completed) {

            emailOutboxService.enqueueRefundProcessed(booking.getId(), refundAmount);

        }



        log.info("Manual refund {} for booking {} by staff {}", refundAmount, booking.getReference(), staff.email());

        return buildRefundResponse(booking, refundAmount, normalizedReference);

    }



    private RefundResponse processCancelledBookingMayaRefund(

            Booking booking,

            List<BookingLedger> ledgerEntries,

            BigDecimal refundAmount,

            String reason,

            StaffPrincipal staff) {

        if (booking.getRefundStatus() != RefundStatus.PENDING

                && booking.getRefundStatus() != RefundStatus.FAILED) {

            throw new BusinessException("This booking does not have a pending refund");

        }



        BigDecimal remainingBefore = mayaRefundService.remainingRefundDue(booking, ledgerEntries);

        if (remainingBefore.compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessException("Nothing remaining to refund on this booking");

        }



        boolean preferVoid = booking.getCancellationTier() == CancellationTier.FULL

                && mayaRefundService.isSamePaymentDay(ledgerEntries)

                && refundAmount.compareTo(mayaRefundService.mayaRefundableAmount(ledgerEntries)) >= 0

                && mayaRefundService.sumManualRefunds(ledgerEntries).compareTo(BigDecimal.ZERO) == 0;



        try {

            RefundExecution refund = mayaRefundService.reversePayment(

                    booking, refundAmount, reason, preferVoid);

            updateRefundStatusAfterPayment(booking, refund.amount());

            bookingHoldService.writeAuditLog(

                    booking,

                    BookingStatus.CANCELLED.name(),

                    BookingStatus.CANCELLED.name(),

                    refund.method() == MayaRefundService.ReversalMethod.VOID

                            ? "STAFF_VOID"

                            : "STAFF_REFUND",

                    staff.id(),

                    reason.trim());



            if (booking.getRefundStatus() == RefundStatus.COMPLETED) {

                emailOutboxService.enqueueRefundProcessed(booking.getId(), refund.amount());

            }



            return buildRefundResponse(booking, refund.amount(), refund.mayaReferenceId());

        } catch (BusinessException ex) {

            booking.setRefundStatus(RefundStatus.FAILED);

            bookingRepository.save(booking);

            throw ex;

        }

    }



    private void updateRefundStatusAfterPayment(Booking booking, BigDecimal refundedAmount) {

        BigDecimal remaining = mayaRefundService.remainingRefundDue(booking, loadLedger(booking.getId()));

        booking.setRefundStatus(remaining.compareTo(BigDecimal.ZERO) <= 0

                ? RefundStatus.COMPLETED

                : RefundStatus.PENDING);

        bookingRepository.save(booking);

    }



    private RefundResponse buildRefundResponse(Booking booking, BigDecimal refundedAmount, String paymentReference) {

        List<BookingLedger> updatedLedger = loadLedger(booking.getId());

        return new RefundResponse(

                booking.getReference(),

                booking.getStatus().name(),

                refundedAmount,

                booking.getCurrency(),

                paymentReference,

                mayaRefundService.calculateBalance(updatedLedger));

    }



    private Booking lockBooking(Long bookingId) {

        bookingRepository.findByIdForUpdate(bookingId)

                .orElseThrow(() -> new BusinessException("Booking not found"));

        return bookingRepository.findByIdWithDetails(bookingId)

                .orElseThrow(() -> new BusinessException("Booking not found"));

    }



    private void validateMayaRefundBooking(Booking booking) {

        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {

            throw new BusinessException("Refunds are only supported for online Maya payments");

        }

        if (booking.getCheckedOutAt() != null) {

            throw new BusinessException("Cannot refund a booking that has already checked out");

        }

    }



    private List<BookingLedger> loadLedger(Long bookingId) {

        return bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);

    }



    private BigDecimal resolveRefundCap(

            Booking booking, List<BookingLedger> ledgerEntries, BigDecimal mayaRefundable) {

        if (booking.getStatus() == BookingStatus.CANCELLED) {

            return mayaRefundService.remainingRefundDue(booking, ledgerEntries);

        }

        return mayaRefundable;

    }



    public BigDecimal refundableAmount(List<BookingLedger> entries) {

        return mayaRefundService.refundableAmount(entries);

    }



    public MayaRefundTiming assessMayaTiming(Booking booking, List<BookingLedger> entries, BigDecimal refundAmount) {

        BigDecimal mayaRefundable = mayaRefundService.mayaRefundableAmount(entries);

        BigDecimal amount = refundAmount != null ? refundAmount : resolveRefundCap(booking, entries, mayaRefundable);

        if (amount.compareTo(BigDecimal.ZERO) <= 0 || mayaRefundable.compareTo(BigDecimal.ZERO) <= 0) {

            return new MayaRefundTiming(

                    false,

                    "Nothing remaining to refund via Maya on this booking",

                    null,

                    null);

        }

        BigDecimal mayaAttempt = amount.min(mayaRefundable);

        return mayaRefundService.assessRefundTiming(entries, mayaAttempt, mayaRefundable);

    }

}


