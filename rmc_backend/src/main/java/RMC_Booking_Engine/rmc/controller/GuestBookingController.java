package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.AvailabilityResponse;
import RMC_Booking_Engine.rmc.dto.BookingResponse;
import RMC_Booking_Engine.rmc.dto.BookingStatusResponse;
import RMC_Booking_Engine.rmc.dto.CreateBookingRequest;
import RMC_Booking_Engine.rmc.dto.RoomCatalogResponse;
import RMC_Booking_Engine.rmc.dto.StayAvailabilityCheckResponse;
import RMC_Booking_Engine.rmc.service.AvailabilityService;
import RMC_Booking_Engine.rmc.service.BookingService;
import RMC_Booking_Engine.rmc.service.GuestRoomCatalogService;
import RMC_Booking_Engine.rmc.service.MayaPaymentService;
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

    @GetMapping("/room-catalog")
    public RoomCatalogResponse listRoomCatalog() {
        return new RoomCatalogResponse(guestRoomCatalogService.listActiveCatalog());
    }

    @GetMapping("/availability")
    public AvailabilityResponse searchAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long roomTypeId) {
        return new AvailabilityResponse(
                availabilityService.search(checkIn, checkOut, roomTypeId));
    }

    @GetMapping("/availability/check")
    public StayAvailabilityCheckResponse checkStayAvailability(
            @RequestParam Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return availabilityService.checkStay(roomTypeId, checkIn, checkOut);
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

    @PostMapping("/bookings/{reference}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable String reference,
            @RequestParam String email) {
        return bookingService.cancelBooking(reference, email);
    }
}
