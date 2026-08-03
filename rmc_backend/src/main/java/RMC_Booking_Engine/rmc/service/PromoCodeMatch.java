package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.PromoCode;
import java.math.BigDecimal;

public record PromoCodeMatch(PromoCode promoCode, BigDecimal amountOff) {}
