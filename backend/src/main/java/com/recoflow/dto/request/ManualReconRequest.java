package com.recoflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ManualReconRequest {
    @NotNull private UUID txnId;
    @NotNull private UUID invoiceId;
    private BigDecimal matchedAmount;
    private String notes;
}
