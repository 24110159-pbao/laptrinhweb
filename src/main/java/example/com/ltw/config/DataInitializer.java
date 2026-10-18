package example.com.ltw.config;

import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.entity.UserStatus;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database initialization...");

        // Seed / Update 5 Demo Accounts with 5 distinct roles and password 123456
        seedDemoAccounts();
    }

    private void seedDemoAccounts() {
        log.info("Checking and seeding 5 role demo accounts (Password: 123456)...");

        // 1. ADMIN
        createOrUpdateDemoUser(
                "admin@gamil.com",
                "Trịnh Quản Trị (Admin)",
                "123456",
                Role.ADMIN,
                "0900000001",
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80"
        );

        // 2. MANAGER
        createOrUpdateDemoUser(
                "manager@gamil.com",
                "Lê Quản Lý (Manager)",
                "123456",
                Role.MANAGER,
                "0900000002",
                "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=200&q=80"
        );

        // 3. STAFF
        createOrUpdateDemoUser(
                "staff@gamil.com",
                "Nguyễn Bán Hàng (Staff)",
                "123456",
                Role.STAFF,
                "0900000003",
                "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=200&q=80"
        );

        // 4. SHIPPER
        createOrUpdateDemoUser(
                "shipper@gamil.com",
                "Trần Giao Vận (Shipper)",
                "123456",
                Role.SHIPPER,
                "0900000004",
                "https://images.unsplash.com/photo-1628157582853-a796fa650a6a?auto=format&fit=crop&w=200&q=80"
        );

        // 5. USER
        createOrUpdateDemoUser(
                "user@ngamil.com",
                "Vũ Khách Hàng (User VIP)",
                "123456",
                Role.USER,
                "0900000005",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
        );

        // Also ensure user's personal test email is present with Role USER
        createOrUpdateDemoUser(
                "trinhphuhao2108@gmail.com",
                "Trịnh Phú Hào (User VIP)",
                "123456",
                Role.USER,
                "0373703896",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
        );
    }

    private void createOrUpdateDemoUser(String email, String fullName, String rawPassword, Role role, String phone, String avatarUrl) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhone(phone);
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);
        log.info("Initialized Demo Account -> Email: {} | Role: {} | Password: {}", email, role, rawPassword);
    }
}
