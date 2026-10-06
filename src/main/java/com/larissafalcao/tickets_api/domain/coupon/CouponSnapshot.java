package com.larissafalcao.tickets_api.domain.coupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponSnapshot(UUID id, String code, String description, BigDecimal discountValue,
                             Instant expirationDate, CouponStatus status, boolean published,
                             boolean redeemed, Instant deletedAt) {}
