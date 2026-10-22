package example.com.ltw.payment.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.payment.service.VnPayCallbackService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayCallbackService callbackService;
    private final OrderRepository orderRepository;

    @PostMapping(value = "/payment/vnpay-ipn", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public Map<String, String> ipn(@RequestParam MultiValueMap<String, String> parameters) {
        return callbackService.handle(parameters);
    }

    @GetMapping("/payment/vnpay-return")
    public String returnFromGateway(
            @RequestParam(required = false) String orderCode,
            @RequestParam MultiValueMap<String, String> parameters,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Map<String, String> callbackResult = null;
        if (parameters.getFirst("vnp_SecureHash") != null) {
            callbackResult = callbackService.handle(parameters);
        }
        String responseCode = parameters.getFirst("vnp_ResponseCode");
        String transactionStatus = parameters.getFirst("vnp_TransactionStatus");
        boolean cancelledOrFailed = "24".equals(responseCode)
                || (responseCode != null && !"00".equals(responseCode))
                || "02".equals(transactionStatus);
        if (callbackResult != null && "00".equals(callbackResult.get("RspCode"))
                && cancelledOrFailed) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "24".equals(responseCode)
                            ? "Đã hủy thanh toán thành công. Không có đơn hàng nào được ghi nhận."
                            : "Thanh toán không thành công; không có đơn hàng nào được ghi nhận và sản phẩm đã được trả vào giỏ hàng.");
            return "redirect:/cart";
        }
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        Order order = orderCode == null ? null
                : orderRepository.findByOrderCodeAndUserId(orderCode, userId).orElse(null);
        if (order == null) {
            if (callbackResult != null && "00".equals(callbackResult.get("RspCode"))
                    && "00".equals(responseCode) && "00".equals(transactionStatus)) {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Thanh toán được xác nhận nhưng phiên Checkout không còn hiệu lực; yêu cầu hoàn tiền đang cần được xử lý.");
            }
            return "redirect:/cart";
        }
        model.addAttribute("order", order);
        model.addAttribute("paymentStatus", order.getPaymentStatus());
        model.addAttribute("paymentMessage", switch (order.getPaymentStatus()) {
            case PAID -> "Thanh toán đã được hệ thống xác minh thành công.";
            case REFUND_REQUIRED -> "Giao dịch thành công đến sau khi đơn đã hết hạn; yêu cầu hoàn tiền đang cần được xử lý.";
            case FAILED -> "Thanh toán đã bị hủy hoặc không thành công; đơn đã hủy và sản phẩm đã được trả về giỏ hàng.";
            default -> "Đang chờ backend xác minh kết quả thanh toán từ VNPay.";
        });
        return "checkout/payment-result";
    }

    @GetMapping("/payment/result")
    public String codResult(
            @RequestParam String orderCode,
            HttpSession session,
            Model model
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        Order order = orderRepository.findByOrderCodeAndUserId(orderCode, userId).orElse(null);
        if (order == null) return "redirect:/cart";
        model.addAttribute("order", order);
        model.addAttribute("paymentStatus", order.getPaymentStatus());
        model.addAttribute("paymentMessage", "Đơn hàng đã được tạo. Thanh toán COD khi nhận hàng.");
        return "checkout/payment-result";
    }

    private Long currentUserId(HttpSession session) {
        Object currentUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return currentUser instanceof User user ? user.getId() : null;
    }
}
