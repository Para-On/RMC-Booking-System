package RMC_Booking_Engine.rmc.domain.enums;

public enum EmailKind {
    /** Immediate guest email after booking create (includes booking reference). */
    BOOKING_RECEIVED,
    CONFIRMATION,
    REFUND_PROCESSED,
    BOOKING_REJECTED
}
