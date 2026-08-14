package RMC_Booking_Engine.rmc.obs;

import org.slf4j.MDC;

/**
 * Per-request correlation for checkout → webhook/poll → booking logs
 * ({@code RMC-SPEC-ARCH-001.3}).
 */
public final class RequestCorrelation {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_BOOKING_REFERENCE = "bookingReference";

    private RequestCorrelation() {}

    public static boolean isValidId(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 8
                && trimmed.length() <= 128
                && trimmed.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '-' || ch == '_');
    }

    public static String currentId() {
        String id = MDC.get(MDC_CORRELATION_ID);
        return blankToDash(id);
    }

    public static String currentBookingReference() {
        return blankToDash(MDC.get(MDC_BOOKING_REFERENCE));
    }

    public static void setId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            MDC.remove(MDC_CORRELATION_ID);
        } else {
            MDC.put(MDC_CORRELATION_ID, correlationId.trim());
        }
    }

    public static void setBookingReference(String reference) {
        if (reference == null || reference.isBlank()) {
            MDC.remove(MDC_BOOKING_REFERENCE);
        } else {
            MDC.put(MDC_BOOKING_REFERENCE, reference.trim());
        }
    }

    public static void clear() {
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_BOOKING_REFERENCE);
    }

    public static String describe() {
        return "correlationId=" + currentId() + " bookingReference=" + currentBookingReference();
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
