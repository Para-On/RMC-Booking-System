package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MayaRefundServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");

    private MayaRefundService mayaRefundService;

    @BeforeEach
    void setUp() {
        mayaRefundService = new MayaRefundService(null, null);
    }

    @Test
    void refundableAmount_subtractsExistingRefunds() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "100.00", LocalDate.now(ZONE));
        BookingLedger refund = ledger(LedgerEntryType.REFUND, "40.00", LocalDate.now(ZONE));

        BigDecimal refundable = mayaRefundService.refundableAmount(List.of(credit, refund));

        assertThat(refundable).isEqualByComparingTo("60.00");
    }

    @Test
    void refundableAmount_neverNegative() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "50.00", LocalDate.now(ZONE));
        BookingLedger refund = ledger(LedgerEntryType.REFUND, "80.00", LocalDate.now(ZONE));

        BigDecimal refundable = mayaRefundService.refundableAmount(List.of(credit, refund));

        assertThat(refundable).isEqualByComparingTo("0");
    }

    @Test
    void remainingRefundDue_countsManualAndMayaRefunds() {
        Booking booking = new Booking();
        booking.setRefundEligibleAmount(new BigDecimal("2128.00"));

        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "4256.00", LocalDate.now(ZONE).minusDays(1));
        BookingLedger manual = ledger(LedgerEntryType.MANUAL_REFUND, "1000.00", LocalDate.now(ZONE));
        BookingLedger maya = ledger(LedgerEntryType.REFUND, "128.00", LocalDate.now(ZONE));

        BigDecimal remaining = mayaRefundService.remainingRefundDue(
                booking, List.of(credit, manual, maya));

        assertThat(remaining).isEqualByComparingTo("1000.00");
    }

    @Test
    void assessRefundTiming_blocksPartialRefundOnSameDay() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "4256.00", LocalDate.now(ZONE));

        var timing = mayaRefundService.assessRefundTiming(
                List.of(credit), new BigDecimal("2128.00"), new BigDecimal("4256.00"));

        assertThat(timing.canProcessNow()).isFalse();
        assertThat(timing.blockedReason()).contains("Partial Maya refunds are not available");
        assertThat(timing.availableAt()).isNotNull();
    }

    @Test
    void assessRefundTiming_allowsFullVoidSameDay() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "4256.00", LocalDate.now(ZONE));

        var timing = mayaRefundService.assessRefundTiming(
                List.of(credit), new BigDecimal("4256.00"), new BigDecimal("4256.00"));

        assertThat(timing.canProcessNow()).isTrue();
        assertThat(timing.methodWhenAllowed())
                .isEqualTo(MayaRefundService.ReversalMethod.VOID);
    }

    @Test
    void assessRefundTiming_allowsPartialRefundNextDay() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "4256.00", LocalDate.now(ZONE).minusDays(1));

        var timing = mayaRefundService.assessRefundTiming(
                List.of(credit), new BigDecimal("2128.00"), new BigDecimal("4256.00"));

        assertThat(timing.canProcessNow()).isTrue();
        assertThat(timing.methodWhenAllowed())
                .isEqualTo(MayaRefundService.ReversalMethod.REFUND);
    }

    private BookingLedger ledger(LedgerEntryType type, String amount, LocalDate date) {
        BookingLedger entry = new BookingLedger();
        entry.setEntryType(type);
        entry.setAmount(new BigDecimal(amount));
        entry.setCreatedAt(date.atStartOfDay(ZONE).plusHours(10).toInstant());
        return entry;
    }
}
