package example.com.ltw.address.entity;

import example.com.ltw.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String receiverName;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String phone;

    @NotNull
    @Column(nullable = false)
    private Integer provinceId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String province;

    @NotNull
    @Column(nullable = false)
    private Integer districtId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String district;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String wardCode;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String ward;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String streetAddress;

    @Column(nullable = false)
    private boolean isDefault = false;

    @PrePersist
    protected void onCreate() {
        // Giá trị mặc định cho địa chỉ mới.
        // Service chịu trách nhiệm bảo đảm mỗi User
        // có tối đa một địa chỉ mặc định.
    }
}