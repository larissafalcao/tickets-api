package com.larissafalcao.tickets_api.infrastructure.persistence;

import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import com.larissafalcao.tickets_api.domain.coupon.CouponSnapshot;
import com.larissafalcao.tickets_api.domain.coupon.CouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons")
class JpaCouponEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "discount_value", nullable = false, columnDefinition = "numeric")
    private BigDecimal discountValue;

    @Column(name = "expiration_date", nullable = false)
    private Instant expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CouponStatus status;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean redeemed;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected JpaCouponEntity() {}

    static JpaCouponEntity from(Coupon coupon) {
        JpaCouponEntity entity = new JpaCouponEntity();
        entity.id = coupon.id();
        entity.code = coupon.code();
        entity.description = coupon.description();
        entity.discountValue = coupon.discountValue();
        entity.expirationDate = coupon.expirationDate();
        entity.status = coupon.status();
        entity.published = coupon.published();
        entity.redeemed = coupon.redeemed();
        entity.deletedAt = coupon.deletedAt();
        return entity;
    }

    Coupon toDomain() {
        return Coupon.reconstitute(new CouponSnapshot(id, code, description, discountValue, expirationDate,
                status, published, redeemed, deletedAt));
    }
}
