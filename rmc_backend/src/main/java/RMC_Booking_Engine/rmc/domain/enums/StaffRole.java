package RMC_Booking_Engine.rmc.domain.enums;

public enum StaffRole {
    FRONT_DESK,
    MANAGER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
