package example.com.ltw.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "email_otps",
        indexes = {
                @Index(
                        name = "idx_email_otps_email_purpose",
                        columnList = "email, purpose"
                ),
                @Index(
                        name = "idx_email_otps_expires_at",
                        columnList = "expires_at"
                )
        }
)
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String otpHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer attemptCount = 0;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer resendCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (attemptCount == null) {
            attemptCount = 0;
        }

        if (resendCount == null) {
            resendCount = 0;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}