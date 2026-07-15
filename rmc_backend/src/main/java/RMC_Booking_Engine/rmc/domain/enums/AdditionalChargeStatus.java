package RMC_Booking_Engine.rmc.domain.enums;

public enum AdditionalChargeStatus {
    /** Staff created; guest has not chosen how to pay yet. */
    AWAITING_PAYMENT,
    /** Guest chose Maya; waiting for payment confirmation. */
    PENDING_MAYA,
    /** Guest chose pay-at-hotel; waiting for staff approval. */
    PENDING_APPROVAL,
    /** Staff approved pay-at-hotel; debit on folio, awaiting collection. */
    APPROVED_UNPAID,
    PAID,
    REJECTED,
    CANCELLED
}
