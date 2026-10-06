package com.larissafalcao.tickets_api.infrastructure.web;

import com.larissafalcao.tickets_api.application.coupon.CouponNotFoundException;
import com.larissafalcao.tickets_api.domain.coupon.CouponAlreadyDeletedException;
import com.larissafalcao.tickets_api.domain.coupon.InvalidCouponException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@NullMarked
@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidCouponException.class)
    ProblemDetail invalidCoupon(InvalidCouponException exception) {
        LOGGER.info("Coupon rejected: field={} reason={}", exception.field(), exception.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setProperty("field", exception.field());
        return problem;
    }

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail notFound(CouponNotFoundException exception) {
        LOGGER.info("Request rejected: status=404 reason={}", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CouponAlreadyDeletedException.class)
    ProblemDetail alreadyDeleted(CouponAlreadyDeletedException exception) {
        LOGGER.info("Request rejected: status=409 reason={}", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception exception, @Nullable Object body,
                                                                       HttpHeaders headers, HttpStatusCode status,
                                                                       WebRequest request) {
        LOGGER.info("Request rejected: status={} cause={}", status.value(), exception.getClass().getSimpleName());
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }
}
