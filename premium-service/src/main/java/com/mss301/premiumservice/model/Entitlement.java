package com.mss301.premiumservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mss301.premiumservice.constant.Unit;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DurationFormat;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "entitlements")
// quyền lợi
public class Entitlement {

    @Id
    @Column(name = "entitlement_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long entitlementId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "default_limit", nullable = false)
    private long defaultLimit;

    @Column(name = "unit", nullable = false)
    private Unit unit;

    @ManyToMany(mappedBy = "entitlements")
    @JsonIgnore
    private List<Plan> plans;
}
