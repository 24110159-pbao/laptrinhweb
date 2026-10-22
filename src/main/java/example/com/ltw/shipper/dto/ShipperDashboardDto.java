package example.com.ltw.shipper.dto;

import example.com.ltw.shipping.entity.Shipment;

import java.math.BigDecimal;
import java.util.List;

public class ShipperDashboardDto {
    private Long shipperId;
    private String shipperName;
    private String shipperPhone;
    private Long branchId;
    private String branchName;
    private String branchAddress;
    private String branchPhone;
    private String branchEmail;

    private long pendingPickups;      // PENDING - Mới gán, chờ nhận
    private long activeDeliveries;     // RECEIVED - Đang đi giao
    private long deliveredCount;       // DELIVERED - Giao thành công
    private long failedCount;          // DELIVERY_FAILED - Giao thất bại
    private long cancelledCount;       // CANCELLED - Đã hủy đơn
    private long totalAssigned;        // Tổng số đơn được gán

    // Financial COD Metrics
    private BigDecimal totalCodToCollect;
    private BigDecimal totalCodCollected;
    private long paidOnlineCount;

    // Shipment Lists for CRM Dashboard
    private List<Shipment> recentShipments;
    private List<Shipment> urgentShipments;

    // Time filter metadata
    private String timePreset;
    private String fromDateTime;
    private String toDateTime;
    private String formattedTimeRange;

    public ShipperDashboardDto() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getShipperId() {
        return shipperId;
    }

    public void setShipperId(Long shipperId) {
        this.shipperId = shipperId;
    }

    public String getShipperName() {
        return shipperName;
    }

    public void setShipperName(String shipperName) {
        this.shipperName = shipperName;
    }

    public String getShipperPhone() {
        return shipperPhone;
    }

    public void setShipperPhone(String shipperPhone) {
        this.shipperPhone = shipperPhone;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getBranchAddress() {
        return branchAddress;
    }

    public void setBranchAddress(String branchAddress) {
        this.branchAddress = branchAddress;
    }

    public String getBranchPhone() {
        return branchPhone;
    }

    public void setBranchPhone(String branchPhone) {
        this.branchPhone = branchPhone;
    }

    public String getBranchEmail() {
        return branchEmail;
    }

    public void setBranchEmail(String branchEmail) {
        this.branchEmail = branchEmail;
    }

    public long getPendingPickups() {
        return pendingPickups;
    }

    public void setPendingPickups(long pendingPickups) {
        this.pendingPickups = pendingPickups;
    }

    public long getActiveDeliveries() {
        return activeDeliveries;
    }

    public void setActiveDeliveries(long activeDeliveries) {
        this.activeDeliveries = activeDeliveries;
    }

    public long getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(long deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public long getTotalAssigned() {
        return totalAssigned;
    }

    public void setTotalAssigned(long totalAssigned) {
        this.totalAssigned = totalAssigned;
    }

    public BigDecimal getTotalCodToCollect() {
        return totalCodToCollect;
    }

    public void setTotalCodToCollect(BigDecimal totalCodToCollect) {
        this.totalCodToCollect = totalCodToCollect;
    }

    public BigDecimal getTotalCodCollected() {
        return totalCodCollected;
    }

    public void setTotalCodCollected(BigDecimal totalCodCollected) {
        this.totalCodCollected = totalCodCollected;
    }

    public long getPaidOnlineCount() {
        return paidOnlineCount;
    }

    public void setPaidOnlineCount(long paidOnlineCount) {
        this.paidOnlineCount = paidOnlineCount;
    }

    public List<Shipment> getRecentShipments() {
        return recentShipments;
    }

    public void setRecentShipments(List<Shipment> recentShipments) {
        this.recentShipments = recentShipments;
    }

    public List<Shipment> getUrgentShipments() {
        return urgentShipments;
    }

    public void setUrgentShipments(List<Shipment> urgentShipments) {
        this.urgentShipments = urgentShipments;
    }

    public String getTimePreset() {
        return timePreset;
    }

    public void setTimePreset(String timePreset) {
        this.timePreset = timePreset;
    }

    public String getFromDateTime() {
        return fromDateTime;
    }

    public void setFromDateTime(String fromDateTime) {
        this.fromDateTime = fromDateTime;
    }

    public String getToDateTime() {
        return toDateTime;
    }

    public void setToDateTime(String toDateTime) {
        this.toDateTime = toDateTime;
    }

    public String getFormattedTimeRange() {
        return formattedTimeRange;
    }

    public void setFormattedTimeRange(String formattedTimeRange) {
        this.formattedTimeRange = formattedTimeRange;
    }

    public static class Builder {
        private final ShipperDashboardDto dto = new ShipperDashboardDto();

        public Builder shipperId(Long shipperId) {
            dto.shipperId = shipperId;
            return this;
        }

        public Builder shipperName(String shipperName) {
            dto.shipperName = shipperName;
            return this;
        }

        public Builder shipperPhone(String shipperPhone) {
            dto.shipperPhone = shipperPhone;
            return this;
        }

        public Builder branchId(Long branchId) {
            dto.branchId = branchId;
            return this;
        }

        public Builder branchName(String branchName) {
            dto.branchName = branchName;
            return this;
        }

        public Builder branchAddress(String branchAddress) {
            dto.branchAddress = branchAddress;
            return this;
        }

        public Builder branchPhone(String branchPhone) {
            dto.branchPhone = branchPhone;
            return this;
        }

        public Builder branchEmail(String branchEmail) {
            dto.branchEmail = branchEmail;
            return this;
        }

        public Builder pendingPickups(long pendingPickups) {
            dto.pendingPickups = pendingPickups;
            return this;
        }

        public Builder activeDeliveries(long activeDeliveries) {
            dto.activeDeliveries = activeDeliveries;
            return this;
        }

        public Builder deliveredCount(long deliveredCount) {
            dto.deliveredCount = deliveredCount;
            return this;
        }

        public Builder failedCount(long failedCount) {
            dto.failedCount = failedCount;
            return this;
        }

        public Builder totalAssigned(long totalAssigned) {
            dto.totalAssigned = totalAssigned;
            return this;
        }

        public Builder totalCodToCollect(BigDecimal totalCodToCollect) {
            dto.totalCodToCollect = totalCodToCollect;
            return this;
        }

        public Builder totalCodCollected(BigDecimal totalCodCollected) {
            dto.totalCodCollected = totalCodCollected;
            return this;
        }

        public Builder paidOnlineCount(long paidOnlineCount) {
            dto.paidOnlineCount = paidOnlineCount;
            return this;
        }

        public Builder recentShipments(List<Shipment> recentShipments) {
            dto.recentShipments = recentShipments;
            return this;
        }

        public Builder urgentShipments(List<Shipment> urgentShipments) {
            dto.urgentShipments = urgentShipments;
            return this;
        }

        public Builder timePreset(String timePreset) {
            dto.timePreset = timePreset;
            return this;
        }

        public Builder fromDateTime(String fromDateTime) {
            dto.fromDateTime = fromDateTime;
            return this;
        }

        public Builder toDateTime(String toDateTime) {
            dto.toDateTime = toDateTime;
            return this;
        }

        public Builder formattedTimeRange(String formattedTimeRange) {
            dto.formattedTimeRange = formattedTimeRange;
            return this;
        }

        public ShipperDashboardDto build() {
            return dto;
        }
    }
}
