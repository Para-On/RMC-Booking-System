package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaRefundResponse;
import RMC_Booking_Engine.rmc.dto.MayaVoidResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MayaRefundServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");

    @Mock
    private BookingLedgerRepository bookingLedgerRepository;
    @Mock
    private MayaCheckoutClient mayaCheckoutClient;

    private MayaRefundService mayaRefundService;

    @BeforeEach
    void setUp() {
        mayaRefundService = new MayaRefundService(bookingLedgerRepository, mayaCheckoutClient);
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

    @Test
    void reversePayment_blocksSameDayRefundWhenVoidUnavailable() {
        Booking booking = mayaBooking("1.29");
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "1.29", LocalDate.now(ZONE));
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(67L)).thenReturn(List.of(credit));
        when(mayaCheckoutClient.voidCheckout(eq("checkout-1"), anyString()))
                .thenThrow(new BusinessException(
                        "Unable to process Maya void: 400 — {\"error\":{\"code\":\"PY0045\","
                                + "\"message\":\"Payment is not available for void.\"}}"));

        assertThatThrownBy(() ->
                        mayaRefundService.reversePayment(booking, new BigDecimal("1.29"), "guest cancel", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("API refunds are only allowed after");
        verify(mayaCheckoutClient, never()).refundCheckout(anyString(), any(), anyString(), anyString());
    }

    @Test
    void reversePayment_fallsBackToRefundWhenVoidUnavailableNextDay() {
        Booking booking = mayaBooking("1.29");
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "1.29", LocalDate.now(ZONE).minusDays(1));
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(67L)).thenReturn(List.of(credit));
        // preferVoid true but not same day → goes straight to refund path without void
        when(mayaCheckoutClient.refundCheckout(eq("checkout-1"), eq(new BigDecimal("1.29")), eq("PHP"), anyString()))
                .thenReturn(new MayaRefundResponse("refund-99", "SUCCESS", "REFUND"));
        when(bookingLedgerRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(bookingLedgerRepository.save(any(BookingLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = mayaRefundService.reversePayment(booking, new BigDecimal("1.29"), "guest cancel", true);

        assertThat(result.method()).isEqualTo(MayaRefundService.ReversalMethod.REFUND);
        assertThat(result.mayaReferenceId()).isEqualTo("refund-99");
        verify(mayaCheckoutClient, never()).voidCheckout(anyString(), anyString());
    }

    @Test
    void reversePayment_mapsPy0047ToRetryMessage() {
        Booking booking = mayaBooking("1.29");
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "1.29", LocalDate.now(ZONE).minusDays(1));
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(67L)).thenReturn(List.of(credit));
        when(mayaCheckoutClient.refundCheckout(eq("checkout-1"), eq(new BigDecimal("1.29")), eq("PHP"), anyString()))
                .thenThrow(new BusinessException(
                        "Unable to process Maya refund: 401 — PY0047: Payment is ineligible for refund."));

        assertThatThrownBy(() ->
                        mayaRefundService.reversePayment(booking, new BigDecimal("1.29"), "guest cancel", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("API refunds are only allowed after");
    }

    @Test
    void reversePayment_doesNotFallbackWhenVoidFailsForOtherReason() {
        Booking booking = mayaBooking("1.29");
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "1.29", LocalDate.now(ZONE));
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(67L)).thenReturn(List.of(credit));
        when(mayaCheckoutClient.voidCheckout(eq("checkout-1"), anyString()))
                .thenThrow(new BusinessException("Unable to process Maya void: 401 — Unauthorized"));

        assertThatThrownBy(() ->
                        mayaRefundService.reversePayment(booking, new BigDecimal("1.29"), "guest cancel", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("401");
        verify(mayaCheckoutClient, never()).refundCheckout(anyString(), any(), anyString(), anyString());
    }

    @Test
    void reversePayment_usesVoidWhenMayaAccepts() {
        Booking booking = mayaBooking("1.29");
        BookingLedger credit = ledger(LedgerEntryType.CREDIT, "1.29", LocalDate.now(ZONE));
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(67L)).thenReturn(List.of(credit));
        when(mayaCheckoutClient.voidCheckout(eq("checkout-1"), anyString()))
                .thenReturn(new MayaVoidResponse("void-1", "SUCCESS", "VOIDED"));
        when(bookingLedgerRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(bookingLedgerRepository.save(any(BookingLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = mayaRefundService.reversePayment(booking, new BigDecimal("1.29"), "guest cancel", true);

        assertThat(result.method()).isEqualTo(MayaRefundService.ReversalMethod.VOID);
        verify(mayaCheckoutClient, never()).refundCheckout(anyString(), any(), anyString(), anyString());
    }

    @Test
    void isVoidUnavailable_detectsPy0045() {
        assertThat(MayaRefundService.isVoidUnavailable(
                        new BusinessException("Unable to process Maya void: 400 — PY0045: Payment is not available for void.")))
                .isTrue();
        assertThat(MayaRefundService.isVoidUnavailable(new BusinessException("Unable to process Maya void: 401 — Unauthorized")))
                .isFalse();
        assertThat(MayaRefundService.isVoidUnavailable(new BusinessException("Unable to process Maya refund: 400")))
                .isFalse();
    }

    @Test
    void isRefundNotYetEligible_detectsPy0047() {
        assertThat(MayaRefundService.isRefundNotYetEligible(
                        new BusinessException("Unable to process Maya refund: 401 — PY0047: Payment is ineligible for refund.")))
                .isTrue();
        assertThat(MayaRefundService.isRefundNotYetEligible(new BusinessException("Unable to process Maya refund: 400")))
                .isFalse();
    }

    private Booking mayaBooking(String amount) {
        Booking booking = new Booking();
        booking.setId(67L);
        booking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        booking.setMayaCheckoutId("checkout-1");
        booking.setCurrency("PHP");
        booking.setQuotedTotal(new BigDecimal(amount));
        return booking;
    }

    private BookingLedger ledger(LedgerEntryType type, String amount, LocalDate date) {
        BookingLedger entry = new BookingLedger();
        entry.setEntryType(type);
        entry.setAmount(new BigDecimal(amount));
        entry.setCreatedAt(date.atStartOfDay(ZONE).plusHours(10).toInstant());
        return entry;
    }
}
