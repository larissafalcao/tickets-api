CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(6) NOT NULL,
    description TEXT NOT NULL,
    discount_value NUMERIC NOT NULL,
    expiration_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    published BOOLEAN NOT NULL,
    redeemed BOOLEAN NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT coupon_deletion_state CHECK (
        (status <> 'DELETED' AND deleted_at IS NULL)
        OR (status = 'DELETED' AND deleted_at IS NOT NULL)
    )
);
