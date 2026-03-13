package com.recoflow.repository;

import com.recoflow.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByTenantTenantIdOrderByTsDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findAllByTenantTenantIdAndEntityTypeAndEntityIdOrderByTsDesc(
        UUID tenantId, String entityType, String entityId, Pageable pageable);
}
