package com.mss301.premiumservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
* 1. Admin tạo Plan với các Entitlement
* 2. User đăng ký Subscription cho Plan
* 3. Hệ thống tạo UsageMeter cho từng Entitlement
* 4. Theo dõi usage và kiểm tra giới hạn khi user sử dụng tính năng
* 5. Reset usage khi bắt đầu chu kỳ mới
*/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usage_meters")
// lượng sử dụng
public class UsageMeter {

    @Id
    @Column(name = "usage_meter_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageMeterId;

    @Column(name = "user_id", nullable = false, length = 100)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "entitlement_id", referencedColumnName = "entitlement_id")
    private Entitlement entitlement;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "used")
    private long used;

    @Column(name = "limit_usage")
    private long limit;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public UsageMeter(Long userId, Entitlement entitlement, LocalDateTime periodStart, LocalDateTime periodEnd, long used, long limit, LocalDateTime updatedAt) {
        this.userId = userId;
        this.entitlement = entitlement;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.used = used;
        this.limit = limit;
        this.updatedAt = updatedAt;
    }
}
