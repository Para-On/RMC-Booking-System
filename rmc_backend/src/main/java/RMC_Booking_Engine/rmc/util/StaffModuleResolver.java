package RMC_Booking_Engine.rmc.util;

public final class StaffModuleResolver {

    private StaffModuleResolver() {
    }

    public static StaffModuleInfo resolve(String requestUri) {
        if (requestUri == null) {
            return new StaffModuleInfo("system", "System");
        }
        if (requestUri.contains("/dashboard")) {
            return new StaffModuleInfo("dashboard", "Dashboard");
        }
        if (requestUri.contains("/arrivals")) {
            return new StaffModuleInfo("arrivals", "Arrivals");
        }
        if (requestUri.contains("/bookings")) {
            return new StaffModuleInfo("bookings", "Bookings");
        }
        if (requestUri.contains("/rooms/extras")) {
            return new StaffModuleInfo("rooms-extras", "Extras");
        }
        if (requestUri.contains("/rooms/catalog")) {
            return new StaffModuleInfo("rooms-catalog", "Create room");
        }
        if (requestUri.contains("/rooms/config")) {
            return new StaffModuleInfo("rooms-config", "Room configuration");
        }
        if (requestUri.contains("/rooms")) {
            return new StaffModuleInfo("rooms-operations", "View and update room");
        }
        if (requestUri.contains("/branding")) {
            return new StaffModuleInfo("branding", "Branding");
        }
        if (requestUri.contains("/config")) {
            return new StaffModuleInfo("settings", "Settings");
        }
        if (requestUri.contains("/users")) {
            return new StaffModuleInfo("users", "Staff users");
        }
        if (requestUri.contains("/nav")) {
            return new StaffModuleInfo("modules", "Sidebar modules");
        }
        if (requestUri.contains("/search")) {
            return new StaffModuleInfo("search", "Search");
        }
        if (requestUri.contains("/audit")) {
            return new StaffModuleInfo("settings-audit", "Audit logs");
        }
        return new StaffModuleInfo("system", "System");
    }
}
