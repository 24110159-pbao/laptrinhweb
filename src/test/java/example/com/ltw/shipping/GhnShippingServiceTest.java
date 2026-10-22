package example.com.ltw.shipping;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.shipping.dto.GhnCreateOrderResponse;
import example.com.ltw.shipping.service.GhnShippingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class GhnShippingServiceTest {

    private GhnShippingService ghnShippingService;

    @BeforeEach
    public void setUp() {
        ghnShippingService = new GhnShippingService();
        ReflectionTestUtils.setField(ghnShippingService, "baseUrl", "https://online-gateway.ghn.vn/shiip/public-api");
        ReflectionTestUtils.setField(ghnShippingService, "token", "169841b3-b1a1-11f1-92df-d64e82653d52");
        ReflectionTestUtils.setField(ghnShippingService, "shopId", 4817789);
        ReflectionTestUtils.setField(ghnShippingService, "defaultDistrictId", 1462);
        ReflectionTestUtils.setField(ghnShippingService, "defaultWardCode", "21612");
    }

    @Test
    public void testCreateShippingOrder_returnsTrackingCode() {
        Order order = new Order();
        order.setOrderCode("ORD-TEST-2026-9999");
        order.setReceiverName("Nguyễn Văn Test");
        order.setReceiverPhone("0373703896");
        order.setShippingStreetAddress("206 Điện Biên Phủ, Phường 17");
        order.setShippingDistrictId(1462);
        order.setShippingWardCode("21612");
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setFinalAmount(new BigDecimal("250000"));

        OrderItem item = new OrderItem();
        item.setProductNameSnapshot("Cáp sạc USB-C NexusTech");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("250000"));
        item.setSubtotal(new BigDecimal("250000"));

        List<OrderItem> items = Collections.singletonList(item);

        GhnCreateOrderResponse resp = ghnShippingService.createShippingOrder(order, items);

        Assertions.assertNotNull(resp);
        Assertions.assertTrue(resp.isSuccess());
        Assertions.assertNotNull(resp.getOrderCode());
        Assertions.assertFalse(resp.getOrderCode().isBlank());
        System.out.println("GHN Result Order Code: " + resp.getOrderCode());
        System.out.println("GHN Result Message: " + resp.getMessage());
    }
}
