package example.com.ltw.shipping.service;

import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.shipping.repository.ShipmentEventRepository;
import example.com.ltw.shipping.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ShippingWebhookServiceTest {

    @Test
    void requiresConfiguredSecretAndExactPayloadSignature() throws Exception {
        ShippingWebhookService service = new ShippingWebhookService(
                mock(OrderRepository.class),
                mock(ShipmentRepository.class),
                mock(ShipmentEventRepository.class),
                mock(OrderStatusHistoryRepository.class)
        );
        String body = "{\"eventId\":\"evt-1\"}";
        assertFalse(service.verifySignature(body, "anything"));

        ReflectionTestUtils.setField(service, "webhookSecret", "test-secret");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        assertTrue(service.verifySignature(body, signature));
        assertFalse(service.verifySignature(body + " ", signature));
    }
}
