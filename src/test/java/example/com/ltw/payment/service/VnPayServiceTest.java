package example.com.ltw.payment.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class VnPayServiceTest {

    @Test
    void doesNotCreateVnPayOrderWithoutStatusQueryConfiguration() {
        VnPayService service = new VnPayService(
                "tmn", "secret", "https://sandbox.example/pay",
                "https://shop.example/payment/vnpay-return", ""
        );

        assertThrows(IllegalStateException.class, () -> service.createPaymentUrl(
                "NX123", BigDecimal.valueOf(1000), "127.0.0.1", LocalDateTime.now().plusMinutes(10)
        ));
    }
}
