package com.recoflow.service;

import com.opencsv.CSVReader;
import com.recoflow.config.TenantContext;
import com.recoflow.entity.*;
import com.recoflow.enums.FileStatus;
import com.recoflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementImportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final StatementFileRepository statementFileRepository;
    private final TransactionRepository transactionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final MatchingEngine matchingEngine;

    @Transactional
    public StatementFile upload(MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        User uploader = userRepository.getReferenceById(TenantContext.getUserId());

        StatementFile sf = statementFileRepository.save(StatementFile.builder()
            .tenant(tenant)
            .filename(file.getOriginalFilename())
            .uploadedBy(uploader)
            .status(FileStatus.UPLOADED)
            .build());

        // Read bytes eagerly — MultipartFile stream closes after request finishes
        try {
            byte[] fileBytes = file.getBytes();
            parseAsync(sf.getFileId(), tenantId, fileBytes);
        } catch (Exception e) {
            log.error("Failed to read uploaded file: {}", e.getMessage());
            sf.setStatus(FileStatus.FAILED);
            sf.setErrorDetail(e.getMessage());
            statementFileRepository.save(sf);
        }
        return sf;
    }

    @Async("taskExecutor")
    public void parseAsync(UUID fileId, UUID tenantId, byte[] fileBytes) {
        TenantContext.setTenantId(tenantId);
        StatementFile sf = statementFileRepository.findById(fileId).orElseThrow();
        sf.setStatus(FileStatus.PARSING);
        statementFileRepository.save(sf);

        int total = 0, valid = 0, invalid = 0;
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(fileBytes)))) {
            String[] header = reader.readNext(); // skip header
            String[] row;
            List<Transaction> batch = new ArrayList<>();

            while ((row = reader.readNext()) != null) {
                total++;
                try {
                    Transaction txn = parseRow(row, sf);
                    if (!transactionRepository.existsByTenantTenantIdAndUtr(tenantId, txn.getUtr())) {
                        batch.add(txn);
                        valid++;
                    } else {
                        invalid++;
                        errors.add("Row " + total + ": duplicate UTR " + txn.getUtr());
                    }
                    if (batch.size() >= 500) {
                        transactionRepository.saveAll(batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    invalid++;
                    errors.add("Row " + total + ": " + e.getMessage());
                }
            }
            if (!batch.isEmpty()) transactionRepository.saveAll(batch);

            sf.setStatus(FileStatus.PARSED);
            sf.setTotalRows(total);
            sf.setValidRows(valid);
            sf.setInvalidRows(invalid);
            sf.setParsedAt(Instant.now());
            if (!errors.isEmpty()) sf.setErrorDetail(String.join("\n", errors.subList(0, Math.min(errors.size(), 20))));
            statementFileRepository.save(sf);

            log.info("Parsed file {}: total={}, valid={}, invalid={}", fileId, total, valid, invalid);

            // Trigger auto-matching
            runMatchingForFile(fileId, tenantId);

        } catch (Exception e) {
            log.error("Failed to parse statement file {}: {}", fileId, e.getMessage());
            sf.setStatus(FileStatus.FAILED);
            sf.setErrorDetail(e.getMessage());
            statementFileRepository.save(sf);
        } finally {
            TenantContext.clear();
        }
    }

    private Transaction parseRow(String[] row, StatementFile sf) {
        // Expected CSV: date,time,amount,utr,remark,payer_vpa,type
        if (row.length < 5) throw new IllegalArgumentException("Insufficient columns");
        String dateStr = row[0].trim();
        String timeStr = row.length > 1 ? row[1].trim() : "00:00:00";
        String amountStr = row[2].trim().replaceAll("[^0-9.]", "");
        String utr = row[3].trim();
        String remark = row.length > 4 ? row[4].trim() : "";
        String payerVpa = row.length > 5 ? row[5].trim() : null;
        String txnType = row.length > 6 ? row[6].trim().toUpperCase() : "CREDIT";

        LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
        LocalTime time = LocalTime.parse(timeStr, TIME_FMT);
        Instant txnTime = LocalDateTime.of(date, time).toInstant(ZoneOffset.UTC);

        return Transaction.builder()
            .tenant(sf.getTenant())
            .statementFile(sf)
            .txnTime(txnTime)
            .amount(new BigDecimal(amountStr))
            .utr(utr)
            .remark(remark)
            .payerVpa(payerVpa)
            .txnType(txnType)
            .rawJson(Map.of("raw", String.join(",", row)))
            .build();
    }

    private void runMatchingForFile(UUID fileId, UUID tenantId) {
        log.info("Running auto-matching for file {}", fileId);
        List<Transaction> txns = transactionRepository.findAllByStatementFileFileId(fileId);
        for (Transaction txn : txns) {
            try {
                matchingEngine.matchTransaction(txn, tenantId);
            } catch (Exception e) {
                log.warn("Match failed for txn {}: {}", txn.getTxnId(), e.getMessage());
            }
        }
    }
}
