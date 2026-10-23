package example.com.ltw.review.service;

import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.review.entity.Review;
import example.com.ltw.review.repository.ReviewRepository;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ReviewMediaStorageService mediaStorage;

    @InjectMocks
    private ReviewService reviewService;

    private User customer;
    private Product product;
    private Order order;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(5L);
        product = new Product();
        order = new Order();
        order.setId(8L);
        order.setOrderCode("ORD-1");
        order.setUser(customer);
        order.setStatus(OrderStatus.DELIVERED);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        item = new OrderItem();
        item.setId(13L);
        item.setOrder(order);
        item.setProductVariant(variant);

    }

    @Test
    void createsReviewForOwnedDeliveredOrderItem() {
        stubOwnedItem();
        when(reviewRepository.findByOrderItemId(13L)).thenReturn(Optional.empty());
        when(shipmentRepository.existsByOrderIdAndStatusAndDeliveredAtIsNotNull(8L, ShipmentStatus.DELIVERED))
                .thenReturn(true);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = reviewService.submit(5L, "ORD-1", 13L, null, 5, "  Hài lòng  ", null,
                null, false, false);

        assertEquals(5, saved.getRating());
        assertEquals("Hài lòng", saved.getComment());
        assertEquals(product, saved.getProduct());
        assertEquals(item, saved.getOrderItem());
        verify(orderItemRepository).findForReview(13L);
    }

    @Test
    void rejectsReviewIfShipmentWasNotSuccessfullyDelivered() {
        stubOwnedItem();
        when(reviewRepository.findByOrderItemId(13L)).thenReturn(Optional.empty());
        when(shipmentRepository.existsByOrderIdAndStatusAndDeliveredAtIsNotNull(8L, ShipmentStatus.DELIVERED))
                .thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 4, null, null, null, false, false
        ));

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsItemOutsideCustomersOrder() {
        when(orderItemRepository.findForReview(13L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 4, null, null, null, false, false
        ));
    }

    @Test
    void rejectsInvalidRatingAndCommentLength() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 0, null, null, null, false, false
        ));
        assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 5, "x".repeat(501), null, null, false, false
        ));
        verify(orderItemRepository, never()).findForReview(any());
    }

    @Test
    void editsExistingRecordWithoutChangingOrderItemOrAddingAnotherRecord() {
        stubOwnedItem();
        Review existing = new Review();
        existing.setId(42L);
        existing.setUser(customer);
        existing.setOrderItem(item);
        existing.setProduct(product);
        existing.setMedia(new ArrayList<>());
        when(reviewRepository.findByOrderItemId(13L)).thenReturn(Optional.of(existing));
        when(reviewRepository.saveAndFlush(existing)).thenReturn(existing);

        Review result = reviewService.submit(5L, "ORD-1", 13L, 42L, 3, "Updated", null,
                null, false, false);

        assertEquals(42L, result.getId());
        assertEquals(item, result.getOrderItem());
        assertEquals(3, result.getRating());
        assertEquals("Updated", result.getComment());
        verify(reviewRepository).saveAndFlush(existing);
        verify(shipmentRepository, never()).existsByOrderIdAndStatusAndDeliveredAtIsNotNull(
                8L, ShipmentStatus.DELIVERED
        );
    }

    @Test
    void secondCreateRequestForReviewedItemGetsAlreadyReviewedError() {
        stubOwnedItem();
        Review existing = new Review();
        existing.setId(42L);
        existing.setUser(customer);
        existing.setOrderItem(item);
        when(reviewRepository.findByOrderItemId(13L)).thenReturn(Optional.of(existing));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 4, null, null, null, false, false
        ));

        assertEquals("Dòng sản phẩm này đã được đánh giá.", error.getMessage());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void refusesUncheckedAttachmentPolicy() {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "media", "image.png", "image/png", new byte[]{1, 2, 3}
        );

        assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 5, null, List.of(file), null, false, false
        ));
        verify(mediaStorage, never()).upload(any());
    }

    @Test
    void limitsNewAttachmentsToFivePerReview() {
        List<org.springframework.web.multipart.MultipartFile> files = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> (org.springframework.web.multipart.MultipartFile)
                        new org.springframework.mock.web.MockMultipartFile("media", "f" + index, "image/png", new byte[]{1}))
                .toList();

        assertThrows(IllegalArgumentException.class, () -> reviewService.submit(
                5L, "ORD-1", 13L, null, 5, null, files, null, false, true
        ));
        verify(mediaStorage, never()).upload(any());
    }

    private void stubOwnedItem() {
        when(orderItemRepository.findForReview(13L)).thenReturn(Optional.of(item));
    }
}
