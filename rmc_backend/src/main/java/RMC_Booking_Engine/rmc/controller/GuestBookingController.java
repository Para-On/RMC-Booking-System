package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.AdditionalChargeDto;
import RMC_Booking_Engine.rmc.dto.AvailabilityResponse;
import RMC_Booking_Engine.rmc.dto.BookingResponse;
import RMC_Booking_Engine.rmc.dto.BookingStatusResponse;
import RMC_Booking_Engine.rmc.dto.CreateBookingRequest;
import RMC_Booking_Engine.rmc.dto.PayAdditionalChargeRequest;
import RMC_Booking_Engine.rmc.dto.RoomCatalogResponse;
import RMC_Booking_Engine.rmc.dto.StayAvailabilityCheckResponse;
import RMC_Booking_Engine.rmc.dto.PricingPolicyResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.service.AdditionalChargeService;
import RMC_Booking_Engine.rmc.service.AvailabilityService;
import RMC_Booking_Engine.rmc.service.BookingService;
import RMC_Booking_Engine.rmc.service.ConfigService;
import RMC_Booking_Engine.rmc.service.GuestRoomCatalogService;
import RMC_Booking_Engine.rmc.service.MayaPaymentService;
import RMC_Booking_Engine.rmc.service.PromoCodeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestBookingController {

    private final AvailabilityService availabilityService;
    private final GuestRoomCatalogService guestRoomCatalogService;
    private final BookingService bookingService;
    private final MayaPaymentService mayaPaymentService;
    private final ConfigService configService;
    private final AdditionalChargeService additionalChargeService;
    private final PromoCodeService promoCodeService;

    @GetMapping("/pricing-policy")
    public PricingPolicyResponse getPricingPolicy() {
        return configService.getPricingPolicy();
    }

    @GetMapping("/room-catalog")
    public RoomCatalogResponse listRoomCatalog() {
        return new RoomCatalogResponse(guestRoomCatalogService.listActiveCatalog());
    }

    @GetMapping("/availability")
    public AvailabilityResponse searchAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String promoType,
            @RequestParam(required = false) String offerCode,
            @RequestParam(required = false) String organizationCode) {
        String promoCodeError = null;
        boolean codesPresent = (offerCode != null && !offerCode.isBlank())
                || (organizationCode != null && !organizationCode.isBlank())
                || (promoType != null && !promoType.isBlank());
        if (codesPresent) {
            try {
                promoCodeService.assertGuestCodesPresentAndValid(promoType, offerCode, organizationCode);
            } catch (BusinessException ex) {
                promoCodeError = ex.getMessage();
            }
        }
        return new AvailabilityResponse(
                availabilityService.search(
                        checkIn,
                        checkOut,
                        roomTypeId,
                        promoCodeError == null ? promoType : null,
                        promoCodeError == null ? offerCode : null,
                        promoCodeError == null ? organizationCode : null),
                promoCodeError);
    }

    @GetMapping("/availability/check")
    public StayAvailabilityCheckResponse checkStayAvailability(
            @RequestParam Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long ratePlanId,
            @RequestParam(required = false) String promoType,
            @RequestParam(required = false) String offerCode,
            @RequestParam(required = false) String organizationCode) {
        return availabilityService.checkStay(
                roomTypeId, checkIn, checkOut, ratePlanId, promoType, offerCode, organizationCode);
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @GetMapping("/bookings/{reference}")
    public BookingResponse getBooking(
            @PathVariable String reference,
            @RequestParam String email) {
        return bookingService.getBooking(reference, email);
    }

    @GetMapping("/bookings/{reference}/status")
    public BookingStatusResponse getStatus(@PathVariable String reference) {
        return bookingService.getStatus(reference);
    }

    @PostMapping("/bookings/{reference}/confirm-payment")
    public BookingStatusResponse confirmPayment(@PathVariable String reference) {
        mayaPaymentService.confirmPaymentByReference(reference);
        return bookingService.getStatus(reference);
    }

    @PostMapping("/bookings/{reference}/charges/{chargeId}/pay")
    public AdditionalChargeDto payAdditionalCharge(
            @PathVariable String reference,
            @PathVariable Long chargeId,
            @Valid @RequestBody PayAdditionalChargeRequest request) {
        return additionalChargeService.guestPay(
                reference, chargeId, request.email(), request.paymentMethod());
    }

    @PostMapping("/bookings/{reference}/charges/{chargeId}/confirm-payment")
    public AdditionalChargeDto confirmChargePayment(
            @PathVariable String reference, @PathVariable Long chargeId) {
        additionalChargeService.confirmMayaPaymentByChargeId(chargeId);
        return additionalChargeService.getChargeDto(chargeId);
    }

    @PostMapping("/bookings/{reference}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable String reference,
            @RequestParam String email) {
        return bookingService.cancelBooking(reference, email);
    }
}
