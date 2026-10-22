package example.com.ltw.payment.service;

import example.com.ltw.checkout.service.CheckoutService;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.payment.entity.Payment;
import example.com.ltw.payment.entity.TransactionStatus;
import example.com.ltw.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnPayCallbackServiceTest {

    @Mock private VnPayService vnPayService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CheckoutService checkoutService;

    @InjectMocks private VnPayCallbackService callbackService;

    @Test
    void signedCancelledPaymentCancelsOrderWithoutTransactionNumber() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("vnp_TxnRef", "NX12");
        parameters.add("vnp_ResponseCode", "24");
        parameters.add("vnp_TransactionStatus", "02");
        parameters.add("vnp_Amount", "12000");
        parameters.add("vnp_CurrCode", "VND");

        Order order = new Order();
        order.setOrderCode("NX12");
        order.setFinalAmount(new BigDecimal("120.00"));
        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(TransactionStatus.PENDING);

        when(vnPayService.verify(parameters)).thenReturn(true);
        when(orderRepository.findByOrderCodeForUpdate("NX12")).thenReturn(Optional.of(order));
        when(paymentRepository.findByTransactionNoForUpdate("NX12")).thenReturn(Optional.of(payment));

        assertEquals("00", callbackService.handle(parameters).get("RspCode"));

        verify(checkoutService).cancelUnpaidVnPayOrder(
                org.mockito.ArgumentMatchers.same(order),
                org.mockito.ArgumentMatchers.same(payment),
                org.mockito.ArgumentMatchers.eq("24"),
                anyString()
        );
        assertEquals(TransactionStatus.PENDING, payment.getStatus());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
    }
}
