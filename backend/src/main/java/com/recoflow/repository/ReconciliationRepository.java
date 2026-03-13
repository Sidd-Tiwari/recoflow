package com.recoflow.repository;

import com.recoflow.entity.Reconciliation;
import com.recoflow.enums.ReconStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Page<Reconciliation> findAllByTenantTenantIdAndStatus(UUID tenantId, ReconStatus status, Pageable pageable);

    Page<Reconciliation> findAllByTenantTenantId(UUID tenantId, Pageable pageable);

    Optional<Reconciliation> findByReconIdAndTenantTenantId(UUID reconId, UUID tenantId);

    List<Reconciliation> findByTransactionTxnId(UUID txnId);

    List<Reconciliation> findByInvoiceInvoiceIdAndStatus(UUID invoiceId, ReconStatus status);

    @Query("SELECT COALESCE(SUM(r.matchedAmount), 0) FROM Reconciliation r " +
           "WHERE r.invoice.invoiceId = :invoiceId AND r.status = 'CONFIRMED'")
    BigDecimal sumConfirmedAmountForInvoice(UUID invoiceId);

    long countByTenantTenantIdAndStatus(UUID tenantId, ReconStatus status);

    @Query("SELECT COALESCE(SUM(r.matchedAmount), 0) FROM Reconciliation r " +
           "WHERE r.tenant.tenantId = :tenantId AND r.status = 'CONFIRMED' " +
           "AND r.confirmedAt BETWEEN :from AND :to")
    BigDecimal sumConfirmedInRange(UUID tenantId, Instant from, Instant to);
}
