package com.larissafalcao.tickets_api.infrastructure.persistence;

import com.larissafalcao.tickets_api.domain.coupon.CouponStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataCouponRepository extends JpaRepository<JpaCouponEntity, UUID> {
    Optional<JpaCouponEntity> findByIdAndStatusNot(UUID id, CouponStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select coupon from JpaCouponEntity coupon where coupon.id = :id")
    Optional<JpaCouponEntity> findByIdForDeletion(@Param("id") UUID id);
}
