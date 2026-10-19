-- Run once on existing databases before allowing multiple carts per customer.
ALTER TABLE carts DROP INDEX uk_carts_user;
