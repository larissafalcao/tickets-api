package com.larissafalcao.tickets_api.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CouponTest {
    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");
    private static final BigDecimal HALF = new BigDecimal("0.5");

    @ParameterizedTest
    @MethodSource("validCodes")
    void removesSpecialCharactersAndPreservesCase(String raw, String normalized) {
        assertThat(create(raw, "Description", HALF, NOW, false).code())
                .isEqualTo(normalized);
    }

    static Stream<Arguments> validCodes() {
        return Stream.of(Arguments.of("ABC-123", "ABC123"), Arguments.of(" a b C_1.2/3 ", "abC123"),
                Arguments.of("éABC123😀", "ABC123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "!!!!!!", "ABCDE", "ABCDEFG", "ABC-1234", "áéíóúç"})
    void rejectsCodesWithoutExactlySixAlphanumericCharacters(String code) {
        assertThatThrownBy(() -> create(code, "Description", HALF, NOW, false))
                .isInstanceOf(InvalidCouponException.class)
                .extracting(exception -> ((InvalidCouponException) exception).field()).isEqualTo("code");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void requiresNonBlankDescription(String description) {
        assertThatThrownBy(() -> create("ABC123", description, HALF, NOW, false))
                .isInstanceOf(InvalidCouponException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-100", "0", "0.499999999999999999999"})
    void requiresDiscountOfAtLeastHalf(String discount) {
        BigDecimal value = discount == null ? null : new BigDecimal(discount);
        assertThatThrownBy(() -> create("ABC123", "Description", value, NOW, false))
                .isInstanceOf(InvalidCouponException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "0.5000", "1.1234567890123456789",
            "999999999999999999999999999999999999999999999999999999999999.123456789"})
    void acceptsDiscountsWithoutUpperLimitOrRounding(String discount) {
        BigDecimal value = new BigDecimal(discount);
        assertThat(create("ABC123", "Description", value, NOW, false).discountValue()).isEqualTo(value);
    }

    @Test
    void requiresExpirationDate() {
        assertThatThrownBy(() -> create("ABC123", "Description", HALF, null, false))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void rejectsExpirationEvenOneNanosecondInThePast() {
        Instant justBefore = NOW.minusNanos(1);
        assertThatThrownBy(() -> create("ABC123", "Description", HALF, justBefore, false))
                .isInstanceOf(InvalidCouponException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 3600})
    void acceptsExpirationAtOrAfterCreation(long nanoseconds) {
        Instant expiration = NOW.plusNanos(nanoseconds);
        assertThat(create("ABC123", "Description", HALF, expiration, false).expirationDate())
                .isEqualTo(expiration);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void initializesOnlyServerControlledFields(Boolean published) {
        Coupon coupon = create("ABC123", "Description", HALF, NOW, published);
        assertThat(coupon.id()).isNotNull();
        assertThat(coupon.published()).isEqualTo(Boolean.TRUE.equals(published));
        assertThat(coupon.status()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(coupon.redeemed()).isFalse();
        assertThat(coupon.deletedAt()).isNull();
    }

    @Test
    void deletingPreservesAllRegistrationFieldsAndCannotBeRepeated() {
        Coupon coupon = create("ABC-123", "Original description", new BigDecimal("0.812345"), NOW, true);
        UUID id = coupon.id();
        Instant deletion = NOW.plusSeconds(3600);
        coupon.delete(deletion);

        assertThat(coupon.id()).isEqualTo(id);
        assertThat(coupon.code()).isEqualTo("ABC123");
        assertThat(coupon.description()).isEqualTo("Original description");
        assertThat(coupon.discountValue()).isEqualTo(new BigDecimal("0.812345"));
        assertThat(coupon.expirationDate()).isEqualTo(NOW);
        assertThat(coupon.published()).isTrue();
        assertThat(coupon.redeemed()).isFalse();
        assertThat(coupon.status()).isEqualTo(CouponStatus.DELETED);
        assertThat(coupon.deletedAt()).isEqualTo(deletion);
        Instant later = deletion.plusSeconds(1);
        assertThatThrownBy(() -> coupon.delete(later))
                .isInstanceOf(CouponAlreadyDeletedException.class);
        assertThat(coupon.deletedAt()).isEqualTo(deletion);
    }

    @Test
    void reconstitutesExpiredAndRedeemedCouponAndAllowsDeletion() {
        UUID id = UUID.randomUUID();
        Coupon coupon = Coupon.reconstitute(new CouponSnapshot(id, "ABC123", "Description", HALF,
                NOW.minusSeconds(100), CouponStatus.ACTIVE, true, true, null));
        coupon.delete(NOW);
        assertThat(coupon.id()).isEqualTo(id);
        assertThat(coupon.redeemed()).isTrue();
        assertThat(coupon.status()).isEqualTo(CouponStatus.DELETED);
    }

    @Test
    void reconstitutingDeletedCouponDoesNotRestoreItsAbilityToBeDeleted() {
        Coupon coupon = Coupon.reconstitute(new CouponSnapshot(UUID.randomUUID(), "ABC123", "Description", HALF,
                NOW.minusSeconds(100), CouponStatus.DELETED, false, false, NOW.minusSeconds(10)));
        assertThatThrownBy(() -> coupon.delete(NOW)).isInstanceOf(CouponAlreadyDeletedException.class);
        assertThat(coupon.deletedAt()).isEqualTo(NOW.minusSeconds(10));
    }

    private Coupon create(String code, String description, BigDecimal discount, Instant expiration, Boolean published) {
        return Coupon.create(code, description, discount, expiration, published, NOW);
    }
}
