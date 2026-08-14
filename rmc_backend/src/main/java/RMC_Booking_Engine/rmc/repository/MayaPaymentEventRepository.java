package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.MayaPaymentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MayaPaymentEventRepository extends JpaRepository<MayaPaymentEvent, Long> {

    List<MayaPaymentEvent> findByBookingReferenceOrderByReceivedAtAsc(String bookingReference);

    long countByPayloadSha256(String payloadSha256);
}
