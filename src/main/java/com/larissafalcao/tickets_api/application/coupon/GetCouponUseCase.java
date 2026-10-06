package com.larissafalcao.tickets_api.application.coupon;

import com.larissafalcao.tickets_api.application.port.CouponRepository;
import java.util.UUID;

public final class GetCouponUseCase {
    private final CouponRepository repository;

    public GetCouponUseCase(CouponRepository repository) {
        this.repository = repository;
    }

    public CouponResult execute(UUID id) {
        return repository.findNotDeletedById(id).map(CouponResult::from)
                .orElseThrow(() -> new CouponNotFoundException(id));
    }
}
