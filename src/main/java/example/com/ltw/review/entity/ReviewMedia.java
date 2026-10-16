package example.com.ltw.review.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
        name = "review_media",
        indexes = {
                @Index(
                        name = "idx_review_media_review_id",
                        columnList = "review_id"
                )
        }
)
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String url;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String publicId;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}