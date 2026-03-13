package com.recoflow.service;

import com.recoflow.entity.Invoice;
import com.recoflow.entity.Reconciliation;
import com.recoflow.entity.Tenant;
import com.recoflow.entity.Transaction;
import com.recoflow.enums.InvoiceStatus;
import com.recoflow.enums.ReconStatus;
import com.recoflow.repository.InvoiceRepository;
import com.recoflow.repository.ReconciliationRepository;
import com.recoflow.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngine {

    // Scoring weights (total = 100)
    private static final int WEIGHT_AMOUNT    = 40;
    private static final int WEIGHT_TIME      = 20;
    private static final int WEIGHT_REMARK    = 25;
    private static final int WEIGHT_VPA       = 15;

    private static final double HIGH_CONFIDENCE   = 0.80;
    private static final double MEDIUM_CONFIDENCE = 0.50;

    private final InvoiceRepository invoiceRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public List<Reconciliation> matchTransaction(Transaction txn, UUID tenantId) {
        // Skip if already confirmed
        boolean alreadyConfirmed = reconciliationRepository.findByTransactionTxnId(txn.getTxnId())
            .stream().anyMatch(r -> r.getStatus() == ReconStatus.CONFIRMED);
        if (alreadyConfirmed) return List.of();

        // Get candidate invoices (within ±2% amount, open status, ±7 days)
        LocalDate txnDate = txn.getTxnTime().atZone(ZoneOffset.UTC).toLocalDate();
        List<Invoice> candidates = invoiceRepository.findCandidatesByAmount(tenantId, txn.getAmount());
        candidates.addAll(invoiceRepository.findOpenInvoicesInDateRange(
            tenantId, txnDate.minusDays(7), txnDate.plusDays(1)));

        // Deduplicate
        Map<UUID, Invoice> uniqueCandidates = new LinkedHashMap<>();
        candidates.forEach(inv -> uniqueCandidates.put(inv.getInvoiceId(), inv));

        List<Reconciliation> suggestions = new ArrayList<>();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        for (Invoice invoice : uniqueCandidates.values()) {
            if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED) {
                continue;
            }
            ScoreResult result = score(txn, invoice);
            if (result.confidence() >= MEDIUM_CONFIDENCE) {
                Reconciliation recon = Reconciliation.builder()
                    .tenant(tenant)
                    .transaction(txn)
                    .invoice(invoice)
                    .matchedAmount(txn.getAmount())
                    .confidence(BigDecimal.valueOf(result.confidence()).setScale(4, RoundingMode.HALF_UP))
                    .reason(result.reasons())
                    .status(ReconStatus.SUGGESTED)
                    .build();
                suggestions.add(reconciliationRepository.save(recon));
            }
        }

        // Sort by confidence descending
        suggestions.sort(Comparator.comparing(Reconciliation::getConfidence).reversed());
        log.debug("Matched txn {} → {} suggestions", txn.getTxnId(), suggestions.size());
        return suggestions;
    }

    // ─── Scoring Logic ────────────────────────────────────────────────────────

    private ScoreResult score(Transaction txn, Invoice invoice) {
        List<String> reasons = new ArrayList<>();
        int total = 0;

        // 1. Amount match (40 pts)
        BigDecimal outstanding = invoice.getTotal().subtract(invoice.getPaidAmount());
        BigDecimal diff = txn.getAmount().subtract(outstanding).abs();
        double pct = diff.divide(outstanding.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP).doubleValue();
        if (pct == 0) {
            total += WEIGHT_AMOUNT;
            reasons.add("amount_exact");
        } else if (pct <= 0.01) {
            total += WEIGHT_AMOUNT / 2;
            reasons.add("amount_near_match");
        }

        // 2. Time window (20 pts)
        LocalDate txnDate = txn.getTxnTime().atZone(ZoneOffset.UTC).toLocalDate();
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(invoice.getInvoiceDate(), txnDate));
        if (daysDiff == 0) {
            total += WEIGHT_TIME;
            reasons.add("same_day");
        } else if (daysDiff <= 1) {
            total += WEIGHT_TIME / 2;
            reasons.add("within_1_day");
        } else if (daysDiff <= 3) {
            total += WEIGHT_TIME / 4;
            reasons.add("within_3_days");
        }

        // 3. Remark similarity (25 pts)
        if (txn.getRemark() != null) {
            String remark = txn.getRemark().toLowerCase();
            String invoiceNo = invoice.getInvoiceNo().toLowerCase();
            String customerName = invoice.getCustomer().getName().toLowerCase();
            if (remark.contains(invoiceNo)) {
                total += WEIGHT_REMARK;
                reasons.add("remark_contains_invoice_no");
            } else if (remark.contains(customerName)) {
                total += (int)(WEIGHT_REMARK * 0.6);
                reasons.add("remark_contains_customer_name");
            } else if (partialMatch(remark, customerName)) {
                total += (int)(WEIGHT_REMARK * 0.2);
                reasons.add("remark_partial_match");
            }
        }

        // 4. VPA hint match (15 pts)
        String vpaHint = invoice.getCustomer().getVpaHint();
        if (vpaHint != null && txn.getPayerVpa() != null &&
            txn.getPayerVpa().toLowerCase().contains(vpaHint.toLowerCase())) {
            total += WEIGHT_VPA;
            reasons.add("vpa_match");
        }

        return new ScoreResult(total / 100.0, reasons);
    }

    private boolean partialMatch(String remark, String name) {
        String[] words = name.split("\\s+");
        int matched = 0;
        for (String word : words) {
            if (word.length() > 3 && remark.contains(word)) matched++;
        }
        return words.length > 0 && (double) matched / words.length > 0.5;
    }

    public record ScoreResult(double confidence, List<String> reasons) {}
}
