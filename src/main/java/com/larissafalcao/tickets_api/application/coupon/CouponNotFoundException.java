package com.larissafalcao.tickets_api.application.coupon;

import java.util.UUID;

public final class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(UUID id) {
        super("Coupon " + id + " was not found.");
    }
}
