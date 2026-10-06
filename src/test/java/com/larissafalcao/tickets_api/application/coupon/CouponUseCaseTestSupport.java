package com.larissafalcao.tickets_api.application.coupon;

import com.larissafalcao.tickets_api.application.port.CouponRepository;
import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import com.larissafalcao.tickets_api.domain.coupon.CouponStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CouponUseCaseTestSupport {
    static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    static CreateCouponCommand validCommand() {
        return new CreateCouponCommand("ABC-123", "Description", new BigDecimal("0.812345"),
                NOW.plusSeconds(60), true);
    }

    static Coupon validCoupon() {
        var command = validCommand();
        return Coupon.create(command.code(), command.description(), command.discountValue(),
                command.expirationDate(), command.published(), NOW);
    }

    static final class InMemoryCouponRepository implements CouponRepository {
        final Map<UUID, Coupon> coupons = new HashMap<>();

        @Override
        public Optional<Coupon> findNotDeletedById(UUID id) {
            return findByIdForDeletion(id).filter(coupon -> coupon.status() != CouponStatus.DELETED);
        }

        @Override
        public Optional<Coupon> findByIdForDeletion(UUID id) {
            return Optional.ofNullable(coupons.get(id));
        }

        @Override
        public void save(Coupon coupon) {
            coupons.put(coupon.id(), coupon);
        }
    }
}
