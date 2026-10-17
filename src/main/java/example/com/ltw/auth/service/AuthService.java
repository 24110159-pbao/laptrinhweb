package example.com.ltw.auth.service;

import example.com.ltw.auth.dto.LoginRequest;
import example.com.ltw.auth.dto.RegisterRequest;
import example.com.ltw.auth.dto.ResetPasswordRequest;
import example.com.ltw.auth.entity.EmailOtp;
import example.com.ltw.auth.entity.OtpPurpose;
import example.com.ltw.auth.repository.EmailOtpRepository;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.entity.UserStatus;
import example.com.ltw.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SESSION_CURRENT_USER = "currentUser";
    public static final String SESSION_PENDING_REGISTER = "pendingRegister";
    public static final String SESSION_PENDING_EMAIL = "pendingEmail";
    public static final String SESSION_RESET_EMAIL = "resetEmail";
    public static final String SESSION_LAST_OTP_SENT_TIME = "lastOtpSentTime";

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    /**
     * Start registration flow: validate -> generate OTP -> send email -> store pending data in session.
     */
    @Transactional
    public void startRegistration(RegisterRequest request, HttpSession session) {
        String email = request.getEmail().trim().toLowerCase();

        // Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        // Check if email already registered and verified
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && existingUser.get().isEmailVerified()) {
            throw new IllegalArgumentException("Email '" + email + "' đã được đăng ký trên hệ thống!");
        }

        // Generate 6-digit OTP
        String otpCode = generate6DigitOtp();

        // Save OTP to DB
        saveNewOtp(email, otpCode, OtpPurpose.REGISTER);

        // Send email
        mailService.sendOtpEmail(email, otpCode, "Đăng ký tài khoản NexusTech");

        // Save state into session
        session.setAttribute(SESSION_PENDING_REGISTER, request);
        session.setAttribute(SESSION_PENDING_EMAIL, email);
        session.setAttribute(SESSION_LAST_OTP_SENT_TIME, System.currentTimeMillis());

        log.info("Registration OTP initiated for {} with code: {}", email, otpCode);
    }

    /**
     * Resend OTP for pending registration.
     */
    @Transactional
    public void resendRegistrationOtp(HttpSession session) {
        String email = (String) session.getAttribute(SESSION_PENDING_EMAIL);
        if (email == null) {
            throw new IllegalStateException("Không tìm thấy phiên đăng ký. Vui lòng đăng ký lại từ đầu!");
        }

        Long lastSent = (Long) session.getAttribute(SESSION_LAST_OTP_SENT_TIME);
        if (lastSent != null && (System.currentTimeMillis() - lastSent) < 45_000) {
            long remainingSeconds = (45_000 - (System.currentTimeMillis() - lastSent)) / 1000;
            throw new IllegalArgumentException("Vui lòng đợi " + remainingSeconds + " giây nữa để gửi lại mã OTP!");
        }

        String otpCode = generate6DigitOtp();
        saveNewOtp(email, otpCode, OtpPurpose.REGISTER);
        mailService.sendOtpEmail(email, otpCode, "Gửi lại mã OTP đăng ký");

        session.setAttribute(SESSION_LAST_OTP_SENT_TIME, System.currentTimeMillis());
        log.info("Resent registration OTP to {}", email);
    }

    /**
     * Complete registration by verifying OTP code.
     */
    @Transactional
    public User completeRegistration(String email, String otpCode, HttpSession session) {
        email = email.trim().toLowerCase();

        // Validate OTP
        EmailOtp otp = validateOtpCode(email, otpCode, OtpPurpose.REGISTER);
        otp.setVerifiedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);

        // Retrieve pending registration info
        RegisterRequest registerRequest = (RegisterRequest) session.getAttribute(SESSION_PENDING_REGISTER);
        if (registerRequest == null) {
            throw new IllegalStateException("Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại!");
        }

        // Create or update user
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            user.setFullName(registerRequest.getFullName());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setPhone(registerRequest.getPhone());
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user = new User();
            user.setEmail(email);
            user.setFullName(registerRequest.getFullName());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setPhone(registerRequest.getPhone());
            user.setRole(Role.USER);
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            user.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80");
        }

        User savedUser = userRepository.save(user);

        // Log user in
        session.setAttribute(SESSION_CURRENT_USER, savedUser);
        session.removeAttribute(SESSION_PENDING_REGISTER);
        session.removeAttribute(SESSION_PENDING_EMAIL);

        log.info("User {} successfully registered and verified!", email);
        return savedUser;
    }

    /**
     * Standard Login
     */
    public User login(LoginRequest request, HttpSession session) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác!");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên!");
        }

        // Save to session
        session.setAttribute(SESSION_CURRENT_USER, user);
        log.info("User {} successfully logged in.", email);
        return user;
    }

    /**
     * Logout
     */
    public void logout(HttpSession session) {
        session.removeAttribute(SESSION_CURRENT_USER);
        session.invalidate();
    }

    /**
     * Forgot password - Send OTP
     */
    @Transactional
    public void startForgotPassword(String email, HttpSession session) {
        email = email.trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email này!"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa!");
        }

        String otpCode = generate6DigitOtp();
        saveNewOtp(email, otpCode, OtpPurpose.RESET_PASSWORD);
        mailService.sendOtpEmail(email, otpCode, "Khôi phục mật khẩu tài khoản");

        session.setAttribute(SESSION_RESET_EMAIL, email);
        session.setAttribute(SESSION_LAST_OTP_SENT_TIME, System.currentTimeMillis());
        log.info("Forgot password OTP sent to {}", email);
    }

    /**
     * Verify forgot password OTP and update password
     */
    @Transactional
    public void completeForgotPassword(ResetPasswordRequest request, HttpSession session) {
        String email = request.getEmail().trim().toLowerCase();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        EmailOtp otp = validateOtpCode(email, request.getOtp(), OtpPurpose.RESET_PASSWORD);
        otp.setVerifiedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại!"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        session.removeAttribute(SESSION_RESET_EMAIL);
        log.info("Password successfully reset for {}", email);
    }

    private void saveNewOtp(String email, String otpCode, OtpPurpose purpose) {
        // Find existing OTP if any
        Optional<EmailOtp> existingOtpOpt = emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose);
        int resendCount = 0;
        if (existingOtpOpt.isPresent()) {
            resendCount = existingOtpOpt.get().getResendCount() != null ? existingOtpOpt.get().getResendCount() + 1 : 1;
        }

        EmailOtp newOtp = new EmailOtp();
        newOtp.setEmail(email);
        newOtp.setOtpHash(passwordEncoder.encode(otpCode));
        newOtp.setPurpose(purpose);
        newOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        newOtp.setAttemptCount(0);
        newOtp.setResendCount(resendCount);

        emailOtpRepository.save(newOtp);
    }

    private EmailOtp validateOtpCode(String email, String otpCode, OtpPurpose purpose) {
        EmailOtp otp = emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã OTP cho email này. Vui lòng yêu cầu mã mới!"));

        if (otp.getVerifiedAt() != null) {
            throw new IllegalArgumentException("Mã OTP này đã được sử dụng. Vui lòng yêu cầu mã mới!");
        }

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn sau 5 phút. Vui lòng nhấn gửi lại mã!");
        }

        if (otp.getAttemptCount() >= 5) {
            throw new IllegalArgumentException("Bạn đã nhập sai mã quá 5 lần. Vui lòng yêu cầu mã OTP mới!");
        }

        if (!passwordEncoder.matches(otpCode, otp.getOtpHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            emailOtpRepository.save(otp);
            int remaining = 5 - otp.getAttemptCount();
            throw new IllegalArgumentException("Mã OTP không chính xác! Bạn còn " + remaining + " lần thử.");
        }

        return otp;
    }

    private String generate6DigitOtp() {
        int code = 100_000 + random.nextInt(900_000);
        return String.valueOf(code);
    }
}
