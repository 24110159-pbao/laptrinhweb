CREATE DATABASE lwtck
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;


USE lwtck;

-- =====================================================
-- 1. BRANCHES
-- =====================================================
INSERT INTO branches
(id, name, address, email, phone, latitude, longitude, status, created_at, updated_at)
VALUES
(1, 'Chi nhánh TP.HCM', 'Quận 1, TP.HCM', 'hcm@example.com', '0901000001', 10.7769000, 106.7009000, 'ACTIVE', NOW(), NOW()),
(2, 'Chi nhánh Hà Nội', 'Cầu Giấy, Hà Nội', 'hn@example.com', '0901000002', 21.0362000, 105.7906000, 'ACTIVE', NOW(), NOW()),
(3, 'Chi nhánh Đà Nẵng', 'Hải Châu, Đà Nẵng', 'dn@example.com', '0901000003', 16.0544000, 108.2022000, 'ACTIVE', NOW(), NOW());


-- =====================================================
-- 2. USERS
-- =====================================================
INSERT INTO users
(id, full_name, email, password, phone, role, status, branch_id, email_verified, created_at, updated_at)
VALUES
(1, 'Admin', 'admin@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000001', 'ADMIN', 'ACTIVE', 1, b'1', NOW(), NOW()),
(2, 'Quản lý 1', 'manager1@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000002', 'MANAGER', 'ACTIVE', 1, b'1', NOW(), NOW()),
(3, 'Quản lý 2', 'manager2@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000003', 'MANAGER', 'ACTIVE', 2, b'1', NOW(), NOW()),
(4, 'Nhân viên 1', 'staff1@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000004', 'STAFF', 'ACTIVE', 1, b'1', NOW(), NOW()),
(5, 'Nhân viên 2', 'staff2@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000005', 'STAFF', 'ACTIVE', 2, b'1', NOW(), NOW()),
(6, 'Nhân viên 3', 'staff3@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000006', 'STAFF', 'ACTIVE', 3, b'1', NOW(), NOW()),
(7, 'Shipper 1', 'shipper1@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000007', 'SHIPPER', 'ACTIVE', 1, b'1', NOW(), NOW()),
(8, 'Shipper 2', 'shipper2@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000008', 'SHIPPER', 'ACTIVE', 2, b'1', NOW(), NOW()),
(9, 'Nguyễn Minh Anh', 'user1@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000009', 'USER', 'ACTIVE', NULL, b'1', NOW(), NOW()),
(10, 'Trần Hoàng Nam', 'user2@gmail.com', '$2a$10$4TozRZaTiflOLwzzpV7D4.zX6jc9VbBgm1sn4ItlzSZgeHK5ZDheu', '0911000010', 'USER', 'ACTIVE', NULL, b'1', NOW(), NOW());


-- =====================================================
-- 3. BRANDS
-- =====================================================
INSERT INTO brands (id, name, logo_url, status)
VALUES
(1, 'Apple', '', 'ACTIVE'),
(2, 'Samsung', '', 'ACTIVE'),
(3, 'Xiaomi', '', 'ACTIVE');


-- =====================================================
-- 4. CATEGORIES
-- =====================================================
INSERT INTO categories
(id, name, slug, description, status, created_at)
VALUES
(1, 'Điện thoại', 'dien-thoai', 'Điện thoại thông minh', 'ACTIVE', NOW()),
(2, 'Phụ kiện', 'phu-kien', 'Phụ kiện điện thoại', 'ACTIVE', NOW());


-- =====================================================
-- 5. PRODUCTS
-- =====================================================
INSERT INTO products
(id, name, slug, description, battery_capacity, chipset, operating_system, screen_size, status, brand_id, category_id, created_at, updated_at)
VALUES
(1, 'iPhone 16', 'iphone-16', 'Điện thoại Apple', 3561, 'Apple A18', 'iOS', 6.10, 'ACTIVE', 1, 1, NOW(), NOW()),
(2, 'iPhone 16 Pro', 'iphone-16-pro', 'Điện thoại Apple phiên bản Pro', 3582, 'Apple A18 Pro', 'iOS', 6.30, 'ACTIVE', 1, 1, NOW(), NOW()),
(3, 'Samsung Galaxy S25', 'samsung-s25', 'Điện thoại Samsung', 4000, 'Snapdragon 8 Elite', 'Android', 6.20, 'ACTIVE', 2, 1, NOW(), NOW()),
(4, 'Xiaomi 14T', 'xiaomi-14t', 'Điện thoại Xiaomi', 5000, 'Dimensity 8300-Ultra', 'Android', 6.67, 'ACTIVE', 3, 1, NOW(), NOW()),
(5, 'Cáp sạc USB-C', 'cap-sac-usbc', 'Phụ kiện sạc điện thoại', NULL, NULL, NULL, NULL, 'ACTIVE', 3, 2, NOW(), NOW());


-- 6. PRODUCT VARIANTS
INSERT INTO product_variants
(id, product_id, color, ram, storage, price, sale_price, status, created_at, updated_at)
VALUES
(1, 1, 'Đen', '8GB', '128GB', 22000000, 21000000, 'ACTIVE', NOW(), NOW()),
(2, 1, 'Trắng', '8GB', '256GB', 25000000, 24000000, 'ACTIVE', NOW(), NOW()),
(3, 2, 'Titan', '8GB', '256GB', 29000000, 28000000, 'ACTIVE', NOW(), NOW()),
(4, 3, 'Xanh', '12GB', '256GB', 22000000, 21000000, 'ACTIVE', NOW(), NOW()),
(5, 4, 'Xám', '12GB', '256GB', 13000000, 12000000, 'ACTIVE', NOW(), NOW()),
(6, 5, 'Trắng', 'Không áp dụng', 'Không áp dụng', 250000, 200000, 'ACTIVE', NOW(), NOW());

-- =====================================================
-- 7. PRODUCT IMAGES
-- =====================================================
INSERT INTO product_images
(id, product_id, image_url, public_id, is_thumbnail, display_order)
VALUES
(1, 1, 'https://example.com/iphone16.jpg', 'iphone16', b'1', 1),
(2, 1, 'https://example.com/iphone16-back.jpg', 'iphone16-back', b'0', 2),
(3, 2, 'https://example.com/iphone16pro.jpg', 'iphone16pro', b'1', 1),
(4, 3, 'https://example.com/samsung-s25.jpg', 'samsung-s25', b'1', 1),
(5, 4, 'https://example.com/xiaomi14t.jpg', 'xiaomi14t', b'1', 1),
(6, 5, 'https://example.com/cap-sac.jpg', 'cap-sac', b'1', 1);


-- =====================================================
-- 8. BRANCH INVENTORIES
-- =====================================================
INSERT INTO branch_inventories
(id, branch_id, variant_id, quantity, reserved_quantity, status, updated_at)
VALUES
(1, 1, 1, 20, 0, 'ACTIVE', NOW()),
(2, 1, 2, 15, 0, 'ACTIVE', NOW()),
(3, 1, 3, 10, 0, 'ACTIVE', NOW()),
(4, 2, 4, 20, 0, 'ACTIVE', NOW()),
(5, 2, 5, 15, 0, 'ACTIVE', NOW()),
(6, 3, 6, 50, 0, 'ACTIVE', NOW());


-- =====================================================
-- 9. ADDRESSES
-- =====================================================
INSERT INTO addresses
(id, user_id, receiver_name, phone, province, province_id, district, district_id, ward, ward_code, street_address, is_default)
VALUES
(1, 9, 'Nguyễn Minh Anh', '0911000009', 'Hồ Chí Minh', 79, 'Quận 1', 760, 'Bến Nghé', '26734', '12 Nguyễn Huệ', b'1'),
(2, 10, 'Trần Hoàng Nam', '0911000010', 'Hà Nội', 1, 'Cầu Giấy', 5, 'Dịch Vọng', '00136', '100 Cầu Giấy', b'1');


-- =====================================================
-- 10. CARTS
-- =====================================================
INSERT INTO carts (id, user_id, branch_id, created_at, updated_at)
VALUES
(1, 9, 1, NOW(), NOW()),
(2, 10, 2, NOW(), NOW());


-- =====================================================
-- 11. CART ITEMS
-- =====================================================
INSERT INTO cart_items (id, cart_id, inventory_id, quantity, created_at)
VALUES
(1, 1, 1, 1, NOW()),
(2, 1, 2, 1, NOW()),
(3, 2, 4, 1, NOW());


-- =====================================================
-- 12. FAVORITES
-- =====================================================
INSERT INTO favorites (id, user_id, product_id, created_at)
VALUES
(1, 9, 1, NOW()),
(2, 9, 3, NOW()),
(3, 10, 2, NOW());


-- =====================================================
-- 13. DISCOUNT CODES
-- =====================================================
INSERT INTO discount_codes
(id, code, discount_value, quantity, start_at, end_at, status)
VALUES
(1, 'SALE10', 10.00, 100, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE'),
(2, 'SALE20', 20.00, 50, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE');


-- =====================================================
-- 14. ORDERS
-- =====================================================
INSERT INTO orders
(id, order_code, user_id, branch_id, receiver_name, receiver_phone,
 shipping_province, shipping_province_id, shipping_district, shipping_district_id,
 shipping_ward, shipping_ward_code, shipping_street_address,
 subtotal, discount_amount, shipping_fee, final_amount, note,
 status, payment_method, payment_status, discount_code_id, created_at, updated_at)
VALUES
(1, 'ORD001', 9, 1, 'Nguyễn Minh Anh', '0911000009', 'Hồ Chí Minh', 79, 'Quận 1', 760, 'Bến Nghé', '26734', '12 Nguyễn Huệ', 21000000, 0, 30000, 21030000, '', 'PENDING', 'COD', 'UNPAID', NULL, NOW(), NOW()),
(2, 'ORD002', 10, 2, 'Trần Hoàng Nam', '0911000010', 'Hà Nội', 1, 'Cầu Giấy', 5, 'Dịch Vọng', '00136', '100 Cầu Giấy', 21000000, 0, 30000, 21030000, '', 'CONFIRMED', 'COD', 'UNPAID', NULL, NOW(), NOW()),
(3, 'ORD003', 9, 1, 'Nguyễn Minh Anh', '0911000009', 'Hồ Chí Minh', 79, 'Quận 1', 760, 'Bến Nghé', '26734', '12 Nguyễn Huệ', 28000000, 0, 30000, 28030000, '', 'SHIPPING', 'VNPAY', 'PAID', NULL, NOW(), NOW()),
(4, 'ORD004', 10, 2, 'Trần Hoàng Nam', '0911000010', 'Hà Nội', 1, 'Cầu Giấy', 5, 'Dịch Vọng', '00136', '100 Cầu Giấy', 12000000, 0, 30000, 12030000, '', 'DELIVERED', 'VNPAY', 'PAID', NULL, NOW(), NOW()),
(5, 'ORD005', 9, 1, 'Nguyễn Minh Anh', '0911000009', 'Hồ Chí Minh', 79, 'Quận 1', 760, 'Bến Nghé', '26734', '12 Nguyễn Huệ', 24000000, 0, 30000, 24030000, '', 'CANCELLED', 'COD', 'UNPAID', NULL, NOW(), NOW()),
(6, 'ORD006', 10, 2, 'Trần Hoàng Nam', '0911000010', 'Hà Nội', 1, 'Cầu Giấy', 5, 'Dịch Vọng', '00136', '100 Cầu Giấy', 12000000, 0, 30000, 12030000, '', 'RETURN_REFUNDED', 'VNPAY', 'FAILED', NULL, NOW(), NOW()),
(7, 'ORD007', 9, 1, 'Nguyễn Minh Anh', '0911000009', 'Hồ Chí Minh', 79, 'Quận 1', 760, 'Bến Nghé', '26734', '12 Nguyễn Huệ', 200000, 0, 30000, 230000, '', 'PENDING', 'VNPAY', 'FAILED', NULL, NOW(), NOW()),
(8, 'ORD008', 10, 2, 'Trần Hoàng Nam', '0911000010', 'Hà Nội', 1, 'Cầu Giấy', 5, 'Dịch Vọng', '00136', '100 Cầu Giấy', 200000, 0, 30000, 230000, '', 'CONFIRMED', 'VNPAY', 'UNPAID', NULL, NOW(), NOW()),
(9, 'ORD009', 9, 1, 'Nguyễn Minh Anh', '0911000009', 'Hồ Chí Minh', 79, 'Quận 1', 760, 'Bến Nghé', '26734', '12 Nguyễn Huệ', 21000000, 0, 30000, 21030000, '', 'SHIPPING', 'COD', 'UNPAID', NULL, NOW(), NOW()),
(10, 'ORD010', 10, 3, 'Trần Hoàng Nam', '0911000010', 'Hà Nội', 1, 'Cầu Giấy', 5, 'Dịch Vọng', '00136', '100 Cầu Giấy', 21000000, 0, 30000, 21030000, '', 'DELIVERED', 'VNPAY', 'PAID', NULL, NOW(), NOW());


-- =====================================================
-- 15. ORDER ITEMS
-- =====================================================
INSERT INTO order_items
(id, order_id, product_variant_id, product_name_snapshot, variant_snapshot, quantity, unit_price, subtotal, created_at)
VALUES
(1, 1, 1, 'iPhone 16', 'Đen / 128GB', 1, 21000000, 21000000, NOW()),
(2, 2, 4, 'Samsung Galaxy S25', 'Xanh / 256GB', 1, 21000000, 21000000, NOW()),
(3, 3, 3, 'iPhone 16 Pro', 'Titan / 256GB', 1, 28000000, 28000000, NOW()),
(4, 4, 5, 'Xiaomi 14T', 'Xám / 256GB', 1, 12000000, 12000000, NOW()),
(5, 5, 2, 'iPhone 16', 'Trắng / 256GB', 1, 24000000, 24000000, NOW()),
(6, 6, 5, 'Xiaomi 14T', 'Xám / 256GB', 1, 12000000, 12000000, NOW()),
(7, 7, 6, 'Cáp sạc USB-C', 'Trắng', 1, 200000, 200000, NOW()),
(8, 8, 6, 'Cáp sạc USB-C', 'Trắng', 1, 200000, 200000, NOW()),
(9, 9, 1, 'iPhone 16', 'Đen / 128GB', 1, 21000000, 21000000, NOW()),
(10, 10, 4, 'Samsung Galaxy S25', 'Xanh / 256GB', 1, 21000000, 21000000, NOW());


-- =====================================================
-- 16. PAYMENTS
-- =====================================================
INSERT INTO payments
(id, order_id, amount, bank_code, created_at, payment_date, payment_method, response_code, status, transaction_no)
VALUES
(1, 1, 21030000, NULL, NOW(), NULL, 'COD', NULL, 'PENDING', NULL),
(2, 2, 21030000, NULL, NOW(), NULL, 'COD', NULL, 'PENDING', NULL),
(3, 3, 28030000, 'NCB', NOW(), NOW(), 'VNPAY', '00', 'SUCCESS', 'TX003'),
(4, 4, 12030000, 'NCB', NOW(), NOW(), 'VNPAY', '00', 'SUCCESS', 'TX004'),
(5, 5, 24030000, NULL, NOW(), NULL, 'COD', NULL, 'PENDING', NULL),
(6, 6, 12030000, 'NCB', NOW(), NULL, 'VNPAY', '99', 'FAILED', 'TX006'),
(7, 7, 230000, 'NCB', NOW(), NULL, 'VNPAY', '99', 'FAILED', 'TX007'),
(8, 8, 230000, 'NCB', NOW(), NULL, 'VNPAY', '00', 'PENDING', 'TX008'),
(9, 9, 21030000, NULL, NOW(), NULL, 'COD', NULL, 'PENDING', NULL),
(10, 10, 21030000, 'NCB', NOW(), NOW(), 'VNPAY', '00', 'SUCCESS', 'TX010');


-- =====================================================
-- 17. SHIPMENTS
-- =====================================================
INSERT INTO shipments
(id, order_id, shipper_id, ghn_order_code, shipping_fee, status,
 expected_delivery_at, delivered_at, created_at, updated_at)
VALUES
(1, 1, 7, 'GHN001', 30000, 'PENDING', DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, NOW(), NOW()),
(2, 2, 8, 'GHN002', 30000, 'RECEIVED', DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, NOW(), NOW()),
(3, 3, 7, 'GHN003', 30000, 'RECEIVED', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NOW(), NOW()),
(4, 4, 8, 'GHN004', 30000, 'DELIVERED', NOW(), NOW(), NOW(), NOW()),
(5, 5, 7, 'GHN005', 30000, 'CANCELLED', NOW(), NULL, NOW(), NOW()),
(6, 6, 8, 'GHN006', 30000, 'CANCELLED', NOW(), NULL, NOW(), NOW()),
(7, 9, 7, 'GHN009', 30000, 'DELIVERY_FAILED', NOW(), NULL, NOW(), NOW()),
(8, 10, 8, 'GHN010', 30000, 'DELIVERED', NOW(), NOW(), NOW(), NOW());


-- =====================================================
-- 18. REVIEWS
-- =====================================================
INSERT INTO reviews
(id, user_id, product_id, order_item_id, rating, comment, created_at, updated_at)
VALUES
(1, 10, 4, 4, 5, 'Sản phẩm tốt, giao hàng nhanh.', NOW(), NOW()),
(2, 10, 3, 10, 4, 'Máy đẹp, sử dụng ổn định.', NOW(), NOW());


-- =====================================================
-- 19. REVIEW MEDIA
-- =====================================================
INSERT INTO review_media
(id, review_id, url, public_id, resource_type, created_at)
VALUES
(1, 1, 'https://example.com/review1.jpg', 'review1', 'image', NOW()),
(2, 2, 'https://example.com/review2.jpg', 'review2', 'image', NOW());


-- =====================================================
-- 20. CHAT CONVERSATIONS
-- =====================================================
INSERT INTO chat_conversations
(id, user_id, branch_id, staff_id, status, created_at, updated_at)
VALUES
(1, 9, 1, 4, 'OPEN', NOW(), NOW()),
(2, 10, 2, 5, 'CLOSED', NOW(), NOW());


-- =====================================================
-- 21. CHAT MESSAGES
-- =====================================================
INSERT INTO chat_messages
(id, conversation_id, sender_id, content, created_at, read_at)
VALUES
(1, 1, 9, 'Xin chào, sản phẩm còn hàng không?', NOW(), NOW()),
(2, 1, 4, 'Dạ, sản phẩm hiện vẫn còn hàng.', NOW(), NULL),
(3, 2, 10, 'Tôi muốn hỏi về bảo hành.', NOW(), NOW()),
(4, 2, 5, 'Sản phẩm được bảo hành theo chính sách hãng.', NOW(), NOW());


-- =====================================================
-- 22. EMAIL OTPS
-- Dữ liệu hash bên dưới chỉ dùng để demo.
-- =====================================================
INSERT INTO email_otps
(id, email, otp_hash, purpose, attempt_count, resend_count, created_at, expires_at, verified_at)
VALUES
(1, 'user1@gmail.com', 'demo-hash-register', 'REGISTER', 0, 0, NOW(), DATE_ADD(NOW(), INTERVAL 5 MINUTE), NOW()),
(2, 'user2@gmail.com', 'demo-hash-reset', 'RESET_PASSWORD', 0, 0, NOW(), DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL);
