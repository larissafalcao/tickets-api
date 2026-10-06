package com.larissafalcao.tickets_api.domain.coupon;

import java.time.Instant;

public record ExpirationDate(Instant value) {
    public ExpirationDate {
        if (value == null) {
            throw new InvalidCouponException("expirationDate", "Expiration date is required.");
        }
    }

    public void ensureNotPast(Instant now) {
        if (value.isBefore(now)) {
            throw new InvalidCouponException("expirationDate", "Expiration date must not be in the past.");
        }
    }
}
