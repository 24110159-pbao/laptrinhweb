package example.com.ltw.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public MailService(JavaMailSender mailSender, @Value("${spring.mail.username:trinhphuhao2108@gmail.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendOtpEmail(String toEmail, String otpCode, String purposeText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, "NEXUSTECH Flagship Store");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP (" + otpCode + ") - NEXUSTECH STORE");

            String htmlContent = buildOtpHtmlTemplate(toEmail, otpCode, purposeText);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent OTP email to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email xác thực. Vui lòng thử lại sau!", e);
        } catch (Exception e) {
            log.error("Unexpected error while sending mail: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi gửi email: " + e.getMessage(), e);
        }
    }

    private String buildOtpHtmlTemplate(String toEmail, String otpCode, String purposeText) {
        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <title>Mã OTP - NEXUSTECH</title>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background-color: #0b1120;
                        color: #f1f5f9;
                    }
                    .email-container {
                        max-width: 580px;
                        margin: 30px auto;
                        background: #0f172a;
                        border: 1px solid #1e293b;
                        border-radius: 16px;
                        overflow: hidden;
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
                    }
                    .header {
                        background: linear-gradient(135deg, #090e17 0%, #1e1e38 100%);
                        padding: 30px 40px;
                        text-align: center;
                        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
                    }
                    .logo-text {
                        font-size: 24px;
                        font-weight: 800;
                        letter-spacing: 2px;
                        color: #ffffff;
                        margin: 0;
                        text-transform: uppercase;
                    }
                    .logo-tagline {
                        font-size: 11px;
                        letter-spacing: 3px;
                        color: #38bdf8;
                        margin-top: 5px;
                        text-transform: uppercase;
                    }
                    .body {
                        padding: 40px;
                        text-align: center;
                    }
                    .title {
                        font-size: 20px;
                        font-weight: 600;
                        color: #ffffff;
                        margin-bottom: 12px;
                    }
                    .description {
                        font-size: 14px;
                        line-height: 1.6;
                        color: #94a3b8;
                        margin-bottom: 30px;
                    }
                    .otp-box {
                        background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
                        border: 2px dashed #0284c7;
                        border-radius: 12px;
                        padding: 20px;
                        margin: 0 auto 30px auto;
                        display: inline-block;
                        letter-spacing: 10px;
                        font-size: 36px;
                        font-weight: 800;
                        color: #38bdf8;
                        text-shadow: 0 0 15px rgba(56, 189, 248, 0.4);
                    }
                    .meta-info {
                        background: rgba(255, 255, 255, 0.03);
                        border-radius: 10px;
                        padding: 16px;
                        font-size: 13px;
                        color: #cbd5e1;
                        text-align: left;
                        line-height: 1.6;
                        margin-bottom: 30px;
                    }
                    .meta-info strong {
                        color: #f8fafc;
                    }
                    .warning {
                        font-size: 12px;
                        color: #f87171;
                        line-height: 1.5;
                    }
                    .footer {
                        background: #090e17;
                        padding: 24px 40px;
                        text-align: center;
                        border-top: 1px solid #1e293b;
                        font-size: 12px;
                        color: #64748b;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="logo-text">⚡ NEXUSTECH</div>
                        <div class="logo-tagline">FLAGSHIP STORE • VIP EXPERIENCE</div>
                    </div>
                    <div class="body">
                        <div class="title">XÁC THỰC BẢO MẬT TÀI KHOẢN</div>
                        <p class="description">
                            Xin chào <strong>{{EMAIL}}</strong>,<br>
                            Bạn vừa thực hiện yêu cầu <strong>{{PURPOSE}}</strong> trên hệ thống <strong>NEXUSTECH Flagship Store</strong>.
                            Dưới đây là mã xác thực OTP của bạn:
                        </p>
                        
                        <div class="otp-box">{{OTP}}</div>
                        
                        <div class="meta-info">
                            • <strong>Thời gian hiệu lực:</strong> 5 phút kể từ lúc nhận thư.<br>
                            • <strong>Mục đích:</strong> {{PURPOSE}}<br>
                            • <strong>Cảnh báo:</strong> Tuyệt đối không cung cấp mã OTP này cho bất kỳ ai, kể cả nhân viên hỗ trợ của NEXUSTECH.
                        </div>
                        
                        <p class="warning">
                            Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc đổi mật khẩu tài khoản ngay để đảm bảo an toàn.
                        </p>
                    </div>
                    <div class="footer">
                        &copy; 2026 NEXUSTECH Flagship Store. All rights reserved.<br>
                        Hệ thống bán lẻ thiết bị công nghệ chính hãng cao cấp hàng đầu Việt Nam.
                    </div>
                </div>
            </body>
            </html>
            """;
        return template
                .replace("{{EMAIL}}", toEmail)
                .replace("{{PURPOSE}}", purposeText)
                .replace("{{OTP}}", otpCode);
    }
}
