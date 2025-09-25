package com.mss301.profileservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.profileservice.entity.GuardianProfile;

@Repository
public interface GuardianProfileRepository extends JpaRepository<GuardianProfile, Long> {
    Optional<GuardianProfile> findByUserId(Long userId);

    List<GuardianProfile> findByRelationship(String relationship);

    boolean existsByUserId(Long userId);
}
