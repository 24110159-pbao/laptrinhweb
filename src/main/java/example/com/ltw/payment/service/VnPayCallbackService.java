package example.com.ltw.payment.service;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderRepository;
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
        if (order == null) return result("01", "Order not found");
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
        String gatewayTransaction = parameters.getFirst("vnp_TransactionNo");
        if (gatewayTransaction == null || gatewayTransaction.isBlank()) {
            return result("99", "Missing transaction number");
        }

        if (payment.getStatus() == TransactionStatus.SUCCESS) {
            if (!gatewayTransaction.equals(payment.getGatewayTransactionNo())) {
                return result("04", "Transaction number mismatch");
            }
            return result("00", "Confirm Success");
        }
        payment.setGatewayTransactionNo(gatewayTransaction);
        payment.setBankCode(parameters.getFirst("vnp_BankCode"));
        payment.setResponseCode(responseCode);
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            payment.setStatus(TransactionStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            if (order.getStatus() == OrderStatus.CANCELLED) {
                order.setPaymentStatus(PaymentStatus.REFUND_REQUIRED);
                log.error("Late VNPay success for cancelled order {}; manual refund is required", orderCode);
            } else {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        } else {
            payment.setStatus(TransactionStatus.FAILED);
        }
        return result("00", "Confirm Success");
    }

    private Map<String, String> result(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
