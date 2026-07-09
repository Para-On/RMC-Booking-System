package RMC_Booking_Engine.rmc.domain.enums;

public enum CutoffUnit {
    HOURS,
    DAYS;

    public static CutoffUnit fromString(String value) {
        if (value == null || value.isBlank()) {
            return HOURS;
        }
        return CutoffUnit.valueOf(value.trim().toUpperCase());
    }
}
