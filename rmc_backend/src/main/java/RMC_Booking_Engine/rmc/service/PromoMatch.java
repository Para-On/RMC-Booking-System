package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Promo;
import java.math.BigDecimal;

public record PromoMatch(Promo promo, BigDecimal amountOff) {}
