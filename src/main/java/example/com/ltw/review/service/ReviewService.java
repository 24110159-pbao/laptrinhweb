package example.com.ltw.review.service;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.review.entity.Review;
import example.com.ltw.review.entity.ReviewMedia;
import example.com.ltw.review.repository.ReviewRepository;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_MEDIA_COUNT = 5;

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReviewMediaStorageService mediaStorage;

    @Transactional
    public Review submit(
            Long userId,
            String orderCode,
            Long orderItemId,
            Long reviewId,
            Integer rating,
            String comment,
            List<MultipartFile> uploads,
            List<Long> retainedMediaIds,
            boolean mediaSelectionSubmitted,
            boolean policyAccepted
    ) {
        validateText(rating, comment);
        List<MultipartFile> files = uploads == null ? List.of()
                : uploads.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (files.size() > MAX_MEDIA_COUNT) {
            throw new IllegalArgumentException("Mỗi đánh giá chỉ được đính kèm tối đa 5 tệp.");
        }
        if (!files.isEmpty() && !policyAccepted) {
            throw new IllegalArgumentException("Hãy xác nhận tệp đính kèm tuân thủ chính sách sử dụng.");
        }

        OrderItem item = orderItemRepository.findForReview(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không thuộc đơn hàng của bạn."));
        User user = item.getOrder().getUser();
        if (!orderCode.equals(item.getOrder().getOrderCode()) || !userId.equals(user.getId())) {
            throw new IllegalArgumentException("Sản phẩm không thuộc đơn hàng của bạn.");
        }
        Order order = item.getOrder();
        Review review = reviewRepository.findByOrderItemId(item.getId()).orElse(null);
        boolean isNew = review == null;

        if (isNew) {
            if (reviewId != null) {
                throw new IllegalArgumentException("Không tìm thấy đánh giá cần chỉnh sửa.");
            }
            ensureDelivered(order);
            review = new Review();
            review.setUser(user);
            review.setOrderItem(item);
            review.setProduct(item.getProductVariant().getProduct());
        } else {
            if (reviewId == null) {
                throw new IllegalArgumentException("Dòng sản phẩm này đã được đánh giá.");
            }
            if (!review.getId().equals(reviewId) || !review.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa đánh giá này.");
            }
        }

        List<ReviewMedia> existingMedia = new ArrayList<>(review.getMedia());
        List<ReviewMedia> retainedMedia = chooseRetainedMedia(existingMedia, retainedMediaIds, mediaSelectionSubmitted);
        if (retainedMedia.size() + files.size() > MAX_MEDIA_COUNT) {
            throw new IllegalArgumentException("Mỗi đánh giá chỉ được đính kèm tối đa 5 tệp.");
        }

        List<ReviewMedia> uploaded = new ArrayList<>();
        List<ReviewMedia> removedMedia = existingMedia.stream()
                .filter(media -> !retainedMedia.contains(media))
                .toList();
        try {
            review.setRating(rating);
            review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
            if (isNew) {
                review = reviewRepository.saveAndFlush(review);
            }
            review.getMedia().clear();
            review.getMedia().addAll(retainedMedia);
            for (MultipartFile file : files) {
                ReviewMedia media = mediaStorage.upload(file);
                media.setReview(review);
                review.getMedia().add(media);
                uploaded.add(media);
            }
            Review saved = reviewRepository.saveAndFlush(review);
            scheduleMediaDeletion(removedMedia);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            cleanupUploaded(uploaded);
            throw new IllegalStateException("Dòng sản phẩm này đã được đánh giá.", ex);
        } catch (RuntimeException ex) {
            cleanupUploaded(uploaded);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Review> getReviewsForOrder(Long orderId, Long userId) {
        return reviewRepository.findByOrderItemOrderIdAndUserId(orderId, userId).stream()
                .collect(Collectors.toMap(review -> review.getOrderItem().getId(), review -> review));
    }

    @Transactional(readOnly = true)
    public List<Review> getProductReviews(Long productId) {
        return reviewRepository.findVisibleByProductId(productId);
    }

    @Transactional(readOnly = true)
    public ReviewStatistics getProductStatistics(Long productId) {
        return reviewRepository.findStatisticsByProductId(productId)
                .map(stats -> new ReviewStatistics(stats.getAverageRating(), stats.getReviewCount()))
                .orElseGet(() -> new ReviewStatistics(null, 0L));
    }

    private void ensureDelivered(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED
                || !shipmentRepository.existsByOrderIdAndStatusAndDeliveredAtIsNotNull(
                order.getId(), ShipmentStatus.DELIVERED)) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá sản phẩm sau khi đơn hàng được giao thành công.");
        }
    }

    private List<ReviewMedia> chooseRetainedMedia(
            List<ReviewMedia> existing,
            List<Long> retainedMediaIds,
            boolean selectionSubmitted
    ) {
        if (!selectionSubmitted) {
            return existing;
        }
        List<Long> requested = retainedMediaIds == null ? List.of() : retainedMediaIds;
        Set<Long> uniqueIds = new HashSet<>(requested);
        if (uniqueIds.size() != requested.size()) {
            throw new IllegalArgumentException("Danh sách tệp đánh giá không hợp lệ.");
        }
        Map<Long, ReviewMedia> existingById = existing.stream()
                .collect(Collectors.toMap(ReviewMedia::getId, media -> media));
        if (!existingById.keySet().containsAll(uniqueIds)) {
            throw new IllegalArgumentException("Không thể giữ lại tệp không thuộc đánh giá này.");
        }
        return requested.stream().map(existingById::get).toList();
    }

    private void validateText(Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao phải là số nguyên từ 1 đến 5.");
        }
        if (comment != null && comment.length() > 500) {
            throw new IllegalArgumentException("Bình luận không được vượt quá 500 ký tự.");
        }
    }

    private void cleanupUploaded(List<ReviewMedia> uploaded) {
        uploaded.forEach(media -> mediaStorage.delete(media.getPublicId(), media.getResourceType()));
    }

    private void scheduleMediaDeletion(List<ReviewMedia> removedMedia) {
        if (removedMedia.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                removedMedia.forEach(media -> mediaStorage.delete(media.getPublicId(), media.getResourceType()));
            }
        });
    }

    public record ReviewStatistics(Double averageRating, Long reviewCount) {
    }
}
