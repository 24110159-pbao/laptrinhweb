package example.com.ltw.admin.service;

import example.com.ltw.admin.dto.AdminDiscountForm;
import example.com.ltw.discount.entity.DiscountCode;
import example.com.ltw.discount.entity.DiscountStatus;
import example.com.ltw.discount.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDiscountService {

    private final DiscountCodeRepository discountCodeRepository;

    @Transactional(readOnly = true)
    public List<DiscountCode> getAllDiscounts(DiscountStatus status) {
        if (status != null) {
            return discountCodeRepository.findAllByStatusOrderByStartAtDesc(status);
        }
        return discountCodeRepository.findAllByOrderByStartAtDesc();
    }

    @Transactional(readOnly = true)
    public DiscountCode getDiscountById(Long id) {
        return discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mã giảm giá."));
    }

    @Transactional
    public DiscountCode createDiscount(AdminDiscountForm form) {
        String code = form.getCode().trim().toUpperCase();

        if (discountCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá '" + code + "' đã tồn tại.");
        }

        if (form.getStartAt().isAfter(form.getEndAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        DiscountCode discount = new DiscountCode();
        discount.setCode(code);
        discount.setDiscountValue(form.getDiscountValue());
        discount.setQuantity(form.getQuantity());
        discount.setStartAt(form.getStartAt());
        discount.setEndAt(form.getEndAt());
        discount.setStatus(form.getStatus() != null ? form.getStatus() : DiscountStatus.ACTIVE);

        return discountCodeRepository.save(discount);
    }

    @Transactional
    public DiscountCode updateDiscount(Long id, AdminDiscountForm form) {
        DiscountCode discount = getDiscountById(id);
        String code = form.getCode().trim().toUpperCase();

        if (!discount.getCode().equalsIgnoreCase(code) && discountCodeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá '" + code + "' đã tồn tại.");
        }

        if (form.getStartAt().isAfter(form.getEndAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        discount.setCode(code);
        discount.setDiscountValue(form.getDiscountValue());
        discount.setQuantity(form.getQuantity());
        discount.setStartAt(form.getStartAt());
        discount.setEndAt(form.getEndAt());
        if (form.getStatus() != null) {
            discount.setStatus(form.getStatus());
        }

        return discountCodeRepository.save(discount);
    }

    @Transactional
    public DiscountStatus toggleDiscountStatus(Long id) {
        DiscountCode discount = getDiscountById(id);
        if (discount.getStatus() == DiscountStatus.ACTIVE) {
            discount.setStatus(DiscountStatus.INACTIVE);
        } else {
            discount.setStatus(DiscountStatus.ACTIVE);
        }
        discountCodeRepository.save(discount);
        return discount.getStatus();
    }
}
