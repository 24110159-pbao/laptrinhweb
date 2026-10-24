package example.com.ltw.shipping.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.checkout.entity.CheckoutSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class GhnShippingServiceTest {

    @Test
    void testGhnShippingServiceInitializes() {
        GhnShippingService service = new GhnShippingService();
        assertNotNull(service);
    }
}
