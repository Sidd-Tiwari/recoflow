package com.recoflow.repository;

import com.recoflow.entity.Invoice;
import com.recoflow.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Page<Invoice> findAllByTenantTenantId(UUID tenantId, Pageable pageable);

    Optional<Invoice> findByInvoiceIdAndTenantTenantId(UUID invoiceId, UUID tenantId);

    List<Invoice> findByTenantTenantIdAndStatus(UUID tenantId, InvoiceStatus status);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.invoiceNo, 10) AS int)), 0) " +
           "FROM Invoice i WHERE i.tenant.tenantId = :tenantId " +
           "AND i.invoiceNo LIKE CONCAT('INV-', :year, '-%')")
    Integer findMaxInvoiceSequence(UUID tenantId, String year);

    @Query("SELECT i FROM Invoice i WHERE i.tenant.tenantId = :tenantId " +
           "AND i.status IN ('SENT', 'PARTIAL') " +
           "AND ABS(i.total - i.paidAmount - :amount) / i.total < 0.02")
    List<Invoice> findCandidatesByAmount(UUID tenantId, BigDecimal amount);

    @Query("SELECT i FROM Invoice i WHERE i.tenant.tenantId = :tenantId " +
           "AND i.status IN ('SENT', 'PARTIAL') " +
           "AND i.invoiceDate BETWEEN :from AND :to")
    List<Invoice> findOpenInvoicesInDateRange(UUID tenantId, LocalDate from, LocalDate to);

    long countByTenantTenantIdAndStatus(UUID tenantId, InvoiceStatus status);
}
