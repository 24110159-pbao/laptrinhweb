package example.com.ltw.staff.dto;

import example.com.ltw.order.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public class StaffDashboardDto {
    private Long branchId;
    private String branchName;
    private String branchAddress;
    private String branchPhone;

    // Order Volume Metrics
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long shippingOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    // Payment & Revenue Metrics (Scoped exclusively to current branch / staff orders)
    private BigDecimal totalOrderAmount;
    private BigDecimal totalRevenue;
    private BigDecimal collectedRevenue;
    private BigDecimal pendingCodRevenue;
    private long paidOrders;
    private long unpaidOrders;
    private long pendingCodOrders;

    // Lists for CRM Dashboard
    private List<Order> recentOrders;
    private List<Order> urgentOrders;

    // Time-Range Filter Metadata
    private String timePreset;
    private String fromDateTime;
    private String toDateTime;
    private String formattedTimeRange;

    public StaffDashboardDto() {
    }

    public static Builder builder() {
        return new Builder();
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

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getConfirmedOrders() {
        return confirmedOrders;
    }

    public void setConfirmedOrders(long confirmedOrders) {
        this.confirmedOrders = confirmedOrders;
    }

    public long getShippingOrders() {
        return shippingOrders;
    }

    public void setShippingOrders(long shippingOrders) {
        this.shippingOrders = shippingOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getTotalOrderAmount() {
        return totalOrderAmount;
    }

    public void setTotalOrderAmount(BigDecimal totalOrderAmount) {
        this.totalOrderAmount = totalOrderAmount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getCollectedRevenue() {
        return collectedRevenue;
    }

    public void setCollectedRevenue(BigDecimal collectedRevenue) {
        this.collectedRevenue = collectedRevenue;
    }

    public BigDecimal getPendingCodRevenue() {
        return pendingCodRevenue;
    }

    public void setPendingCodRevenue(BigDecimal pendingCodRevenue) {
        this.pendingCodRevenue = pendingCodRevenue;
    }

    public long getPaidOrders() {
        return paidOrders;
    }

    public void setPaidOrders(long paidOrders) {
        this.paidOrders = paidOrders;
    }

    public long getUnpaidOrders() {
        return unpaidOrders;
    }

    public void setUnpaidOrders(long unpaidOrders) {
        this.unpaidOrders = unpaidOrders;
    }

    public long getPendingCodOrders() {
        return pendingCodOrders;
    }

    public void setPendingCodOrders(long pendingCodOrders) {
        this.pendingCodOrders = pendingCodOrders;
    }

    public List<Order> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<Order> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<Order> getUrgentOrders() {
        return urgentOrders;
    }

    public void setUrgentOrders(List<Order> urgentOrders) {
        this.urgentOrders = urgentOrders;
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
        private final StaffDashboardDto dto = new StaffDashboardDto();

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

        public Builder totalOrders(long totalOrders) {
            dto.totalOrders = totalOrders;
            return this;
        }

        public Builder pendingOrders(long pendingOrders) {
            dto.pendingOrders = pendingOrders;
            return this;
        }

        public Builder confirmedOrders(long confirmedOrders) {
            dto.confirmedOrders = confirmedOrders;
            return this;
        }

        public Builder shippingOrders(long shippingOrders) {
            dto.shippingOrders = shippingOrders;
            return this;
        }

        public Builder deliveredOrders(long deliveredOrders) {
            dto.deliveredOrders = deliveredOrders;
            return this;
        }

        public Builder cancelledOrders(long cancelledOrders) {
            dto.cancelledOrders = cancelledOrders;
            return this;
        }

        public Builder totalOrderAmount(BigDecimal totalOrderAmount) {
            dto.totalOrderAmount = totalOrderAmount;
            return this;
        }

        public Builder totalRevenue(BigDecimal totalRevenue) {
            dto.totalRevenue = totalRevenue;
            return this;
        }

        public Builder collectedRevenue(BigDecimal collectedRevenue) {
            dto.collectedRevenue = collectedRevenue;
            return this;
        }

        public Builder pendingCodRevenue(BigDecimal pendingCodRevenue) {
            dto.pendingCodRevenue = pendingCodRevenue;
            return this;
        }

        public Builder paidOrders(long paidOrders) {
            dto.paidOrders = paidOrders;
            return this;
        }

        public Builder unpaidOrders(long unpaidOrders) {
            dto.unpaidOrders = unpaidOrders;
            return this;
        }

        public Builder pendingCodOrders(long pendingCodOrders) {
            dto.pendingCodOrders = pendingCodOrders;
            return this;
        }

        public Builder recentOrders(List<Order> recentOrders) {
            dto.recentOrders = recentOrders;
            return this;
        }

        public Builder urgentOrders(List<Order> urgentOrders) {
            dto.urgentOrders = urgentOrders;
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

        public StaffDashboardDto build() {
            return dto;
        }
    }
}
