package com.larissafalcao.tickets_api.application.coupon;

import static com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.NOW;
import static com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.validCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larissafalcao.tickets_api.application.coupon.CouponUseCaseTestSupport.InMemoryCouponRepository;
import com.larissafalcao.tickets_api.domain.coupon.InvalidCouponException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CreateCouponUseCaseTest {
    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final CreateCouponUseCase useCase = new CreateCouponUseCase(repository, () -> NOW);

    @Test
    void returnsTheSameCouponThatWasSaved() {
        CouponResult result = useCase.execute(validCommand());

        assertThat(repository.coupons).hasSize(1).containsKey(result.id());
        assertThat(result).isEqualTo(CouponResult.from(repository.coupons.get(result.id())));
        assertThat(result.code()).isEqualTo("ABC123");
    }

    @Test
    void invalidCommandDoesNotReachPersistence() {
        var invalid = new CreateCouponCommand("ABC123", "Description", new BigDecimal("0.49"),
                NOW.plusSeconds(60), false);

        assertThatThrownBy(() -> useCase.execute(invalid)).isInstanceOf(InvalidCouponException.class);
        assertThat(repository.coupons).isEmpty();
    }

    @Test
    void repeatedCodesCreateIndependentIdentities() {
        CouponResult first = useCase.execute(validCommand());
        CouponResult second = useCase.execute(validCommand());

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.code()).isEqualTo(second.code());
        assertThat(repository.coupons).hasSize(2);
    }
}
