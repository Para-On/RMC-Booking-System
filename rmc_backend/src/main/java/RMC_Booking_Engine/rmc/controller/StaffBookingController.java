package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.dto.ArrivalsResponse;
import RMC_Booking_Engine.rmc.dto.CheckInRequest;
import RMC_Booking_Engine.rmc.dto.CheckOutResponse;
import RMC_Booking_Engine.rmc.dto.OverrideRequest;
import RMC_Booking_Engine.rmc.dto.StaffBookingDetailResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffBookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final StaffBookingService staffBookingService;

    @GetMapping("/arrivals")
    public ArrivalsResponse arrivals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return staffBookingService.getArrivals(date);
    }

    @GetMapping("/bookings/{id}")
    public StaffBookingDetailResponse getBooking(@PathVariable Long id) {
        return staffBookingService.getBookingDetail(id);
    }

    @PostMapping("/bookings/{id}/check-in")
    public StaffBookingDetailResponse checkIn(
            @PathVariable Long id,
            @RequestBody(required = false) CheckInRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        Long roomUnitId = request != null ? request.roomUnitId() : null;
        return staffBookingService.checkIn(id, roomUnitId, staff);
    }

    @PostMapping("/bookings/{id}/check-out")
    public CheckOutResponse checkOut(
            @PathVariable Long id,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffBookingService.checkOut(id, staff);
    }

    @PostMapping("/bookings/{id}/override")
    public StaffBookingDetailResponse overrideStatus(
            @PathVariable Long id,
            @Valid @RequestBody OverrideRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        BookingStatus target = parseStatus(request.targetStatus());
        return staffBookingService.overrideStatus(id, target, request.reason().trim(), staff);
    }

    private BookingStatus parseStatus(String value) {
        try {
            return BookingStatus.valueOf(value);
        } catch (Exception ex) {
            throw new BusinessException("Invalid booking status");
        }
    }
}
