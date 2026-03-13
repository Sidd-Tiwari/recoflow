package com.recoflow.service;

import com.recoflow.config.TenantContext;
import com.recoflow.dto.request.CreateInvoiceRequest;
import com.recoflow.entity.*;
import com.recoflow.enums.InvoiceStatus;
import com.recoflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public Page<Invoice> listInvoices(InvoiceStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        if (status != null) {
            Specification<Invoice> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("tenant").get("tenantId"), tenantId),
                cb.equal(root.get("status"), status)
            );
            return invoiceRepository.findAll(spec, pageable);
        }
        return invoiceRepository.findAllByTenantTenantId(tenantId, pageable);
    }

    public Invoice getById(UUID invoiceId) {
        return invoiceRepository.findByInvoiceIdAndTenantTenantId(invoiceId, TenantContext.getTenantId())
            .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @Transactional
    public Invoice create(CreateInvoiceRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = TenantContext.getUserId();

        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Customer customer = customerRepository.findByCustomerIdAndTenantTenantId(req.getCustomerId(), tenantId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        User creator = userRepository.getReferenceById(userId);

        Invoice invoice = Invoice.builder()
            .tenant(tenant)
            .customer(customer)
            .invoiceNo(generateInvoiceNo(tenantId))
            .invoiceDate(req.getInvoiceDate())
            .dueDate(req.getDueDate())
            .notes(req.getNotes())
            .status(InvoiceStatus.DRAFT)
            .createdBy(creator)
            .build();

        req.getItems().forEach(item -> {
            InvoiceItem lineItem = InvoiceItem.builder()
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .rate(item.getRate())
                .taxPct(item.getTaxPct())
                .amount(item.getQuantity().multiply(item.getRate()))
                .sortOrder(item.getSortOrder())
                .build();
            invoice.addItem(lineItem);
        });

        invoice.recalculateTotals();
        Invoice saved = invoiceRepository.save(invoice);
        auditService.log(creator, "INVOICE_CREATED", "INVOICE", saved.getInvoiceId().toString(), null,
            java.util.Map.of("invoiceNo", saved.getInvoiceNo(), "total", saved.getTotal()));
        return saved;
    }

    @Transactional
    public Invoice updateStatus(UUID invoiceId, InvoiceStatus newStatus) {
        Invoice invoice = getById(invoiceId);
        InvoiceStatus oldStatus = invoice.getStatus();
        validateTransition(oldStatus, newStatus);
        invoice.setStatus(newStatus);
        Invoice saved = invoiceRepository.save(invoice);
        User actor = userRepository.getReferenceById(TenantContext.getUserId());
        auditService.log(actor, "INVOICE_STATUS_CHANGED", "INVOICE", invoiceId.toString(),
            java.util.Map.of("status", oldStatus), java.util.Map.of("status", newStatus));
        return saved;
    }

    private void validateTransition(InvoiceStatus from, InvoiceStatus to) {
        boolean valid = switch (from) {
            case DRAFT    -> to == InvoiceStatus.SENT || to == InvoiceStatus.CANCELLED;
            case SENT     -> to == InvoiceStatus.PARTIAL || to == InvoiceStatus.PAID || to == InvoiceStatus.CANCELLED;
            case PARTIAL  -> to == InvoiceStatus.PAID || to == InvoiceStatus.CANCELLED;
            default       -> false;
        };
        if (!valid) throw new IllegalStateException("Invalid status transition: " + from + " → " + to);
    }

    private String generateInvoiceNo(UUID tenantId) {
        String year = String.valueOf(Year.now().getValue());
        Integer seq = invoiceRepository.findMaxInvoiceSequence(tenantId, year);
        return String.format("INV-%s-%04d", year, (seq == null ? 0 : seq) + 1);
    }
}
