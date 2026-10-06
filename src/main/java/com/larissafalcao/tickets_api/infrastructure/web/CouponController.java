package com.larissafalcao.tickets_api.infrastructure.web;

import com.larissafalcao.tickets_api.application.coupon.CouponResult;
import com.larissafalcao.tickets_api.application.coupon.CreateCouponUseCase;
import com.larissafalcao.tickets_api.application.coupon.DeleteCouponUseCase;
import com.larissafalcao.tickets_api.application.coupon.GetCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupon")
@Tag(name = "coupon")
public final class CouponController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponController.class);

    private final CreateCouponUseCase create;
    private final GetCouponUseCase get;
    private final DeleteCouponUseCase delete;

    public CouponController(CreateCouponUseCase create, GetCouponUseCase get, DeleteCouponUseCase delete) {
        this.create = create;
        this.get = get;
        this.delete = delete;
    }

    @PostMapping
    @Operation(summary = "Create a coupon")
    @ApiResponse(responseCode = "201", description = "Coupon created")
    @ApiResponse(responseCode = "400", description = "Invalid request or coupon",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CouponResult> create(@RequestBody CreateCouponRequest request) {
        CouponResult result = create.execute(request.toCommand());
        LOGGER.info("Coupon created: id={}", result.id());
        return ResponseEntity.created(URI.create("/coupon/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a coupon that was not deleted")
    @ApiResponse(responseCode = "200", description = "Coupon found")
    @ApiResponse(responseCode = "400", description = "Malformed UUID",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Coupon missing or deleted",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CouponResult get(@PathVariable UUID id) {
        CouponResult result = get.execute(id);
        LOGGER.info("Coupon retrieved: id={}", id);
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a coupon")
    @ApiResponse(responseCode = "204", description = "Coupon deleted", content = @Content)
    @ApiResponse(responseCode = "400", description = "Malformed UUID",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Coupon missing",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Coupon already deleted",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        delete.execute(id);
        LOGGER.info("Coupon deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
