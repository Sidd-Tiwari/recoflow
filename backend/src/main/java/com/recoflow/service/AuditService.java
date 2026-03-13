package com.recoflow.service;

import com.recoflow.config.TenantContext;
import com.recoflow.entity.AuditLog;
import com.recoflow.entity.Tenant;
import com.recoflow.entity.User;
import com.recoflow.repository.AuditLogRepository;
import com.recoflow.repository.TenantRepository;
import com.recoflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Async
    public void log(User actor, String action, String entityType, String entityId,
                    Map<String, Object> before, Map<String, Object> after) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) return;

            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return;

            auditLogRepository.save(AuditLog.builder()
                .tenant(tenant)
                .actorUser(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeJson(before)
                .afterJson(after)
                .build());
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogs(UUID tenantId, String entityType, String entityId, Pageable pageable) {
        if (entityType != null && entityId != null) {
            return auditLogRepository.findAllByTenantTenantIdAndEntityTypeAndEntityIdOrderByTsDesc(
                tenantId, entityType, entityId, pageable);
        }
        return auditLogRepository.findAllByTenantTenantIdOrderByTsDesc(tenantId, pageable);
    }
}
