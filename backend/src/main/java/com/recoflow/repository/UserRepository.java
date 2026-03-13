package com.recoflow.repository;

import com.recoflow.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenantTenantId(String email, UUID tenantId);

    Optional<User> findByUserId(UUID userId);

    Page<User> findAllByTenantTenantId(UUID tenantId, Pageable pageable);

    boolean existsByEmailAndTenantTenantId(String email, UUID tenantId);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.userId = :userId")
    Optional<User> findByIdWithRoles(UUID userId);
}
