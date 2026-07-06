package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.RefundResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
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

    @Transactional
    public RefundResponse processRefund(Long bookingId, BigDecimal requestedAmount, String reason, StaffPrincipal staff) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {
            throw new BusinessException("Refunds are only supported for online Maya payments");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed bookings can be refunded");
        }
        if (booking.getCheckedOutAt() != null) {
            throw new BusinessException("Cannot refund a booking that has already checked out");
        }
        if (!refundPolicyService.isWithinRefundWindow(booking)) {
            throw new BusinessException("Refund window has passed for this booking");
        }

        List<BookingLedger> ledgerEntries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal refundableBefore = mayaRefundService.refundableAmount(ledgerEntries);

        RefundExecution refund = mayaRefundService.executeRefund(booking, requestedAmount, reason);

        boolean fullRefund = refund.amount().compareTo(refundableBefore) >= 0;

        if (fullRefund) {
            BookingStatus previous = booking.getStatus();
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            bookingHoldService.releaseActiveHolds(booking);
            bookingHoldService.writeAuditLog(
                    booking, previous.name(), BookingStatus.CANCELLED.name(),
                    "STAFF_REFUND", staff.id(), reason.trim());
        } else {
            bookingHoldService.writeAuditLog(
                    booking, booking.getStatus().name(), booking.getStatus().name(),
                    "STAFF_PARTIAL_REFUND", staff.id(), reason.trim());
        }

        log.info("Refund {} for booking {} by staff {}", refund.amount(), booking.getReference(), staff.email());

        List<BookingLedger> updatedLedger =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        return new RefundResponse(
                booking.getReference(),
                booking.getStatus().name(),
                refund.amount(),
                booking.getCurrency(),
                refund.mayaRefundId(),
                mayaRefundService.calculateBalance(updatedLedger));
    }

    public BigDecimal refundableAmount(List<BookingLedger> entries) {
        return mayaRefundService.refundableAmount(entries);
    }
}
