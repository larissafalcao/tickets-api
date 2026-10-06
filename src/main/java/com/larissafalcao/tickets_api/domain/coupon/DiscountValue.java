package com.larissafalcao.tickets_api.domain.coupon;

import java.math.BigDecimal;

public record DiscountValue(BigDecimal value) {
    private static final BigDecimal MINIMUM = new BigDecimal("0.5");

    public DiscountValue {
        if (value == null) {
            throw new InvalidCouponException("discountValue", "Discount value is required.");
        }
        if (value.compareTo(MINIMUM) < 0) {
            throw new InvalidCouponException("discountValue", "Discount value must be at least 0.5.");
        }
    }
}
