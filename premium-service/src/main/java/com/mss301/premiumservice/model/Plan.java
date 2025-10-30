package com.mss301.premiumservice.model;

import com.mss301.premiumservice.constant.Currency;
import com.mss301.premiumservice.constant.PlanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plans")
// gói đăng ký
public class Plan {

    @Id
    @Column(name = "plan_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "billing_cycle", nullable = false)
    private int billingCycle;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "plans", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Entitlement> entitlements;

    // Helper methods để sync quan hệ
    public void addEntitlement(Entitlement entitlement) {
        if (this.entitlements == null) {
            this.entitlements = new ArrayList<>();
        }
        if (!this.entitlements.contains(entitlement)) {
            this.entitlements.add(entitlement);
            if (entitlement.getPlans() == null) {
                entitlement.setPlans(new ArrayList<>());
            }
            if (!entitlement.getPlans().contains(this)) {
                entitlement.getPlans().add(this);
            }
        }
    }

    public void removeEntitlement(Entitlement entitlement) {
        if (this.entitlements != null) {
            this.entitlements.remove(entitlement);
            if (entitlement.getPlans() != null) {
                entitlement.getPlans().remove(this);
            }
        }
    }
}
