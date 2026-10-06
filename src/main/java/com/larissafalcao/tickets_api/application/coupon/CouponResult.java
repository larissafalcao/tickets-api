package com.larissafalcao.tickets_api.application.coupon;

import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import com.larissafalcao.tickets_api.domain.coupon.CouponStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResult(UUID id, String code, String description, BigDecimal discountValue,
                           Instant expirationDate, CouponStatus status, boolean published, boolean redeemed) {
    public static CouponResult from(Coupon coupon) {
        return new CouponResult(coupon.id(), coupon.code(), coupon.description(), coupon.discountValue(),
                coupon.expirationDate(), coupon.status(), coupon.published(), coupon.redeemed());
    }
}
