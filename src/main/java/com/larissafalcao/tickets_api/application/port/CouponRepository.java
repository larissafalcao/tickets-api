package com.larissafalcao.tickets_api.application.port;

import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository {
    Optional<Coupon> findNotDeletedById(UUID id);

    Optional<Coupon> findByIdForDeletion(UUID id);

    void save(Coupon coupon);
}
