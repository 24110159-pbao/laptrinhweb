package example.com.ltw.payment.service;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.checkout.service.CheckoutService;
import example.com.ltw.checkout.entity.CheckoutSession;
import example.com.ltw.checkout.repository.CheckoutSessionRepository;
import example.com.ltw.payment.entity.Payment;
import example.com.ltw.payment.entity.TransactionStatus;
import example.com.ltw.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayCallbackService {

    private final VnPayService vnPayService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CheckoutService checkoutService;
    private final CheckoutSessionRepository checkoutRepository;

    @Transactional
    public Map<String, String> handle(MultiValueMap<String, String> parameters) {
        if (!vnPayService.verify(parameters)) return result("97", "Invalid signature");

        String orderCode = parameters.getFirst("vnp_TxnRef");
        String responseCode = parameters.getFirst("vnp_ResponseCode");
        String transactionStatus = parameters.getFirst("vnp_TransactionStatus");
        String amountText = parameters.getFirst("vnp_Amount");
        if (orderCode == null || responseCode == null || transactionStatus == null || amountText == null) {
            return result("99", "Missing required fields");
        }

        Order order = orderRepository.findByOrderCodeForUpdate(orderCode).orElse(null);
        if (order == null) {
            CheckoutSession checkout = checkoutRepository.findByOrderCodeForUpdate(orderCode).orElse(null);
            if (checkout == null) return result("01", "Order not found");
            long amount;
            try {
                amount = Long.parseLong(amountText);
            } catch (NumberFormatException ex) {
                return result("04", "Invalid amount");
            }
            if (!"VND".equals(parameters.getFirst("vnp_CurrCode"))
                    || BigDecimal.valueOf(amount).compareTo(
                    checkout.getFinalAmount().multiply(BigDecimal.valueOf(100))) != 0) {
                return result("04", "Invalid amount");
            }
            boolean paid = "00".equals(responseCode) && "00".equals(transactionStatus);
            boolean failed = !"00".equals(responseCode) || "02".equals(transactionStatus);
            if (paid) {
                String gatewayTransaction = parameters.getFirst("vnp_TransactionNo");
                if (gatewayTransaction == null || gatewayTransaction.isBlank()) {
                    return result("99", "Missing transaction number");
                }
                checkoutService.completeVnPayCheckout(
                        checkout, gatewayTransaction, parameters.getFirst("vnp_BankCode"), responseCode
                );
            } else if (failed) {
                checkoutService.cancelVnPayCheckout(checkout);
            }
            return result("00", "Confirm Success");
        }
        Payment payment = paymentRepository.findByTransactionNoForUpdate(orderCode).orElse(null);
        if (payment == null) return result("01", "Order not found");
        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) return result("04", "Invalid payment method");

        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException ex) {
            return result("04", "Invalid amount");
        }
        if (!"VND".equals(parameters.getFirst("vnp_CurrCode"))
                || BigDecimal.valueOf(amount).compareTo(order.getFinalAmount().multiply(BigDecimal.valueOf(100))) != 0) {
            log.warn("VNPay amount mismatch for order {}", orderCode);
            return result("04", "Invalid amount");
        }
        boolean paid = "00".equals(responseCode) && "00".equals(transactionStatus);
        boolean failed = !"00".equals(responseCode) || "02".equals(transactionStatus);
        String gatewayTransaction = parameters.getFirst("vnp_TransactionNo");
        if (paid && (gatewayTransaction == null || gatewayTransaction.isBlank())) {
            return result("99", "Missing transaction number");
        }

        if (payment.getStatus() == TransactionStatus.SUCCESS) {
            if (paid && !gatewayTransaction.equals(payment.getGatewayTransactionNo())) {
                return result("04", "Transaction number mismatch");
            }
            return result("00", "Confirm Success");
        }
        if (!paid && !failed) return result("00", "Payment status is still pending");
        if (gatewayTransaction != null && !gatewayTransaction.isBlank()) {
            payment.setGatewayTransactionNo(gatewayTransaction);
        }
        payment.setBankCode(parameters.getFirst("vnp_BankCode"));
        payment.setResponseCode(responseCode);
        if (paid) {
            payment.setStatus(TransactionStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            if (order.getStatus() == OrderStatus.CANCELLED) {
                order.setPaymentStatus(PaymentStatus.REFUND_REQUIRED);
                log.error("Late VNPay success for cancelled order {}; manual refund is required", orderCode);
            } else {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        } else {
            checkoutService.cancelUnpaidVnPayOrder(order, payment, responseCode,
                    "Khách hàng hủy hoặc không hoàn tất thanh toán VNPay; sản phẩm đã được trả về giỏ hàng.");
        }
        return result("00", "Confirm Success");
    }

    private Map<String, String> result(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
