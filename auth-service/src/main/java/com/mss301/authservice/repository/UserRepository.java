package com.mss301.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.authservice.entity.UserAccount;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);
}
