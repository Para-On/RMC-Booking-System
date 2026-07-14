package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.dto.ArrivalsResponse;
import RMC_Booking_Engine.rmc.dto.BookingListResponse;
import RMC_Booking_Engine.rmc.dto.CheckInRequest;
import RMC_Booking_Engine.rmc.dto.CheckOutResponse;
import RMC_Booking_Engine.rmc.dto.OverrideRequest;
import RMC_Booking_Engine.rmc.dto.ManualRefundRequest;
import RMC_Booking_Engine.rmc.dto.RefundRequest;
import RMC_Booking_Engine.rmc.dto.RefundResponse;
import RMC_Booking_Engine.rmc.dto.RejectBookingRequest;
import RMC_Booking_Engine.rmc.dto.StaffBookingDetailResponse;
import RMC_Booking_Engine.rmc.dto.TransferRoomRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.security.RequireArrivalsAccess;
import RMC_Booking_Engine.rmc.security.RequireArrivalsOrRoomsOpsAccess;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.service.StaffBookingService;
import RMC_Booking_Engine.rmc.service.StaffRefundService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final StaffBookingService staffBookingService;
    private final StaffRefundService staffRefundService;

    @RequireArrivalsAccess
    @GetMapping("/arrivals")
    public ArrivalsResponse arrivals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return staffBookingService.getArrivals(date);
    }

    @RequireArrivalsAccess
    @GetMapping("/bookings")
    public BookingListResponse listBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        BookingStatus bookingStatus = null;
        if (status != null && !status.isBlank()) {
            bookingStatus = parseStatus(status.trim());
        }
        return staffBookingService.listBookings(bookingStatus, q, page, size);
    }

    @RequireArrivalsOrRoomsOpsAccess
    @GetMapping("/bookings/{id}")
    public StaffBookingDetailResponse getBooking(@PathVariable Long id) {
        return staffBookingService.getBookingDetail(id);
    }

    @RequireArrivalsAccess
    @PostMapping("/bookings/{id}/check-in")
    public StaffBookingDetailResponse checkIn(
            @PathVariable Long id,
            @RequestBody(required = false) CheckInRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        Long roomUnitId = request != null ? request.roomUnitId() : null;
        return staffBookingService.checkIn(id, roomUnitId, staff);
    }

    @RequireArrivalsOrRoomsOpsAccess
    @PostMapping("/bookings/{id}/check-out")
    public CheckOutResponse checkOut(
            @PathVariable Long id,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffBookingService.checkOut(id, staff);
    }

    @RequireArrivalsOrRoomsOpsAccess
    @PostMapping("/bookings/{id}/transfer-room")
    public StaffBookingDetailResponse transferRoom(
            @PathVariable Long id,
            @Valid @RequestBody TransferRoomRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffBookingService.transferRoom(id, request.roomUnitId(), request.reason(), staff);
    }

    @RequireArrivalsAccess
    @PostMapping("/bookings/{id}/approve")
    public StaffBookingDetailResponse approvePayLater(
            @PathVariable Long id, @AuthenticationPrincipal StaffPrincipal staff) {
        return staffBookingService.approvePayLater(id, staff);
    }

    @RequireArrivalsAccess
    @PostMapping("/bookings/{id}/reject")
    public StaffBookingDetailResponse rejectPayLater(
            @PathVariable Long id,
            @RequestBody(required = false) RejectBookingRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        String reason = request != null ? request.reason() : null;
        return staffBookingService.rejectPayLater(id, reason, staff);
    }

    @RequireArrivalsAccess
    @RequestMapping(value = "/bookings/{id}/override", method = RequestMethod.POST)
    public StaffBookingDetailResponse overrideStatus(
            @PathVariable Long id,
            @Valid @RequestBody OverrideRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        BookingStatus target = parseStatus(request.targetStatus());
        return staffBookingService.overrideStatus(id, target, request.reason().trim(), staff);
    }

    @PostMapping("/bookings/{id}/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public RefundResponse refundBooking(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffRefundService.processRefund(id, request.amount(), request.reason(), staff);
    }

    @PostMapping("/bookings/{id}/manual-refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public RefundResponse manualRefundBooking(
            @PathVariable Long id,
            @Valid @RequestBody ManualRefundRequest request,
            @AuthenticationPrincipal StaffPrincipal staff) {
        return staffRefundService.processManualRefund(
                id,
                request.amount(),
                request.reason(),
                request.externalReference(),
                request.method(),
                staff);
    }

    private BookingStatus parseStatus(String value) {
        try {
            return BookingStatus.valueOf(value);
        } catch (Exception ex) {
            throw new BusinessException("Invalid booking status");
        }
    }
}
