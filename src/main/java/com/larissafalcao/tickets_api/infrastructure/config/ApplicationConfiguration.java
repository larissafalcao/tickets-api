package com.larissafalcao.tickets_api.infrastructure.config;

import com.larissafalcao.tickets_api.application.coupon.CreateCouponUseCase;
import com.larissafalcao.tickets_api.application.coupon.DeleteCouponUseCase;
import com.larissafalcao.tickets_api.application.coupon.GetCouponUseCase;
import com.larissafalcao.tickets_api.application.port.CouponRepository;
import com.larissafalcao.tickets_api.application.port.TimeProvider;
import com.larissafalcao.tickets_api.application.port.TransactionRunner;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {
    @Bean
    TimeProvider timeProvider() {
        return Instant::now;
    }

    @Bean
    CreateCouponUseCase createCouponUseCase(CouponRepository repository, TimeProvider time) {
        return new CreateCouponUseCase(repository, time);
    }

    @Bean
    GetCouponUseCase getCouponUseCase(CouponRepository repository) {
        return new GetCouponUseCase(repository);
    }

    @Bean
    DeleteCouponUseCase deleteCouponUseCase(CouponRepository repository, TimeProvider time,
                                           TransactionRunner transactions) {
        return new DeleteCouponUseCase(repository, time, transactions);
    }

    @Bean
    OpenAPI couponOpenApi() {
        return new OpenAPI().info(new Info().title("Coupon API").version("1.0.0")
                .description("Coupon creation, lookup and soft deletion. Repeated codes are permitted."));
    }
}
