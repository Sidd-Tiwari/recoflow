package com.recoflow.repository;

import com.recoflow.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findAllByTenantTenantId(UUID tenantId, Pageable pageable);

    Optional<Customer> findByCustomerIdAndTenantTenantId(UUID customerId, UUID tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenant.tenantId = :tenantId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR c.phone LIKE CONCAT('%', :q, '%'))")
    List<Customer> searchByTenant(UUID tenantId, String q);
}
