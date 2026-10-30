package example.com.ltw.order.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.OrderStatusHistory;
import example.com.ltw.order.entity.ReturnRequest;
import example.com.ltw.order.entity.ReturnRequestHistory;
import example.com.ltw.order.service.OrderService;
import example.com.ltw.order.service.ReturnRequestService;
import example.com.ltw.review.entity.Review;
import example.com.ltw.review.service.ReviewService;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.order.repository.ReturnRequestRepository;
import example.com.ltw.order.repository.ReturnRequestHistoryRepository;
import example.com.ltw.payment.entity.Payment;
import example.com.ltw.payment.repository.PaymentRepository;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.shipping.entity.ShipmentEvent;
import example.com.ltw.shipping.repository.ShipmentEventRepository;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestHistoryRepository returnHistoryRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final ReviewService reviewService;

    @GetMapping("/account/orders")
    public String orders(HttpSession session, Model model) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";

        List<ReturnRequest> userReturns = returnRequestRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        Map<Long, ReturnRequest> returnRequestMap = new HashMap<>();
        for (ReturnRequest r : userReturns) {
            if (r.getOrder() != null && !returnRequestMap.containsKey(r.getOrder().getId())) {
                returnRequestMap.put(r.getOrder().getId(), r);
            }
        }

        List<OrderView> views = orderService.customerOrders(user.getId()).stream()
                .map(order -> new OrderView(
                        order,
                        orderItemRepository.findByOrderIdOrderById(order.getId()),
                        returnRequestMap.get(order.getId())
                ))
                .toList();
        model.addAttribute("currentUser", user);
        model.addAttribute("orders", views);
        return "order/list";
    }

    @GetMapping("/account/orders/{orderCode}")
    public String detail(@PathVariable String orderCode, HttpSession session, Model model) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";
        Order order = orderService.customerOrder(orderCode, user.getId());
        model.addAttribute("currentUser", user);
        model.addAttribute("order", order);
        model.addAttribute("items", orderItemRepository.findByOrderIdOrderById(order.getId()));
        model.addAttribute("reviews", reviewService.getReviewsForOrder(order.getId(), user.getId()));
        model.addAttribute("history", historyRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId()));
        model.addAttribute("payment", paymentRepository.findByOrderId(order.getId()).orElse(null));
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
        model.addAttribute("shipment", shipment);
        model.addAttribute("canCreateReview", order.getStatus() == OrderStatus.DELIVERED
                && shipment != null
                && shipment.getStatus() == ShipmentStatus.DELIVERED
                && shipment.getDeliveredAt() != null);
        List<ReturnRequest> returns = returnRequestRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId());
        model.addAttribute("returns", returns);
        model.addAttribute("returnHistory", returns.stream().collect(java.util.stream.Collectors.toMap(
                ReturnRequest::getId,
                request -> returnHistoryRepository.findAllByReturnRequestIdOrderByCreatedAtAsc(request.getId())
        )));
        model.addAttribute("shipmentEvents", shipment == null ? List.of()
                : shipmentEventRepository.findAllByShipmentIdOrderByCreatedAtAsc(shipment.getId()));
        return "order/detail";
    }

    @PostMapping("/account/orders/{orderCode}/cancel")
    public String cancel(@PathVariable String orderCode, HttpSession session, RedirectAttributes flash) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";
        try {
            orderService.cancelByCustomer(orderCode, user);
            flash.addFlashAttribute("successMessage", "Đơn hàng đã được hủy. Nếu đã thanh toán, khoản hoàn tiền cần được xử lý riêng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/orders/" + orderCode;
    }

    @PostMapping("/account/orders/{orderCode}/return")
    public String requestReturn(
            @PathVariable String orderCode,
            @RequestParam Long orderItemId,
            @RequestParam int quantity,
            @RequestParam String reason,
            HttpSession session,
            RedirectAttributes flash
    ) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";
        try {
            returnRequestService.submit(orderCode, user,
                    List.of(new ReturnRequestService.ReturnLineInput(orderItemId, quantity)), reason);
            flash.addFlashAttribute("successMessage", "Yêu cầu trả hàng đã được gửi để xét duyệt.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/orders/" + orderCode;
    }

    @PostMapping("/account/orders/{orderCode}/items/{orderItemId}/review")
    public String submitReview(
            @PathVariable String orderCode,
            @PathVariable Long orderItemId,
            @RequestParam(required = false) Long reviewId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) List<MultipartFile> media,
            @RequestParam(required = false) List<Long> retainedMediaIds,
            @RequestParam(required = false, defaultValue = "false") boolean mediaSelectionSubmitted,
            HttpSession session,
            RedirectAttributes flash
    ) {
        User user = currentUser(session);
        if (user == null) return "redirect:/login";
        try {
            reviewService.submit(user.getId(), orderCode, orderItemId, reviewId, rating, comment, media,
                    retainedMediaIds, mediaSelectionSubmitted);
            flash.addFlashAttribute("successMessage", "Đánh giá của bạn đã được lưu và hiển thị.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/orders/" + orderCode;
    }

    private User currentUser(HttpSession session) {
        Object value = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return value instanceof User user ? user : null;
    }

    public record OrderView(Order order, List<OrderItem> items, ReturnRequest returnRequest) {
        public OrderView(Order order, List<OrderItem> items) {
            this(order, items, null);
        }

        public boolean hasReturnRequest() {
            return returnRequest != null;
        }
    }
}
