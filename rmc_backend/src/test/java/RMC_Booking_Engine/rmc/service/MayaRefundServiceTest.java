package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;

import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MayaRefundServiceTest {

    private MayaRefundService mayaRefundService;

    @BeforeEach
    void setUp() {
        mayaRefundService = new MayaRefundService(null, null);
    }

    @Test
    void refundableAmount_subtractsExistingRefunds() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "100.00");
        BookingLedger refund = ledger(LedgerEntryType.REFUND, "40.00");

        BigDecimal refundable = mayaRefundService.refundableAmount(List.of(credit, refund));

        assertThat(refundable).isEqualByComparingTo("60.00");
    }

    @Test
    void refundableAmount_neverNegative() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "50.00");
        BookingLedger refund = ledger(LedgerEntryType.REFUND, "80.00");

        BigDecimal refundable = mayaRefundService.refundableAmount(List.of(credit, refund));

        assertThat(refundable).isEqualByComparingTo("0");
    }

    private BookingLedger ledger(LedgerEntryType type, String amount) {
        BookingLedger entry = new BookingLedger();
        entry.setEntryType(type);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }
}
