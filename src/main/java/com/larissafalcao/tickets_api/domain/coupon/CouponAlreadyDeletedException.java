package com.larissafalcao.tickets_api.domain.coupon;

import java.util.UUID;

public final class CouponAlreadyDeletedException extends RuntimeException {
    public CouponAlreadyDeletedException(UUID id) {
        super("Coupon " + id + " has already been deleted.");
    }
}
