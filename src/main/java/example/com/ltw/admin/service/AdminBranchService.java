package example.com.ltw.admin.service;

import example.com.ltw.admin.dto.AdminBranchForm;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chi nhánh."));
    }

    @Transactional
    public Branch createBranch(AdminBranchForm form) {
        String name = form.getName().trim();
        if (branchRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chi nhánh đã tồn tại.");
        }

        Branch branch = new Branch();
        branch.setName(name);
        branch.setAddress(form.getAddress().trim());
        branch.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        branch.setEmail(form.getEmail() != null ? form.getEmail().trim() : null);
        branch.setLatitude(form.getLatitude());
        branch.setLongitude(form.getLongitude());
        branch.setStatus(form.getStatus() != null ? form.getStatus() : BranchStatus.ACTIVE);

        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long id, AdminBranchForm form) {
        Branch branch = getBranchById(id);
        String name = form.getName().trim();

        if (!branch.getName().equalsIgnoreCase(name) && branchRepository.existsByNameAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chi nhánh đã tồn tại.");
        }

        branch.setName(name);
        branch.setAddress(form.getAddress().trim());
        branch.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        branch.setEmail(form.getEmail() != null ? form.getEmail().trim() : null);
        branch.setLatitude(form.getLatitude());
        branch.setLongitude(form.getLongitude());
        if (form.getStatus() != null) {
            branch.setStatus(form.getStatus());
        }

        return branchRepository.save(branch);
    }

    @Transactional
    public BranchStatus toggleBranchStatus(Long id) {
        Branch branch = getBranchById(id);
        if (branch.getStatus() == BranchStatus.ACTIVE) {
            branch.setStatus(BranchStatus.INACTIVE);
        } else {
            branch.setStatus(BranchStatus.ACTIVE);
        }
        branchRepository.save(branch);
        return branch.getStatus();
    }
}
