package example.com.ltw.shipping.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.checkout.entity.CheckoutSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class GhnShippingServiceTest {

    @Test
    void rejectsShippingQuoteWhenBranchGhnDataIsMissingInsteadOfReturningZero() {
        GhnShippingService service = new GhnShippingService(
                "https://dev-online-gateway.ghn.vn/shiip/public-api", "", 0, 0
        );

        assertThrows(IllegalStateException.class, () -> service.quote(
                mock(Branch.class), List.<CartItem>of(), mock(CheckoutSession.class)
        ));
    }
}
