package com.larissafalcao.tickets_api.infrastructure.persistence;

import com.larissafalcao.tickets_api.application.port.CouponRepository;
import com.larissafalcao.tickets_api.domain.coupon.Coupon;
import com.larissafalcao.tickets_api.domain.coupon.CouponStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaCouponRepository implements CouponRepository {
    private final SpringDataCouponRepository repository;

    JpaCouponRepository(SpringDataCouponRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Coupon> findNotDeletedById(UUID id) {
        return repository.findByIdAndStatusNot(id, CouponStatus.DELETED).map(JpaCouponEntity::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Coupon> findByIdForDeletion(UUID id) {
        return repository.findByIdForDeletion(id).map(JpaCouponEntity::toDomain);
    }

    @Override
    public void save(Coupon coupon) {
        repository.saveAndFlush(JpaCouponEntity.from(coupon));
    }
}
