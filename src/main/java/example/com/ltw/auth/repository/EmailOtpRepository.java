package example.com.ltw.auth.repository;

import example.com.ltw.auth.entity.EmailOtp;
import example.com.ltw.auth.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndPurposeOrderByIdDesc(String email, OtpPurpose purpose);

    void deleteByEmail(String email);
}
