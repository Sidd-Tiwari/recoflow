package com.recoflow.repository;

import com.recoflow.entity.StatementFile;
import com.recoflow.enums.FileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatementFileRepository extends JpaRepository<StatementFile, UUID> {

    Page<StatementFile> findAllByTenantTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Optional<StatementFile> findByFileIdAndTenantTenantId(UUID fileId, UUID tenantId);

    long countByTenantTenantIdAndStatus(UUID tenantId, FileStatus status);
}
