package example.com.ltw.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO đại diện cho Shipper trong danh sách đề xuất phân công đơn hàng tại chi nhánh.
 * Hỗ trợ thuật toán cân bằng tải: Ưu tiên Shipper ít việc nhất -> Người chờ lâu nhất nhận trước.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperAssignmentOptionDto {

    private Long id;
    private String fullName;
    private String phone;
    private String email;

    /**
     * Số lượng đơn hàng đang phụ trách vận chuyển (trạng thái PENDING hoặc RECEIVED chưa hoàn thành).
     */
    private long activeOrdersCount;

    /**
     * Thời điểm đơn hàng gần nhất được gán cho Shipper này (createdAt của Shipment mới nhất).
     * Nếu null nghĩa là chưa nhận đơn nào trong ngày/hệ thống -> được xem là chờ lâu nhất.
     */
    private LocalDateTime lastAssignedAt;

    /**
     * Mô tả thời gian chờ nhận đơn (Ví dụ: "Chờ 45 phút", "Chưa có đơn (Chờ từ đầu ca)").
     */
    private String waitingTimeDescription;

    /**
     * Đánh dấu xem đây có phải là Shipper được thuật toán xếp hạng số 1 (Đề xuất tối ưu nhất) hay không.
     */
    private boolean recommended;
}
