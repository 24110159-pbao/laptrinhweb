package example.com.ltw.ai.service;

import org.springframework.stereotype.Component;

/**
 * Cung cấp schema và business rules cho AI sinh SQL.
 *
 * Chỉ mô tả những bảng và column thực tế được phép sử dụng
 * trong database lwtck_schema.
 */
@Component
public class DatabaseSchemaProvider {

    public String getSchemaAndRules() {

        return """
                DATABASE: lwtck_schema

                MySQL physical schema.
                Table and column names use snake_case.
                Only use the tables and columns listed below.

                ==================================================
                USERS
                ==================================================

                users(
                    id,
                    avatar_url,
                    created_at,
                    email,
                    email_verified,
                    full_name,
                    password,
                    phone,
                    role,
                    status,
                    updated_at,
                    branch_id
                )

                Rules:
                - email_verified is BOOLEAN/BIT.
                - role is one of:
                  ADMIN, MANAGER, SHIPPER, STAFF, USER.
                - status is one of ACTIVE, LOCKED.
                - branch_id references branches.id.
                - password exists but MUST NEVER be selected.
                - Never expose password or password hash.

                ==================================================
                ADDRESSES
                ==================================================

                addresses(
                    id,
                    district,
                    district_id,
                    is_default,
                    phone,
                    province,
                    province_id,
                    receiver_name,
                    street_address,
                    ward,
                    ward_code,
                    user_id
                )

                Rules:
                - user_id references users.id.
                - is_default is BOOLEAN/BIT.
                - An address stores recipient and shipping information.
                - Do not assume that each user has only one address.

                ==================================================
                BRANCHES
                ==================================================

                branches(
                    id,
                    address,
                    created_at,
                    email,
                    latitude,
                    longitude,
                    name,
                    phone,
                    status,
                    updated_at
                )

                Rules:
                - status is one of ACTIVE, INACTIVE.
                - latitude and longitude are geographic coordinates.

                ==================================================
                BRANDS
                ==================================================

                brands(
                    id,
                    logo_url,
                    name,
                    status
                )

                Rules:
                - status is one of ACTIVE, INACTIVE.

                ==================================================
                CATEGORIES
                ==================================================

                categories(
                    id,
                    created_at,
                    description,
                    name,
                    slug,
                    status
                )

                Rules:
                - status is one of ACTIVE, INACTIVE.

                ==================================================
                PRODUCTS
                ==================================================

                products(
                    id,
                    battery_capacity,
                    chipset,
                    created_at,
                    description,
                    name,
                    operating_system,
                    screen_size,
                    slug,
                    status,
                    updated_at,
                    brand_id,
                    category_id
                )

                Rules:
                - brand_id references brands.id.
                - category_id references categories.id.
                - status is one of ACTIVE, INACTIVE.
                - Product pricing and variant-specific attributes
                  belong to product_variants, not products.
                - Product images are stored in product_images.
                - Do not assume products has a price, stock,
                  image_url, ram, storage, or color column.

                ==================================================
                PRODUCT VARIANTS
                ==================================================

                product_variants(
                    id,
                    color,
                    created_at,
                    price,
                    ram,
                    sale_price,
                    status,
                    storage,
                    updated_at,
                    product_id
                )

                Rules:
                - product_id references products.id.
                - status is one of ACTIVE, INACTIVE.
                - price is the listed variant price.
                - sale_price is the variant sale price.
                - Do not assume sale_price is non-null or positive.
                - Do not assume sale_price is always lower than price.
                - RAM, storage, color, price, and sale_price belong
                  to product variants.
                - Do not assume a product has only one variant.

                ==================================================
                PRODUCT IMAGES
                ==================================================

                product_images(
                    id,
                    display_order,
                    image_url,
                    is_thumbnail,
                    public_id,
                    product_id
                )

                Rules:
                - product_id references products.id.
                - is_thumbnail is BOOLEAN/BIT.
                - A product can have multiple images.
                - Use display_order to determine image ordering
                  when ordering is needed.
                - Do not assume a product has exactly one image.

                ==================================================
                BRANCH INVENTORIES
                ==================================================

                branch_inventories(
                    id,
                    quantity,
                    reserved_quantity,
                    status,
                    updated_at,
                    branch_id,
                    variant_id
                )

                Rules:
                - branch_id references branches.id.
                - variant_id references product_variants.id.
                - status is one of ACTIVE, INACTIVE.
                - quantity is the recorded inventory quantity.
                - reserved_quantity is the reserved inventory quantity.
                - Do not assume quantity alone equals available stock.
                - When calculating available stock, use
                  quantity - reserved_quantity, provided the business
                  context treats reserved quantities as unavailable.
                - Do not assume there is only one inventory row
                  per variant across all branches.

                ==================================================
                CARTS
                ==================================================

                carts(
                    id,
                    created_at,
                    updated_at,
                    branch_id,
                    user_id
                )

                Rules:
                - user_id references users.id.
                - branch_id references branches.id.
                - Do not assume there is only one cart per user
                  unless guaranteed by the application.

                ==================================================
                CART ITEMS
                ==================================================

                cart_items(
                    id,
                    created_at,
                    quantity,
                    cart_id,
                    inventory_id
                )

                Rules:
                - cart_id references carts.id.
                - inventory_id references branch_inventories.id.
                - To retrieve a cart item's product, join:
                  cart_items.inventory_id
                  -> branch_inventories.id
                  -> branch_inventories.variant_id
                  -> product_variants.id
                  -> product_variants.product_id
                  -> products.id.

                ==================================================
                FAVORITES
                ==================================================

                favorites(
                    id,
                    created_at,
                    product_id,
                    user_id
                )

                Rules:
                - product_id references products.id.
                - user_id references users.id.
                - Do not assume a unique constraint unless confirmed.

                ==================================================
                ORDERS
                ==================================================

                orders(
                    id,
                    created_at,
                    discount_amount,
                    final_amount,
                    note,
                    order_code,
                    payment_method,
                    payment_status,
                    receiver_name,
                    receiver_phone,
                    shipping_district,
                    shipping_district_id,
                    shipping_fee,
                    shipping_province,
                    shipping_province_id,
                    shipping_street_address,
                    shipping_ward,
                    shipping_ward_code,
                    status,
                    subtotal,
                    updated_at,
                    assigned_staff_id,
                    branch_id,
                    discount_code_id,
                    user_id,
                    discount_applied_to_shipping,
                    payment_expires_at,
                    checkout_session_id
                )

                Rules:
                - user_id references users.id.
                - branch_id references branches.id.
                - assigned_staff_id references users.id.
                - discount_code_id references discount_codes.id.
                - status is one of:
                  CANCELLED,
                  CONFIRMED,
                  DELIVERED,
                  PENDING,
                  RETURN_REFUNDED,
                  SHIPPING.
                - payment_method is one of COD, VNPAY.
                - payment_status is one of:
                  FAILED,
                  PAID,
                  PARTIALLY_REFUNDED,
                  PENDING,
                  REFUNDED,
                  REFUND_REQUIRED,
                  UNPAID.
                - discount_applied_to_shipping is BOOLEAN/BIT.
                - receiver_name and receiver_phone are shipping snapshots.
                - Shipping address columns store the order's
                  shipping address snapshot.
                - final_amount is the final order amount after
                  applicable discounts and charges.
                - subtotal is the order subtotal.
                - shipping_fee is the shipping fee recorded for
                  the order.
                - discount_amount is the recorded discount amount.
                - Use order_code when a human-readable order code
                  is requested.
                - Do not assume payment_status and order status
                  represent the same state.

                ==================================================
                ORDER ITEMS
                ==================================================

                order_items(
                    id,
                    created_at,
                    product_name_snapshot,
                    quantity,
                    subtotal,
                    unit_price,
                    variant_snapshot,
                    order_id,
                    product_variant_id
                )

                Rules:
                - order_id references orders.id.
                - product_variant_id references product_variants.id.
                - product_name_snapshot is the product name at purchase.
                - variant_snapshot is the variant information at purchase.
                - unit_price is the historical unit price.
                - subtotal is the recorded order-item subtotal.
                - quantity is the purchased quantity.
                - Prefer snapshot columns for historical order details.
                - Do not assume current product prices equal historical
                  purchase prices.

                ==================================================
                DISCOUNT CODES
                ==================================================

                discount_codes(
                    id,
                    code,
                    discount_value,
                    end_at,
                    quantity,
                    start_at,
                    status
                )

                Rules:
                - discount_value is a percentage value.
                - quantity represents the stored remaining-use quantity.
                - status is one of ACTIVE, INACTIVE.
                - Check start_at and end_at when evaluating validity.
                - orders.discount_code_id references discount_codes.id.
                - Use orders.discount_code_id to associate a discount
                  code with an order.
                - Do not invent a discount usage-history table.
                - Do not assume discount_value is a fixed currency amount.

                ==================================================
                PAYMENTS
                ==================================================

                payments(
                    id,
                    amount,
                    bank_code,
                    created_at,
                    payment_date,
                    payment_method,
                    response_code,
                    status,
                    transaction_no,
                    order_id
                )

                Rules:
                - order_id references orders.id.
                - payment_method is one of COD, VNPAY.
                - status is one of FAILED, PENDING, SUCCESS.
                - amount is the amount recorded for the payment.
                - payment_date is the recorded payment date.
                - transaction_no is the payment transaction identifier.
                - Do not confuse payments.status with
                  orders.payment_status.
                - A successful payment uses payments.status = 'SUCCESS'.
                - Do not assume one order has only one payment record
                  unless guaranteed by the application.

                ==================================================
                SHIPMENTS
                ==================================================

                shipments(
                    id,
                    created_at,
                    delivered_at,
                    expected_delivery_at,
                    ghn_order_code,
                    shipping_fee,
                    status,
                    updated_at,
                    order_id,
                    shipper_id
                )

                Rules:
                - order_id references orders.id.
                - shipper_id references users.id.
                - status is one of:
                  CANCELLED,
                  DELIVERED,
                  DELIVERY_FAILED,
                  PENDING,
                  RECEIVED.
                - Do not confuse shipment status with order status.
                - Do not assume one order has only one shipment record
                  unless guaranteed by the application.

                ==================================================
                REVIEWS
                ==================================================

                reviews(
                    id,
                    comment,
                    created_at,
                    rating,
                    updated_at,
                    order_item_id,
                    product_id,
                    user_id
                )

                Rules:
                - order_item_id references order_items.id.
                - product_id references products.id.
                - user_id references users.id.
                - rating is expected to be between 1 and 5.
                - Use order_item_id to associate a review with
                  a specific purchased order item.
                - Do not assume a review has order_id directly.
                - Do not assume a unique constraint unless confirmed.
                - Do not assume review eligibility rules beyond
                  what can be established from the schema and
                  explicit application rules.

                ==================================================
                REVIEW MEDIA
                ==================================================

                review_media(
                    id,
                    created_at,
                    public_id,
                    resource_type,
                    url,
                    review_id
                )

                Rules:
                - review_id references reviews.id.
                - A review may have associated media records.
                - Use url to retrieve a media URL when requested.

                ==================================================
                CHAT CONVERSATIONS
                ==================================================

                chat_conversations(
                    id,
                    created_at,
                    status,
                    updated_at,
                    branch_id,
                    staff_id,
                    user_id
                )

                Rules:
                - branch_id references branches.id.
                - staff_id references users.id.
                - user_id references users.id.
                - status is one of CLOSED, OPEN.

                ==================================================
                CHAT MESSAGES
                ==================================================

                chat_messages(
                    id,
                    content,
                    created_at,
                    read_at,
                    conversation_id,
                    sender_id
                )

                Rules:
                - conversation_id references chat_conversations.id.
                - sender_id references users.id.
                - content contains the message body.
                - read_at records when a message was read, if available.

                ==================================================
                EMAIL OTPS
                ==================================================

                email_otps(
                    id,
                    attempt_count,
                    created_at,
                    email,
                    expires_at,
                    otp_hash,
                    purpose,
                    resend_count,
                    verified_at
                )

                Rules:
                - purpose is one of REGISTER, RESET_PASSWORD.
                - NEVER query email_otps.
                - NEVER select otp_hash.
                - NEVER return OTP values, hashes, or verification data.
                - Do not expose authentication or verification secrets.

                ==================================================
                RELATIONSHIPS
                ==================================================

                users.branch_id -> branches.id

                addresses.user_id -> users.id

                products.brand_id -> brands.id
                products.category_id -> categories.id

                product_variants.product_id -> products.id

                product_images.product_id -> products.id

                branch_inventories.branch_id -> branches.id
                branch_inventories.variant_id -> product_variants.id

                carts.branch_id -> branches.id
                carts.user_id -> users.id

                cart_items.cart_id -> carts.id
                cart_items.inventory_id -> branch_inventories.id

                favorites.product_id -> products.id
                favorites.user_id -> users.id

                orders.user_id -> users.id
                orders.branch_id -> branches.id
                orders.assigned_staff_id -> users.id
                orders.discount_code_id -> discount_codes.id

                order_items.order_id -> orders.id
                order_items.product_variant_id -> product_variants.id

                payments.order_id -> orders.id

                shipments.order_id -> orders.id
                shipments.shipper_id -> users.id

                reviews.order_item_id -> order_items.id
                reviews.product_id -> products.id
                reviews.user_id -> users.id

                review_media.review_id -> reviews.id

                chat_conversations.branch_id -> branches.id
                chat_conversations.staff_id -> users.id
                chat_conversations.user_id -> users.id

                chat_messages.conversation_id -> chat_conversations.id
                chat_messages.sender_id -> users.id

                ==================================================
                BUSINESS RULES
                ==================================================

                1. Only SELECT statements are allowed.
                   Do not generate INSERT, UPDATE, DELETE, DROP,
                   ALTER, CREATE, TRUNCATE, or other write statements.

                2. Never select or expose:
                   - password
                   - otp_hash
                   - OTP values
                   - API keys
                   - secrets
                   - authentication tokens
                   - credentials

                3. Never use SELECT *.
                   Always specify the required columns explicitly.

                4. Only query the approved tables and columns
                   listed in this schema.

                5. Never invent tables, columns, relationships,
                   enum values, or constraints.

                6. For actual recognized revenue, normally use:
                   orders.status = 'DELIVERED'.

                7. For order revenue, use orders.final_amount
                   unless the question explicitly asks for a
                   different financial metric.

                8. Do not sum orders.final_amount after joining
                   orders to order_items, payments, or shipments
                   if the join can duplicate an order.
                   Use aggregation or subqueries to avoid
                   double-counting.

                9. For product sales reports, use order_items.quantity
                   and historical order-item data as appropriate.
                   Apply the relevant order status filter when
                   calculating completed sales.

                10. For historical product revenue, use
                    order_items.unit_price or order_items.subtotal
                    as appropriate to the requested metric.
                    Do not use current product variant prices
                    as historical purchase prices.

                11. For payment reports, distinguish:
                    - orders.payment_status
                    - payments.status
                    - orders.status.

                12. A successful payment is represented by
                    payments.status = 'SUCCESS'.
                    Do not substitute orders.status = 'DELIVERED'
                    for payment success.

                13. For available inventory, consider
                    branch_inventories.quantity and
                    branch_inventories.reserved_quantity.
                    Do not treat inventory as product_variants.stock.

                14. For variant prices, use product_variants.price
                    and product_variants.sale_price.
                    Do not query products.price.

                15. For product images, use product_images.image_url.
                    Do not query products.image_url.

                16. For product names and variant attributes in
                    historical orders, prefer order_items snapshot
                    columns when appropriate.

                17. Discount codes are percentage-based.
                    An order references its discount code through
                    orders.discount_code_id.

                18. Do not invent a discount usage-history table.
                    Do not infer historical usage solely from
                    discount_codes.quantity.

                19. For shipping information, use the shipping
                    snapshot columns in orders when the question
                    concerns an order's shipping address.

                20. For shipment tracking, use shipments and its
                    actual status values. Do not invent shipment
                    tracking columns.

                21. For questions involving dates, use the current
                    date supplied by the application.

                22. When the schema does not contain enough
                    information to answer a question, do not guess.

                23. Use explicit JOIN conditions based only on
                    the relationships listed in this schema.

                24. Use clear aliases for tables with overlapping
                    column names.

                25. For aggregate queries, prevent duplicate
                    counting caused by one-to-many relationships.

                26. Never expose sensitive authentication data,
                    even if the user explicitly requests it.

                27. Return only read-only SQL statements when
                    generating SQL for database execution.
                """;
    }
}
