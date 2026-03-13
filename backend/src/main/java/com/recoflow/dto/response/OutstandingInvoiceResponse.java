package com.recoflow.dto.response;

import com.recoflow.enums.InvoiceStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OutstandingInvoiceResponse {
    private UUID invoiceId;
    private String invoiceNo;
    private String customerName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private BigDecimal outstanding;
    private InvoiceStatus status;
}
