package com.larissafalcao.tickets_api.application.coupon;

import static com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.NOW;
import static com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.validCoupon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.InMemoryCouponRepository;
import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import com.larissafalcao.tickets_api.domain.coupon.CouponAlreadyDeletedException;
import com.larissafalcao.tickets_api.domain.coupon.CouponStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeleteCouponUseCaseTest {
    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final DeleteCouponUseCase useCase = new DeleteCouponUseCase(repository, () -> NOW, Runnable::run);

    @Test
    void marksTheCouponAsDeletedAtTheCurrentTime() {
        var coupon = validCoupon();
        repository.coupons.put(coupon.id(), coupon);

        useCase.execute(coupon.id());

        Coupon saved = repository.coupons.get(coupon.id());
        assertThat(saved.status()).isEqualTo(CouponStatus.DELETED);
        assertThat(saved.deletedAt()).isEqualTo(NOW);
    }

    @Test
    void missingCouponIsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(unknown)).isInstanceOf(CouponNotFoundException.class);
        assertThat(repository.coupons).isEmpty();
    }

    @Test
    void repeatedDeletionIsRejectedAndKeepsTheOriginalTimestamp() {
        var coupon = validCoupon();
        coupon.delete(NOW.minusSeconds(1));
        repository.coupons.put(coupon.id(), coupon);
        UUID id = coupon.id();

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(CouponAlreadyDeletedException.class);
        assertThat(coupon.deletedAt()).isEqualTo(NOW.minusSeconds(1));
    }
}
