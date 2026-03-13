package com.recoflow.service;

import com.recoflow.config.TenantContext;
import com.recoflow.dto.response.DailyCollectionResponse;
import com.recoflow.dto.response.OutstandingInvoiceResponse;
import com.recoflow.entity.Invoice;
import com.recoflow.enums.InvoiceStatus;
import com.recoflow.enums.ReconStatus;
import com.recoflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final InvoiceRepository invoiceRepository;

    public DailyCollectionResponse getDailyCollections(LocalDate date) {
        UUID tenantId = TenantContext.getTenantId();
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalCollected = transactionRepository.sumCreditsByDateRange(tenantId, from, to);
        var transactions = transactionRepository.findCreditsByDateRange(tenantId, from, to);
        long matchedCount   = reconciliationRepository.countByTenantTenantIdAndStatus(tenantId, ReconStatus.CONFIRMED);
        long unmatchedCount = transactions.size() - matchedCount;

        return DailyCollectionResponse.builder()
            .date(date)
            .totalCollected(totalCollected)
            .matchedCount(matchedCount)
            .unmatchedCount(Math.max(0, unmatchedCount))
            .transactionCount(transactions.size())
            .build();
    }

    public List<OutstandingInvoiceResponse> getOutstanding() {
        UUID tenantId = TenantContext.getTenantId();
        List<Invoice> sent    = invoiceRepository.findByTenantTenantIdAndStatus(tenantId, InvoiceStatus.SENT);
        List<Invoice> partial = invoiceRepository.findByTenantTenantIdAndStatus(tenantId, InvoiceStatus.PARTIAL);

        List<Invoice> all = new ArrayList<>();
        all.addAll(sent);
        all.addAll(partial);

        return all.stream().map(inv -> OutstandingInvoiceResponse.builder()
            .invoiceId(inv.getInvoiceId())
            .invoiceNo(inv.getInvoiceNo())
            .customerName(inv.getCustomer().getName())
            .invoiceDate(inv.getInvoiceDate())
            .dueDate(inv.getDueDate())
            .total(inv.getTotal())
            .paidAmount(inv.getPaidAmount())
            .outstanding(inv.getTotal().subtract(inv.getPaidAmount()))
            .status(inv.getStatus())
            .build())
            .sorted(Comparator.comparing(OutstandingInvoiceResponse::getDueDate,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }
}
