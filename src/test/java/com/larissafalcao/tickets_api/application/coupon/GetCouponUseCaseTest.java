package com.larissafalcao.tickets_api.application.coupon;

import static com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.NOW;
import static com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.validCoupon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.InMemoryCouponRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetCouponUseCaseTest {
    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final GetCouponUseCase useCase = new GetCouponUseCase(repository);

    @Test
    void returnsACouponThatWasNotDeleted() {
        var coupon = validCoupon();
        repository.coupons.put(coupon.id(), coupon);

        assertThat(useCase.execute(coupon.id())).isEqualTo(CouponResult.from(coupon));
    }

    @Test
    void missingCouponIsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(unknown)).isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void deletedCouponIsNotFound() {
        var coupon = validCoupon();
        coupon.delete(NOW);
        repository.coupons.put(coupon.id(), coupon);
        UUID id = coupon.id();

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(CouponNotFoundException.class);
    }
}
