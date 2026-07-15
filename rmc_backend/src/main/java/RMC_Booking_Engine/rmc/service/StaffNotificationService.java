package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.StaffNotification;
import RMC_Booking_Engine.rmc.domain.entity.StaffNotificationRead;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.StaffNotificationType;
import RMC_Booking_Engine.rmc.dto.StaffNotificationDto;
import RMC_Booking_Engine.rmc.dto.StaffNotificationsResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.StaffNotificationReadRepository;
import RMC_Booking_Engine.rmc.repository.StaffNotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffNotificationService {

    private static final int LIST_LIMIT = 40;

    private final StaffNotificationRepository notificationRepository;
    private final StaffNotificationReadRepository readRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public StaffNotificationsResponse listForStaff(Long staffUserId) {
        List<StaffNotification> notifications =
                notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, LIST_LIMIT));
        Set<Long> readIds = readRepository.findReadNotificationIdsByStaffUserId(staffUserId);
        long unreadCount = notificationRepository.countUnreadForStaff(staffUserId);

        List<StaffNotificationDto> items = notifications.stream()
                .map(notification -> toDto(notification, !readIds.contains(notification.getId())))
                .toList();

        return new StaffNotificationsResponse(items, unreadCount);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long staffUserId) {
        return notificationRepository.countUnreadForStaff(staffUserId);
    }

    @Transactional
    public StaffNotificationsResponse markAllSeen(Long staffUserId) {
        List<Long> unreadIds = notificationRepository.findUnreadIdsForStaff(staffUserId);
        if (!unreadIds.isEmpty()) {
            Instant now = Instant.now();
            List<StaffNotificationRead> reads = unreadIds.stream()
                    .map(id -> {
                        StaffNotificationRead read = new StaffNotificationRead();
                        read.setStaffUserId(staffUserId);
                        read.setNotificationId(id);
                        read.setReadAt(now);
                        return read;
                    })
                    .toList();
            readRepository.saveAll(reads);
        }
        return listForStaff(staffUserId);
    }

    /**
     * REQUIRES_NEW is required: these methods are called from {@code @TransactionalEventListener(AFTER_COMMIT)}.
     * Default REQUIRED would join the already-completed transaction and discard the insert.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyBookingAwaitingApproval(Long bookingId) {
        Booking booking = bookingRepository
                .findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        String guestName = booking.getGuest().getFullName();
        String reference = booking.getReference();
        String roomName = booking.getRoomType().getName();

        StaffNotification notification = new StaffNotification();
        notification.setType(StaffNotificationType.BOOKING_RECEIVED);
        notification.setBookingId(bookingId);
        notification.setLinkPath("/staff/bookings/" + bookingId);
        if (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA) {
            notification.setType(StaffNotificationType.BOOKING_PAYMENT_RECEIVED);
            notification.setTitle("Maya payment confirmed - approve booking");
            notification.setMessage(
                    reference + " · " + guestName + " · " + roomName
                            + " · payment confirmed; booking awaits approval");
        } else {
            notification.setTitle("Pay-at-hotel booking awaits approval");
            notification.setMessage(
                    reference + " · " + guestName + " · " + roomName + " · approve to confirm");
        }
        notificationRepository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyBookingReceived(Long bookingId, boolean pendingPayment) {
        Booking booking = bookingRepository
                .findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        String guestName = booking.getGuest().getFullName();
        String reference = booking.getReference();
        String roomName = booking.getRoomType().getName();

        StaffNotification notification = new StaffNotification();
        notification.setType(StaffNotificationType.BOOKING_RECEIVED);
        notification.setBookingId(bookingId);
        notification.setLinkPath("/staff/bookings/" + bookingId);

        if (pendingPayment) {
            notification.setTitle("New booking awaiting payment");
            notification.setMessage(reference + " · " + guestName + " · " + roomName + " · online payment pending");
        } else {
            notification.setTitle("New booking received");
            notification.setMessage(reference + " · " + guestName + " · " + roomName);
        }

        notificationRepository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyBookingPaymentReceived(Long bookingId) {
        Booking booking = bookingRepository
                .findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA) {
            return;
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            return;
        }

        String guestName = booking.getGuest().getFullName();
        String reference = booking.getReference();

        StaffNotification notification = new StaffNotification();
        notification.setType(StaffNotificationType.BOOKING_PAYMENT_RECEIVED);
        notification.setTitle("Booking payment confirmed");
        notification.setMessage(reference + " · " + guestName + " · Maya payment confirmed");
        notification.setBookingId(bookingId);
        notification.setLinkPath("/staff/bookings/" + bookingId);
        notificationRepository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyBookingCancelled(Long bookingId, boolean refundPending) {
        Booking booking = bookingRepository
                .findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        String guestName = booking.getGuest().getFullName();
        String reference = booking.getReference();
        String roomName = booking.getRoomType().getName();
        String paymentLabel = booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA
                ? "Maya"
                : "pay-later";

        StaffNotification notification = new StaffNotification();
        notification.setType(StaffNotificationType.BOOKING_CANCELLED);
        notification.setBookingId(bookingId);
        notification.setLinkPath("/staff/bookings/" + bookingId);
        if (refundPending) {
            notification.setTitle("Booking cancelled — refund pending");
            notification.setMessage(
                    reference + " · " + guestName + " · " + roomName + " · " + paymentLabel
                            + " · complete the refund");
        } else {
            notification.setTitle("Booking cancelled");
            notification.setMessage(
                    reference + " · " + guestName + " · " + roomName + " · " + paymentLabel);
        }
        notificationRepository.save(notification);
    }

    private StaffNotificationDto toDto(StaffNotification notification, boolean unread) {
        return new StaffNotificationDto(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getBookingId(),
                notification.getLinkPath(),
                notification.getCreatedAt(),
                unread);
    }
}
