package com.mss301.premiumservice.repository;

import com.mss301.premiumservice.model.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    @EntityGraph(attributePaths = { "entitlements" })
    Optional<Plan> findByCode(String code);

    @EntityGraph(attributePaths = { "entitlements" })
    @Query("SELECT p FROM Plan p ORDER BY p.price ASC")
    @Override
    List<Plan> findAll();

    @EntityGraph(attributePaths = { "entitlements" })
    @Override
    Optional<Plan> findById(Long id);
}
