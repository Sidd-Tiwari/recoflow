package com.recoflow.repository;

import com.recoflow.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findAllByStatementFileFileIdAndTenantTenantId(UUID fileId, UUID tenantId, Pageable pageable);

    List<Transaction> findAllByStatementFileFileId(UUID fileId);

    @Query("SELECT t FROM Transaction t WHERE t.tenant.tenantId = :tenantId " +
           "AND t.txnTime BETWEEN :from AND :to AND t.txnType = 'CREDIT'")
    List<Transaction> findCreditsByDateRange(UUID tenantId, Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.txnTime BETWEEN :from AND :to AND t.txnType = 'CREDIT'")
    BigDecimal sumCreditsByDateRange(UUID tenantId, Instant from, Instant to);

    boolean existsByTenantTenantIdAndUtr(UUID tenantId, String utr);
}
