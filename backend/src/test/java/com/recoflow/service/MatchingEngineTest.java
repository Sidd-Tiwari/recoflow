package com.recoflow.service;

import com.recoflow.entity.*;
import com.recoflow.enums.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.recoflow.repository.*;

class MatchingEngineTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ReconciliationRepository reconciliationRepository;
    @Mock TenantRepository tenantRepository;

    @InjectMocks MatchingEngine engine;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private Invoice buildInvoice(String invoiceNo, String customerName, String vpaHint,
                                  BigDecimal total, LocalDate date) {
        Customer c = new Customer();
        c.setName(customerName);
        c.setVpaHint(vpaHint);

        Invoice inv = new Invoice();
        inv.setInvoiceId(UUID.randomUUID());
        inv.setInvoiceNo(invoiceNo);
        inv.setInvoiceDate(date);
        inv.setTotal(total);
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setStatus(InvoiceStatus.SENT);
        inv.setCustomer(c);
        inv.setItems(List.of());
        return inv;
    }

    private Transaction buildTxn(BigDecimal amount, Instant time, String remark, String vpa) {
        Tenant t = new Tenant();
        t.setTenantId(UUID.randomUUID());

        Transaction txn = new Transaction();
        txn.setTxnId(UUID.randomUUID());
        txn.setTenant(t);
        txn.setAmount(amount);
        txn.setTxnTime(time);
        txn.setRemark(remark);
        txn.setPayerVpa(vpa);
        return txn;
    }

    @Test
    void shouldScoreHighConfidenceWhenAllFactorsMatch() {
        LocalDate today = LocalDate.now();
        Invoice inv = buildInvoice("INV-2025-0042", "Sharma Store", "sharma@gpay",
            new BigDecimal("5000.00"), today);
        Transaction txn = buildTxn(new BigDecimal("5000.00"),
            today.atTime(10, 0).toInstant(ZoneOffset.UTC),
            "Payment INV-2025-0042", "sharma@gpay");

        when(reconciliationRepository.findByTransactionTxnId(any())).thenReturn(List.of());
        when(invoiceRepository.findCandidatesByAmount(any(), any())).thenReturn(List.of(inv));
        when(invoiceRepository.findOpenInvoicesInDateRange(any(), any(), any())).thenReturn(List.of());
        when(tenantRepository.getReferenceById(any())).thenReturn(inv.getTenant() == null ? new Tenant() : inv.getTenant());
        when(reconciliationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Reconciliation> results = engine.matchTransaction(txn, txn.getTenant().getTenantId());

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getConfidence().doubleValue()).isGreaterThanOrEqualTo(0.8);
        assertThat(results.get(0).getReason()).contains("amount_exact");
        assertThat(results.get(0).getReason()).contains("remark_contains_invoice_no");
    }

    @Test
    void shouldReturnEmptyWhenAlreadyConfirmed() {
        Transaction txn = buildTxn(new BigDecimal("1000"), Instant.now(), "test", null);
        Reconciliation confirmed = new Reconciliation();
        confirmed.setStatus(com.recoflow.enums.ReconStatus.CONFIRMED);

        when(reconciliationRepository.findByTransactionTxnId(any())).thenReturn(List.of(confirmed));

        List<Reconciliation> results = engine.matchTransaction(txn, UUID.randomUUID());
        assertThat(results).isEmpty();
    }
}
