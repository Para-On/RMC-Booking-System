package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;

import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffRefundServiceTest {

    private StaffRefundService staffRefundService;

    @BeforeEach
    void setUp() {
        staffRefundService = new StaffRefundService(null, null, null, null, new MayaRefundService(null, null));
    }

    @Test
    void refundableAmount_delegatesToMayaRefundService() {
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "100.00");
        BigDecimal refundable = staffRefundService.refundableAmount(List.of(credit));
        assertThat(refundable).isEqualByComparingTo("100.00");
    }

    private BookingLedger ledger(LedgerEntryType type, String amount) {
        BookingLedger entry = new BookingLedger();
        entry.setEntryType(type);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }
}
