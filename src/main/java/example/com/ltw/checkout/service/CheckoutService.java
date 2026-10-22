package example.com.ltw.checkout.service;

import example.com.ltw.address.dto.AddressRequest;
import example.com.ltw.address.entity.Address;
import example.com.ltw.cart.entity.Cart;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.cart.repository.CartItemRepository;
import example.com.ltw.cart.repository.CartRepository;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.checkout.entity.CheckoutSession;
import example.com.ltw.checkout.entity.CheckoutStatus;
import example.com.ltw.checkout.entity.InventoryReservation;
import example.com.ltw.checkout.repository.CheckoutSessionRepository;
import example.com.ltw.checkout.repository.InventoryReservationRepository;
import example.com.ltw.discount.entity.DiscountCode;
import example.com.ltw.discount.entity.DiscountStatus;
import example.com.ltw.discount.entity.DiscountType;
import example.com.ltw.discount.entity.DiscountUsage;
import example.com.ltw.discount.repository.DiscountCodeRepository;
import example.com.ltw.discount.repository.DiscountUsageRepository;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.entity.InventoryTransaction;
import example.com.ltw.inventory.entity.InventoryTransactionType;
import example.com.ltw.inventory.repository.InventoryTransactionRepository;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.OrderStatusHistory;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.payment.entity.Payment;
import example.com.ltw.payment.entity.TransactionStatus;
import example.com.ltw.payment.repository.PaymentRepository;
import example.com.ltw.payment.service.VnPayService;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CheckoutSessionRepository checkoutRepository;
    private final InventoryReservationRepository reservationRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final CheckoutAddressService checkoutAddressService;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountUsageRepository discountUsageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final VnPayService vnPayService;

    @Value("${checkout.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Value("${checkout.vnpay-timeout-minutes:0}")
    private int vnPayTimeoutMinutes;

    @Transactional
    public CheckoutSession start(Long userId, Long cartId) {
        validateHoldConfiguration();
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        Cart cart = cartRepository.findByIdForUpdateAndUserId(cartId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giỏ hàng."));
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Không thể thanh toán giỏ hàng đang trống.");
        }
        CheckoutSession active = checkoutRepository
                .findFirstByCartIdAndStatusOrderByIdDesc(cartId, CheckoutStatus.ACTIVE)
                .orElse(null);
        if (active != null) {
            if (active.getExpiresAt().isAfter(LocalDateTime.now())) return active;
            releaseCheckout(active, CheckoutStatus.EXPIRED);
        }

        List<CartItem> items = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getInventory().getId()))
                .toList();
        for (CartItem item : items) {
            BranchInventory inventory = inventoryRepository.findByIdForUpdate(item.getInventory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không còn tồn tại trong kho."));
            validateSellable(inventory);
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Không đủ tồn kho cho "
                        + inventory.getVariant().getProduct().getName()
                        + ". Số lượng tối đa có thể mua: " + inventory.getAvailableQuantity() + ".");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        CheckoutSession session = new CheckoutSession();
        session.setCart(cart);
        session.setUser(cart.getUser());
        session.setOrderCode("NX" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        session.setStartedAt(now);
        session.setExpiresAt(now.plusMinutes(holdDurationMinutes));
        session.setStatus(CheckoutStatus.ACTIVE);
        checkoutRepository.save(session);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            BranchInventory inventory = inventoryRepository.findByIdForUpdate(item.getInventory().getId()).orElseThrow();
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            BigDecimal price = currentPrice(inventory);
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            InventoryReservation reservation = new InventoryReservation();
            reservation.setCheckoutSession(session);
            reservation.setInventory(inventory);
            reservation.setQuantity(item.getQuantity());
            reservation.setExpiresAt(session.getExpiresAt());
            reservation.setConvertedToOrder(false);
            reservationRepository.save(reservation);
        }
        session.setSubtotal(subtotal);
        session.setFinalAmount(subtotal);
        return session;
    }

    @Transactional(readOnly = true)
    public CheckoutSession getActive(Long checkoutId, Long userId) {
        CheckoutSession session = checkoutRepository.findByIdAndUserId(checkoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên thanh toán."));
        if (session.getStatus() != CheckoutStatus.ACTIVE || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Phiên thanh toán đã hết hạn hoặc không còn hoạt động.");
        }
        return session;
    }

    @Transactional
    public void cancel(Long checkoutId, Long userId) {
        CheckoutSession session = lockActive(checkoutId, userId);
        releaseCheckout(session, CheckoutStatus.CANCELLED);
    }

    @Transactional(noRollbackFor = CheckoutPriceChangedException.class)
    public CheckoutSession quote(
            Long checkoutId, Long userId, Long addressId, AddressRequest newAddress, String discountCode
    ) {
        CheckoutSession session = lockActive(checkoutId, userId);
        Address address;
        if (addressId != null) {
            address = checkoutAddressService.getExistingAddress(addressId, userId);
        } else {
            address = checkoutAddressService.saveNewAddress(userId, newAddress);
        }

        setAddressSnapshot(session, address);
        List<CartItem> items = lockAndValidateItems(session);
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discount = calculateDiscount(session, items, discountCode, shippingFee);
        session.setShippingFee(shippingFee);
        session.setDiscountAmount(discount);
        session.setFinalAmount(session.getSubtotal().subtract(discount).add(shippingFee));
        return session;
    }

    @Transactional(noRollbackFor = CheckoutPriceChangedException.class)
    public FinalizeResult finalizeOrder(Long checkoutId, Long userId, PaymentMethod paymentMethod, String clientIp) {
        CheckoutSession session = lockActive(checkoutId, userId);
        if (session.getReceiverName() == null) {
            throw new IllegalArgumentException("Hãy nhập địa chỉ nhận hàng trước khi xác nhận đơn.");
        }
        if (paymentMethod == PaymentMethod.VNPAY && vnPayTimeoutMinutes <= 0) {
            throw new IllegalStateException("Chưa cấu hình thời hạn thanh toán VNPay.");
        }
        List<CartItem> items = lockAndValidateItems(session);
        BigDecimal freshFee = BigDecimal.ZERO;
        BigDecimal freshDiscount = calculateDiscount(
                session, items, session.getDiscountCodeValue(), freshFee
        );
        if (freshFee.compareTo(session.getShippingFee()) != 0
                || freshDiscount.compareTo(session.getDiscountAmount()) != 0) {
            session.setShippingFee(freshFee);
            session.setDiscountAmount(freshDiscount);
            session.setFinalAmount(session.getSubtotal().subtract(freshDiscount).add(freshFee));
            session.setGhnQuoteToken(UUID.randomUUID().toString());
            return new FinalizeResult(null, null, true,
                    "Tổng tiền đã được cập nhật. Vui lòng kiểm tra và xác nhận lại.");
        }

        DiscountCode discountCode = session.getDiscountCode();
        if (discountCode != null) {
            DiscountCode lockedCode = discountCodeRepository.findByCodeForUpdate(discountCode.getCode())
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không còn hợp lệ."));
            validateDiscount(lockedCode, session, items, userId);
            if (lockedCode.getQuantity() <= 0) {
                throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
            }
            lockedCode.setQuantity(lockedCode.getQuantity() - 1);
            discountCode = lockedCode;
        }

        Order order = createOrder(session, items, discountCode, paymentMethod);
        if (discountCode != null) {
            DiscountUsage usage = new DiscountUsage();
            usage.setDiscountCode(discountCode);
            usage.setUser(session.getUser());
            usage.setOrder(order);
            discountUsageRepository.save(usage);
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTransactionNo(order.getOrderCode());
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(TransactionStatus.PENDING);
        paymentRepository.save(payment);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShippingFee(order.getShippingFee());
        shipmentRepository.save(shipment);

        for (InventoryReservation reservation :
                reservationRepository.findByCheckoutSessionAndConvertedToOrderFalse(session)) {
            reservation.setConvertedToOrder(true);
            BranchInventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventory().getId())
                    .orElseThrow();
            int before = inventory.getQuantity();
            int after = before - reservation.getQuantity();
            inventory.setQuantity(after);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryTransactionRepository.save(InventoryTransaction.create(
                    inventory.getVariant(), inventory.getBranch(), InventoryTransactionType.ORDER_SOLD,
                    before, after, order.getOrderCode(), "Chuyển hàng giữ chỗ thành đơn hàng.", session.getUser()
            ));
            reservation.setConvertedAt(LocalDateTime.now());
        }
        session.setStatus(CheckoutStatus.COMPLETED);
        cartItemRepository.deleteByCartId(session.getCart().getId());

        String paymentUrl = paymentMethod == PaymentMethod.VNPAY
                ? vnPayService.createPaymentUrl(order.getOrderCode(), order.getFinalAmount(), clientIp,
                order.getPaymentExpiresAt())
                : null;
        return new FinalizeResult(order.getOrderCode(), paymentUrl, false, null);
    }

    @Transactional
    public void releaseCheckout(CheckoutSession session, CheckoutStatus terminalStatus) {
        CheckoutSession lockedSession = checkoutRepository.findByIdForUpdate(session.getId())
                .orElseThrow(() -> new IllegalStateException("Phiên Checkout cần giải phóng không còn tồn tại."));
        if (lockedSession.getStatus() != CheckoutStatus.ACTIVE) return;
        for (InventoryReservation reservation :
                reservationRepository.findByCheckoutSessionAndConvertedToOrderFalse(lockedSession)) {
            BranchInventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventory().getId())
                    .orElseThrow();
            if (inventory.getReservedQuantity() < reservation.getQuantity()) {
                throw new IllegalStateException("Tồn kho giữ chỗ không nhất quán; không thể giải phóng an toàn.");
            }
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            reservation.setReleasedAt(LocalDateTime.now());
        }
        lockedSession.setStatus(terminalStatus);
    }

    @Scheduled(fixedDelayString = "${checkout.expiry-scan-millis:60000}")
    @Transactional
    public void expireCheckouts() {
        LocalDateTime now = LocalDateTime.now();
        checkoutRepository.findByStatusAndExpiresAtLessThanEqualOrderById(CheckoutStatus.ACTIVE, now)
                .forEach(session -> releaseCheckout(session, CheckoutStatus.EXPIRED));
        expireUnpaidOrders(now);
    }

    private void expireUnpaidOrders(LocalDateTime now) {
        List<Order> expired = orderRepository.findByPaymentStatusAndPaymentMethodAndPaymentExpiresAtLessThanEqual(
                PaymentStatus.PENDING, PaymentMethod.VNPAY, now
        );
        for (Order candidate : expired) {
            Order order = orderRepository.findByIdForUpdate(candidate.getId()).orElseThrow();
            Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId()).orElse(null);
            if (order.getPaymentStatus() != PaymentStatus.PENDING
                    || order.getStatus() == OrderStatus.CANCELLED
                    || order.getPaymentExpiresAt() == null
                    || order.getPaymentExpiresAt().isAfter(now)
                    || (payment != null && payment.getStatus() == TransactionStatus.SUCCESS)) continue;

            if (payment == null) {
                log.error("No payment row exists for pending VNPay order {}; stock release is blocked", order.getOrderCode());
                continue;
            }
            VnPayService.TransactionCheck transactionCheck;
            try {
                transactionCheck = vnPayService.queryTransaction(payment);
            } catch (IllegalStateException ex) {
                log.warn("VNPay status is unknown for expired order {}; keep stock until next check: {}",
                        order.getOrderCode(), ex.getMessage());
                continue;
            }
            if (transactionCheck.paid()) {
                payment.setStatus(TransactionStatus.SUCCESS);
                payment.setGatewayTransactionNo(transactionCheck.transactionNo());
                payment.setBankCode(transactionCheck.bankCode());
                payment.setResponseCode("00");
                payment.setPaymentDate(now);
                order.setPaymentStatus(PaymentStatus.PAID);
                continue;
            }

            cancelUnpaidVnPayOrder(order, payment, "TIMEOUT",
                    "Tự động hủy: hết hạn thanh toán VNPay; sản phẩm đã được trả về giỏ hàng.");
        }
    }

    @Transactional
    public void cancelUnpaidVnPayOrder(Order order, Payment payment, String responseCode, String note) {
        if (order.getPaymentMethod() != PaymentMethod.VNPAY
                || order.getPaymentStatus() != PaymentStatus.PENDING
                || order.getStatus() == OrderStatus.CANCELLED
                || payment.getStatus() == TransactionStatus.SUCCESS) {
            return;
        }

        User cartUser = userRepository.findByIdForUpdate(order.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy khách hàng để khôi phục giỏ hàng."));
        Cart cart = cartRepository.findByUserIdAndBranchId(cartUser.getId(), order.getBranch().getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(cartUser);
                    newCart.setBranch(order.getBranch());
                    return cartRepository.save(newCart);
                });

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderById(order.getId()).stream()
                .sorted(Comparator.comparing(item -> item.getProductVariant().getId()))
                .toList();
        for (OrderItem item : items) {
            BranchInventory inventory = inventoryRepository.findByBranchIdAndVariantIdForUpdate(
                            order.getBranch().getId(), item.getProductVariant().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy tồn kho để khôi phục sản phẩm vào giỏ hàng."));
            int before = inventory.getQuantity();
            int after = Math.addExact(before, item.getQuantity());
            inventory.setQuantity(after);
            inventoryTransactionRepository.save(InventoryTransaction.create(
                    inventory.getVariant(), inventory.getBranch(), InventoryTransactionType.ORDER_RELEASED,
                    before, after, order.getOrderCode(), note, cartUser
            ));

            CartItem cartItem = cartItemRepository.findByCartIdAndInventoryId(cart.getId(), inventory.getId())
                    .orElseGet(() -> {
                        CartItem newItem = new CartItem();
                        newItem.setCart(cart);
                        newItem.setInventory(inventory);
                        newItem.setQuantity(0);
                        return newItem;
                    });
            cartItem.setQuantity(Math.addExact(cartItem.getQuantity(), item.getQuantity()));
            cartItemRepository.save(cartItem);
        }

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(order.getStatus().name());
        history.setNewStatus(OrderStatus.CANCELLED.name());
        history.setNote(note);
        orderHistoryRepository.save(history);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        payment.setStatus(TransactionStatus.FAILED);
        payment.setResponseCode(responseCode);
        shipmentRepository.findByOrderId(order.getId()).ifPresent(shipment ->
                shipment.setStatus(ShipmentStatus.CANCELLED));
    }

    private CheckoutSession lockActive(Long checkoutId, Long userId) {
        CheckoutSession session = checkoutRepository.findForUpdate(checkoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên thanh toán."));
        if (session.getStatus() != CheckoutStatus.ACTIVE || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Phiên thanh toán đã hết hạn.");
        }
        return session;
    }

    private List<CartItem> lockAndValidateItems(CheckoutSession session) {
        Cart cart = cartRepository.findByIdForUpdateAndUserId(session.getCart().getId(), session.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giỏ hàng."));
        List<CartItem> items = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getInventory().getId()))
                .toList();
        if (items.isEmpty()) throw new IllegalArgumentException("Giỏ hàng không còn sản phẩm.");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            BranchInventory inventory = inventoryRepository.findByIdForUpdate(item.getInventory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không còn tồn tại."));
            validateSellable(inventory);
            InventoryReservation reservation = reservationRepository
                    .findByCheckoutSessionIdAndInventoryIdAndConvertedToOrderFalse(
                            session.getId(), inventory.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt giữ hàng của phiên Checkout."));
            if (reservation.getExpiresAt().isBefore(LocalDateTime.now())
                    || reservation.getQuantity() != item.getQuantity()
                    || inventory.getReservedQuantity() < item.getQuantity()
                    || inventory.getQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Không còn đủ số lượng hàng đã giữ. Vui lòng bắt đầu Checkout lại.");
            }
            subtotal = subtotal.add(currentPrice(inventory).multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        if (subtotal.compareTo(session.getSubtotal()) != 0) {
            session.setSubtotal(subtotal);
            session.setDiscountCode(null);
            session.setDiscountCodeValue(null);
            session.setDiscountAppliedToShipping(false);
            session.setDiscountAmount(BigDecimal.ZERO);
            session.setGhnQuoteToken(null);
            throw new CheckoutPriceChangedException("Giá sản phẩm đã thay đổi. Vui lòng kiểm tra lại Checkout.");
        }
        return items;
    }

    private void validateSellable(BranchInventory inventory) {
        if (inventory.getBranch().getStatus() != example.com.ltw.branch.entity.BranchStatus.ACTIVE
                || inventory.getVariant().getStatus() != ProductVariantStatus.ACTIVE
                || inventory.getVariant().getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Sản phẩm hoặc chi nhánh hiện không kinh doanh.");
        }
    }

    private void setAddressSnapshot(CheckoutSession session, Address address) {
        session.setShippingAddressId(address.getId());
        session.setReceiverName(address.getReceiverName());
        session.setReceiverPhone(address.getPhone());
        session.setShippingProvince(address.getProvince());
        session.setShippingProvinceId(address.getProvinceId());
        session.setShippingDistrict(address.getDistrict());
        session.setShippingDistrictId(address.getDistrictId());
        session.setShippingWard(address.getWard());
        session.setShippingWardCode(address.getWardCode());
        session.setShippingStreetAddress(address.getStreetAddress());
        session.setGhnQuoteToken(null);
    }

    private BigDecimal calculateDiscount(
            CheckoutSession session, List<CartItem> items, String code, BigDecimal shippingFee
    ) {
        session.setDiscountCode(null);
        session.setDiscountCodeValue(null);
        if (code == null || code.isBlank()) return BigDecimal.ZERO;
        DiscountCode discount = discountCodeRepository.findByCodeForUpdate(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));
        validateDiscount(discount, session, items, session.getUser().getId());
        BigDecimal eligibleSubtotal = eligibleSubtotal(discount, items);
        BigDecimal discountBase = eligibleSubtotal.add(discount.isAppliesToShipping() ? shippingFee : BigDecimal.ZERO);
        BigDecimal result = discount.getType() == DiscountType.PERCENTAGE
                ? discountBase.multiply(discount.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
                : discount.getDiscountValue();
        if (discount.getMaximumDiscount() != null) result = result.min(discount.getMaximumDiscount());
        result = result.min(discountBase).max(BigDecimal.ZERO);
        session.setDiscountCode(discount);
        session.setDiscountCodeValue(discount.getCode());
        session.setDiscountAppliedToShipping(discount.isAppliesToShipping());
        return result;
    }

    private void validateDiscount(DiscountCode discount, CheckoutSession session, List<CartItem> items, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (discount.getStatus() != DiscountStatus.ACTIVE
                || now.isBefore(discount.getStartAt()) || now.isAfter(discount.getEndAt())
                || discount.getQuantity() <= 0) {
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn, bị tắt hoặc hết lượt sử dụng.");
        }
        if (discount.getType() == DiscountType.PERCENTAGE
                && (discount.getDiscountValue().signum() <= 0
                || discount.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
                || discount.getType() == DiscountType.FIXED_AMOUNT && discount.getDiscountValue().signum() <= 0) {
            throw new IllegalArgumentException("Cấu hình giá trị mã giảm giá không hợp lệ.");
        }
        BigDecimal eligibleSubtotal = eligibleSubtotal(discount, items);
        if (eligibleSubtotal.signum() <= 0) {
            throw new IllegalArgumentException("Mã giảm giá không áp dụng cho sản phẩm trong giỏ.");
        }
        if (eligibleSubtotal.compareTo(discount.getMinimumOrderAmount()) < 0) {
            throw new IllegalArgumentException("Giá trị sản phẩm áp dụng mã chưa đạt mức tối thiểu.");
        }
        if (discount.getPerCustomerLimit() != null
                && discountUsageRepository.countByDiscountCodeIdAndUserId(discount.getId(), userId)
                >= discount.getPerCustomerLimit()) {
            throw new IllegalArgumentException("Bạn đã sử dụng hết số lượt cho mã giảm giá này.");
        }
    }

    private BigDecimal eligibleSubtotal(DiscountCode discount, List<CartItem> items) {
        return items.stream()
                .filter(item -> discount.getEligibleProducts().isEmpty()
                        && discount.getEligibleCategories().isEmpty()
                        || discount.getEligibleProducts().contains(item.getInventory().getVariant().getProduct())
                        || discount.getEligibleCategories().contains(item.getInventory().getVariant().getProduct().getCategory()))
                .map(item -> currentPrice(item.getInventory()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Order createOrder(
            CheckoutSession session, List<CartItem> items, DiscountCode discountCode, PaymentMethod method
    ) {
        Order order = new Order();
        order.setOrderCode(session.getOrderCode());
        order.setCheckoutSession(session);
        order.setUser(session.getUser());
        order.setBranch(session.getCart().getBranch());
        order.setDiscountCode(discountCode);
        order.setSubtotal(session.getSubtotal());
        order.setDiscountAmount(session.getDiscountAmount());
        order.setDiscountAppliedToShipping(session.isDiscountAppliedToShipping());
        order.setShippingFee(session.getShippingFee());
        order.setFinalAmount(session.getFinalAmount());
        order.setPaymentMethod(method);
        order.setPaymentStatus(method == PaymentMethod.VNPAY ? PaymentStatus.PENDING : PaymentStatus.UNPAID);
        order.setReceiverName(session.getReceiverName());
        order.setReceiverPhone(session.getReceiverPhone());
        order.setShippingProvince(session.getShippingProvince());
        order.setShippingProvinceId(session.getShippingProvinceId());
        order.setShippingDistrict(session.getShippingDistrict());
        order.setShippingDistrictId(session.getShippingDistrictId());
        order.setShippingWard(session.getShippingWard());
        order.setShippingWardCode(session.getShippingWardCode());
        order.setShippingStreetAddress(session.getShippingStreetAddress());
        if (method == PaymentMethod.VNPAY) order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(vnPayTimeoutMinutes));
        orderRepository.save(order);

        BigDecimal eligibleSubtotal = discountCode == null ? BigDecimal.ZERO : eligibleSubtotal(discountCode, items);
        BigDecimal discountBase = eligibleSubtotal.add(
                discountCode != null && discountCode.isAppliesToShipping() ? order.getShippingFee() : BigDecimal.ZERO);
        BigDecimal merchandiseDiscount = discountBase.signum() == 0 ? BigDecimal.ZERO
                : order.getDiscountAmount().multiply(eligibleSubtotal)
                        .divide(discountBase, 2, RoundingMode.DOWN);
        List<OrderItem> createdItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            BranchInventory inventory = cartItem.getInventory();
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductVariant(inventory.getVariant());
            item.setProductNameSnapshot(inventory.getVariant().getProduct().getName());
            item.setVariantSnapshot(inventory.getVariant().getRam() + " / "
                    + inventory.getVariant().getStorage() + " / " + inventory.getVariant().getColor());
            item.setImageUrlSnapshot(inventory.getVariant().getProduct().getImages().stream()
                    .filter(image -> image.isThumbnail())
                    .findFirst()
                    .or(() -> inventory.getVariant().getProduct().getImages().stream().findFirst())
                    .map(example.com.ltw.catalog.entity.ProductImage::getImageUrl)
                    .orElse(null));
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(currentPrice(inventory));
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            createdItems.add(item);
        }
        BigDecimal allocatedDiscount = BigDecimal.ZERO;
        List<OrderItem> discountEligibleItems = createdItems.stream()
                .filter(item -> discountCode != null && (discountCode.getEligibleProducts().isEmpty()
                        && discountCode.getEligibleCategories().isEmpty()
                        || discountCode.getEligibleProducts().contains(item.getProductVariant().getProduct())
                        || discountCode.getEligibleCategories().contains(item.getProductVariant().getProduct().getCategory())))
                .toList();
        for (int index = 0; index < createdItems.size(); index++) {
            OrderItem item = createdItems.get(index);
            boolean eligible = discountEligibleItems.contains(item);
            BigDecimal lineDiscount = BigDecimal.ZERO;
            if (eligible && eligibleSubtotal.signum() > 0) {
                if (discountEligibleItems.get(discountEligibleItems.size() - 1) == item) {
                    lineDiscount = merchandiseDiscount.subtract(allocatedDiscount).max(BigDecimal.ZERO);
                } else {
                    lineDiscount = merchandiseDiscount.multiply(item.getSubtotal())
                            .divide(eligibleSubtotal, 2, RoundingMode.DOWN);
                    allocatedDiscount = allocatedDiscount.add(lineDiscount);
                }
            }
            item.setDiscountAmount(lineDiscount);
            orderItemRepository.save(item);
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setNewStatus(OrderStatus.PENDING.name());
        history.setNote("Đơn hàng được tạo từ Checkout.");
        orderHistoryRepository.save(history);
        return order;
    }

    private BigDecimal currentPrice(BranchInventory inventory) {
        BigDecimal salePrice = inventory.getVariant().getSalePrice();
        return salePrice != null && salePrice.signum() > 0
                && salePrice.compareTo(inventory.getVariant().getPrice()) < 0
                ? salePrice
                : inventory.getVariant().getPrice();
    }

    private void validateHoldConfiguration() {
        if (holdDurationMinutes <= 0) {
            throw new IllegalStateException("Chưa cấu hình checkout.hold-duration-minutes; không thể giữ tồn kho.");
        }
    }

    public record FinalizeResult(String orderCode, String paymentUrl, boolean requiresRequote, String message) {
    }

    public static class CheckoutPriceChangedException extends RuntimeException {
        public CheckoutPriceChangedException(String message) {
            super(message);
        }
    }
}
