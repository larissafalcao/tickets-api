package com.larissafalcao.tickets_api.application.coupon;

import com.larissafalcao.tickets_api.application.port.CouponRepository;
import com.larissafalcao.tickets_api.application.port.TimeProvider;
import com.larissafalcao.tickets_api.domain.coupon.Coupon;

public final class CreateCouponUseCase {
    private final CouponRepository repository;
    private final TimeProvider time;

    public CreateCouponUseCase(CouponRepository repository, TimeProvider time) {
        this.repository = repository;
        this.time = time;
    }

    public CouponResult execute(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(command.code(), command.description(), command.discountValue(),
                command.expirationDate(), command.published(), time.now());
        repository.save(coupon);
        return CouponResult.from(coupon);
    }
}
