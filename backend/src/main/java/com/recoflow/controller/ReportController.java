package com.recoflow.controller;

import com.recoflow.dto.response.DailyCollectionResponse;
import com.recoflow.dto.response.OutstandingInvoiceResponse;
import com.recoflow.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily-collections")
    public ResponseEntity<DailyCollectionResponse> dailyCollections(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDailyCollections(date == null ? LocalDate.now() : date));
    }

    @GetMapping("/outstanding")
    public ResponseEntity<List<OutstandingInvoiceResponse>> outstanding() {
        return ResponseEntity.ok(reportService.getOutstanding());
    }
}
