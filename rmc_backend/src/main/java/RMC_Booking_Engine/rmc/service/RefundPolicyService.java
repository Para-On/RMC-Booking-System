package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class RefundPolicyService {

    public boolean isWithinRefundWindow(Booking booking) {
        RatePlan plan = booking.getRatePlan();
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(
                Instant.now(),
                booking.getCheckInDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        return hoursUntilCheckIn >= plan.getRefundWindowHours();
    }
}
