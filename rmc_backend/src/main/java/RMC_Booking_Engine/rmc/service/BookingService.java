package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.entity.Guest;
import RMC_Booking_Engine.rmc.domain.entity.InventoryHold;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.BookingResponse;
import RMC_Booking_Engine.rmc.dto.BookingStatusResponse;
import RMC_Booking_Engine.rmc.dto.CreateBookingRequest;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutCreated;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.GuestRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    private final AvailabilityService availabilityService;
    private final PricingService pricingService;
    private final ConfigService configService;
    private final BookingReferenceGenerator referenceGenerator;
    private final MayaCheckoutClient mayaCheckoutClient;
    private final MayaPaymentService mayaPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefundPolicyService refundPolicyService;
    private final MayaRefundService mayaRefundService;
    private final BookingHoldService bookingHoldService;
    private final RoomCatalogMapper roomCatalogMapper;

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

        RatePlan ratePlan = ratePlanRepository.findById(request.ratePlanId())
                .orElseThrow(() -> new BusinessException("Rate plan not found"));

        if (!ratePlan.getRoomType().getId().equals(roomType.getId())) {
            throw new BusinessException("Rate plan does not match room type");
        }

        availabilityService.assertAvailable(roomType, request.checkIn(), request.checkOut());
        List<NightlyRateDto> breakdown = pricingService.calculateStayPricing(
                ratePlan.getId(), request.checkIn(), request.checkOut());
        BigDecimal quotedTotal = pricingService.sumTaxInclusive(breakdown);

        Instant now = Instant.now();
        Guest guest = new Guest();
        guest.setEmail(request.email().trim().toLowerCase());
        guest.setFullName(request.fullName().trim());
        guest.setPhone(request.phone().trim());
        guest.setConsentTimestamp(now);
        guest.setDpaConsentVersion(configService.getDpaConsentVersion());
        guest.setAgeConfirmedAt(now);
        guest = guestRepository.save(guest);

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

        if (paymentMethod == PaymentMethod.ONLINE_MAYA) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
        } else {
            booking.setStatus(BookingStatus.CONFIRMED_PAY_LATER);
            booking.setPaymentMethod(PaymentMethod.PAY_AT_HOTEL);
        }

        booking = bookingRepository.save(booking);

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
        } else {
            writeAuditLog(booking, null, BookingStatus.CONFIRMED_PAY_LATER.name(), "GUEST_BOOKING", null, null);
            eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
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

        if (booking.getStatus() != BookingStatus.CONFIRMED_PAY_LATER
                && booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("This booking cannot be cancelled");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && !refundPolicyService.isWithinRefundWindow(booking)) {
            throw new BusinessException("Cancellation window has passed for this booking");
        }

        BookingStatus previous = booking.getStatus();
        String auditTrigger = "GUEST_CANCEL";

        if (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA
                && booking.getStatus() == BookingStatus.CONFIRMED) {
            mayaRefundService.executeRefund(booking, null, "Guest cancellation");
            auditTrigger = "GUEST_CANCEL_REFUND";
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        bookingHoldService.releaseActiveHolds(booking);
        writeAuditLog(booking, previous.name(), BookingStatus.CANCELLED.name(), auditTrigger, null, null);

        List<NightlyRateDto> breakdown = pricingService.calculateStayPricing(
                booking.getRatePlan().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate());
        return toResponse(booking, breakdown, null);
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
                roomCatalogMapper.toCard(booking.getRoomType()));
    }
}
