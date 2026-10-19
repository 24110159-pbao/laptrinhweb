package example.com.ltw.payment.service;

import example.com.ltw.payment.entity.Payment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VnPayService {

    private static final DateTimeFormatter VN_PAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final String tmnCode;
    private final String hashSecret;
    private final String paymentUrl;
    private final String returnUrl;
    private final String queryUrl;
    private final RestClient restClient;

    public VnPayService(
            @Value("${vnpay.tmn-code:}") String tmnCode,
            @Value("${vnpay.hash-secret:}") String hashSecret,
            @Value("${vnpay.payment-url:}") String paymentUrl,
            @Value("${vnpay.return-url:}") String returnUrl,
            @Value("${vnpay.query-url:}") String queryUrl
    ) {
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.paymentUrl = paymentUrl;
        this.returnUrl = returnUrl;
        this.queryUrl = queryUrl;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String createPaymentUrl(String orderCode, BigDecimal amount, String clientIp, LocalDateTime expiresAt) {
        validateConfiguration();
        validateQueryConfiguration();
        long vndAmount;
        try {
            vndAmount = amount.longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("VNPay chỉ hỗ trợ số tiền VND nguyên; tổng tiền đơn hàng không hợp lệ.", ex);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!expiresAt.isAfter(now)) {
            throw new IllegalStateException("Thời hạn thanh toán VNPay đã hết.");
        }
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", tmnCode);
        parameters.put("vnp_Amount", Long.toString(Math.multiplyExact(vndAmount, 100)));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", orderCode);
        parameters.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", returnUrl + (returnUrl.contains("?") ? "&" : "?")
                + "orderCode=" + urlEncode(orderCode));
        parameters.put("vnp_IpAddr", normalizeIp(clientIp));
        parameters.put("vnp_CreateDate", VN_PAY_TIME.format(now.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))));
        parameters.put("vnp_ExpireDate", VN_PAY_TIME.format(expiresAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))));
        String query = encode(parameters);
        return paymentUrl + "?" + query + "&vnp_SecureHash=" + hmacSha512(hashSecret, query);
    }

    public boolean verify(MultiValueMap<String, String> parameters) {
        validateConfiguration();
        String received = parameters.getFirst("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;
        Map<String, String> signed = new TreeMap<>();
        parameters.forEach((key, values) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType") && values != null && !values.isEmpty()) {
                signed.put(key, values.getFirst());
            }
        });
        byte[] expected = hmacSha512(hashSecret, encode(signed)).toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = received.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    public TransactionCheck queryTransaction(Payment payment) {
        validateConfiguration();
        validateQueryConfiguration();
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> request = new TreeMap<>();
        request.put("vnp_Version", "2.1.0");
        request.put("vnp_Command", "querydr");
        request.put("vnp_TmnCode", tmnCode);
        request.put("vnp_TxnRef", payment.getTransactionNo());
        request.put("vnp_OrderInfo", "Truy van giao dich " + payment.getTransactionNo());
        request.put("vnp_TransactionDate", VN_PAY_TIME.format(payment.getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))));
        request.put("vnp_CreateDate", VN_PAY_TIME.format(now.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))));
        request.put("vnp_IpAddr", "127.0.0.1");
        String requestHash = hmacSha512(hashSecret, encode(request));
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        request.forEach(body::add);
        body.add("vnp_SecureHash", requestHash);
        Map<String, String> response;
        try {
            response = restClient.post().uri(queryUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            throw new IllegalStateException("Không thể xác minh trạng thái giao dịch VNPay trước khi giải phóng tồn kho.", ex);
        }
        if (response == null || !verify(response)) {
            throw new IllegalStateException("Phản hồi truy vấn trạng thái VNPay không hợp lệ.");
        }
        if (!payment.getTransactionNo().equals(response.get("vnp_TxnRef"))) {
            throw new IllegalStateException("VNPay trả về mã đơn hàng không khớp.");
        }
        if ((response.get("vnp_CurrCode") != null && !"VND".equals(response.get("vnp_CurrCode")))
                || !Long.toString(Math.multiplyExact(payment.getAmount().longValueExact(), 100L))
                .equals(response.get("vnp_Amount"))) {
            throw new IllegalStateException("VNPay trả về số tiền giao dịch không khớp.");
        }
        if ("00".equals(response.get("vnp_ResponseCode"))) {
            String status = response.get("vnp_TransactionStatus");
            if ("00".equals(status)) {
                if (response.get("vnp_TransactionNo") == null || response.get("vnp_TransactionNo").isBlank()) {
                    throw new IllegalStateException("VNPay xác nhận thành công nhưng thiếu mã giao dịch.");
                }
                return new TransactionCheck(true, response.get("vnp_TransactionNo"), response.get("vnp_BankCode"));
            }
            if ("01".equals(status)) return new TransactionCheck(false, null, null);
        }
        throw new IllegalStateException("VNPay chưa xác nhận được trạng thái giao dịch.");
    }

    public boolean verify(Map<String, String> parameters) {
        validateConfiguration();
        String received = parameters.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;
        Map<String, String> signed = new TreeMap<>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType") && value != null) signed.put(key, value);
        });
        return MessageDigest.isEqual(
                hmacSha512(hashSecret, encode(signed)).toLowerCase().getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    private void validateConfiguration() {
        if (tmnCode.isBlank() || hashSecret.isBlank() || paymentUrl.isBlank() || returnUrl.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình bắt buộc của VNPay Sandbox.");
        }
    }

    private void validateQueryConfiguration() {
        if (queryUrl.isBlank()) throw new IllegalStateException("Chưa cấu hình vnpay.query-url.");
    }

    private String encode(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể ký yêu cầu VNPay.", ex);
        }
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) return "127.0.0.1";
        return ip;
    }

    public record TransactionCheck(boolean paid, String transactionNo, String bankCode) {
    }
}
