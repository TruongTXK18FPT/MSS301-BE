package com.mss301.authservice.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.authservice.entity.UserAccount;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);

    List<UserAccount> findByStatusAndRoleName(UserAccount.UserStatus status, String roleName);

    @Query("select u from UserAccount u left join u.role r where coalesce(r.name,'') <> :roleName")
    Page<UserAccount> findNonAdminUsers(@Param("roleName") String roleName, Pageable pageable);
}
