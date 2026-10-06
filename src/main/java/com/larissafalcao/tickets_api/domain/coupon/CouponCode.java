package com.larissafalcao.tickets_api.domain.coupon;

public record CouponCode(String value) {
    public CouponCode {
        if (value == null) {
            throw new InvalidCouponException("code", "Coupon code is required.");
        }
        value = value.replaceAll("[^A-Za-z0-9]", "");
        if (value.length() != 6) {
            throw new InvalidCouponException("code", "Coupon code must contain exactly six alphanumeric characters.");
        }
    }
}
