package com.larissafalcao.tickets_api.domain.coupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Coupon {
    private final UUID id;
    private final CouponCode code;
    private final String description;
    private final DiscountValue discountValue;
    private final ExpirationDate expirationDate;
    private final boolean published;
    private final boolean redeemed;
    private CouponStatus status;
    private Instant deletedAt;

    private Coupon(CouponSnapshot state) {
        this.id = Objects.requireNonNull(state.id());
        this.code = new CouponCode(state.code());
        if (state.description() == null || state.description().isBlank()) {
            throw new InvalidCouponException("description", "Description is required and must not be blank.");
        }
        this.description = state.description();
        this.discountValue = new DiscountValue(state.discountValue());
        this.expirationDate = new ExpirationDate(state.expirationDate());
        this.status = Objects.requireNonNull(state.status());
        this.published = state.published();
        this.redeemed = state.redeemed();
        this.deletedAt = state.deletedAt();
    }

    public static Coupon create(String code, String description, BigDecimal discountValue,
                                Instant expirationDate, Boolean published, Instant now) {
        Coupon coupon = new Coupon(new CouponSnapshot(UUID.randomUUID(), code, description, discountValue,
                expirationDate, CouponStatus.ACTIVE, Boolean.TRUE.equals(published), false, null));
        coupon.expirationDate.ensureNotPast(now);
        return coupon;
    }

    public static Coupon reconstitute(CouponSnapshot state) {
        return new Coupon(state);
    }

    public void delete(Instant now) {
        if (status == CouponStatus.DELETED) {
            throw new CouponAlreadyDeletedException(id);
        }
        deletedAt = Objects.requireNonNull(now);
        status = CouponStatus.DELETED;
    }

    public UUID id() { return id; }
    public String code() { return code.value(); }
    public String description() { return description; }
    public BigDecimal discountValue() { return discountValue.value(); }
    public Instant expirationDate() { return expirationDate.value(); }
    public CouponStatus status() { return status; }
    public boolean published() { return published; }
    public boolean redeemed() { return redeemed; }
    public Instant deletedAt() { return deletedAt; }
}
