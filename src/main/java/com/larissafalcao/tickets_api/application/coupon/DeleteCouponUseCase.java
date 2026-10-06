package com.larissafalcao.tickets_api.application.coupon;

import com.larissafalcao.tickets_api.application.port.CouponRepository;
import com.larissafalcao.tickets_api.application.port.TimeProvider;
import com.larissafalcao.tickets_api.application.port.TransactionRunner;
import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import java.util.UUID;

public final class DeleteCouponUseCase {
    private final CouponRepository repository;
    private final TimeProvider time;
    private final TransactionRunner transactions;

    public DeleteCouponUseCase(CouponRepository repository, TimeProvider time, TransactionRunner transactions) {
        this.repository = repository;
        this.time = time;
        this.transactions = transactions;
    }

    public void execute(UUID id) {
        transactions.execute(() -> {
            Coupon coupon = repository.findByIdForDeletion(id)
                    .orElseThrow(() -> new CouponNotFoundException(id));
            coupon.delete(time.now());
            repository.save(coupon);
        });
    }
}
