package RMC_Booking_Engine.rmc.security;

public final class StaffNavPaths {

    public static final String DASHBOARD = "/staff/dashboard";
    public static final String ARRIVALS = "/staff/arrivals";
    public static final String BOOKINGS = "/staff/bookings";
    /** @deprecated Use {@link #BOOKINGS} */
    public static final String ARRIVALS_BOOKINGS = "/staff/bookings";
    public static final String GUESTS = "/staff/guests";
    public static final String ROOMS_CATALOG = "/staff/rooms/catalog";
    public static final String ROOMS_CONFIG = "/staff/rooms/config";
    public static final String ROOMS_OPERATIONS = "/staff/rooms/operations";
    public static final String ROOMS_EXTRAS = "/staff/rooms/extras";
    public static final String SETTINGS = "/staff/settings";
    public static final String SETTINGS_AUDIT = "/staff/settings/audit";
    public static final String SETTINGS_REFUND_POLICY = "/staff/settings/refund-policy";
    public static final String SETTINGS_PROMOS = "/staff/settings/promos";
    public static final String SETTINGS_PROMO_CODES = "/staff/settings/promo-codes";
    public static final String BRANDING = "/staff/branding";
    public static final String USERS = "/staff/users";
    public static final String MODULES = "/staff/modules";

    private StaffNavPaths() {
    }
}
