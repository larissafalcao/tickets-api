package com.larissafalcao.tickets_api.application.coupon;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponCommand(String code, String description, BigDecimal discountValue,
                                  Instant expirationDate, Boolean published) {}
