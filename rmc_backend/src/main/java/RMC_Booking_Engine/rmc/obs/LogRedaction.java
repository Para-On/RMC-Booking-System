package RMC_Booking_Engine.rmc.obs;

import java.util.regex.Pattern;

/**
 * Strips secrets, PAN-like digits, and payment tokens from log text
 * ({@code RMC-SPEC-SEC-001.2}, {@code RMC-SPEC-OBS-001.1}).
 */
public final class LogRedaction {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern MAYA_KEY = Pattern.compile("(?i)\\b[ps]k-[A-Za-z0-9]{8,}");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._\\-+/=]+");
    private static final Pattern BASIC = Pattern.compile("(?i)\\bBasic\\s+[A-Za-z0-9+/=]{8,}");
    private static final Pattern NAMED_SECRET = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[_-]?key|authorization)\\s*[=:]\\s*\\S+");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CVV = Pattern.compile("(?i)\\bcvv[:\\s-]*\\d{3,4}\\b");
    private static final Pattern PAN = Pattern.compile("(?<!\\d)(?:\\d[ -]*?){13,19}(?!\\d)");

    private LogRedaction() {}

    public static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String redacted = MAYA_KEY.matcher(value).replaceAll(REDACTED);
        redacted = BEARER.matcher(redacted).replaceAll("Bearer " + REDACTED);
        redacted = BASIC.matcher(redacted).replaceAll("Basic " + REDACTED);
        redacted = NAMED_SECRET.matcher(redacted).replaceAll("$1=" + REDACTED);
        redacted = EMAIL.matcher(redacted).replaceAll("[redacted-email]");
        redacted = CVV.matcher(redacted).replaceAll("cvv " + REDACTED);
        redacted = PAN.matcher(redacted).replaceAll(REDACTED);
        return redacted;
    }

    /** Copy of {@code ex} safe to attach to a log event (message redacted; original stack kept). */
    public static Exception forLogging(Throwable ex) {
        if (ex == null) {
            return new Exception("unknown failure");
        }
        Exception copy = new Exception(ex.getClass().getName() + ": " + redact(ex.getMessage()));
        copy.setStackTrace(ex.getStackTrace());
        return copy;
    }
}
