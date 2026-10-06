package com.larissafalcao.tickets_api.domain.coupon;

public final class InvalidCouponException extends RuntimeException {
    private final String field;

    public InvalidCouponException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
