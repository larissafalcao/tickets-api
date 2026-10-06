package com.larissafalcao.tickets_api.infrastructure.web;

import com.larissafalcao.tickets_api.application.coupon.CreateCouponCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "ABC-123",
                description = "Six ASCII alphanumeric characters after removing special characters.") String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Discount for the next purchase") String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0.5", example = "0.8") BigDecimal discountValue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2099-12-31T23:59:59Z",
                description = "ISO-8601 instant with timezone; must not be in the past.") Instant expirationDate,
        @Schema(defaultValue = "false", description = "Omitted or null means false.") Boolean published) {

    CreateCouponCommand toCommand() {
        return new CreateCouponCommand(code, description, discountValue, expirationDate, published);
    }
}
