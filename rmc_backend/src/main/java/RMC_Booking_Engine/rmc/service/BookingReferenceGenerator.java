package RMC_Booking_Engine.rmc.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class BookingReferenceGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(LocalDate checkIn) {
        String datePart = checkIn.format(DATE_FMT);
        String randomPart = String.format("%04d", RANDOM.nextInt(10000));
        return "RMC-" + datePart + "-" + randomPart;
    }
}
