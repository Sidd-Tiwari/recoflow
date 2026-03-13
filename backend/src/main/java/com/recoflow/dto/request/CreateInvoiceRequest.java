package com.recoflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateInvoiceRequest {

    @NotNull
    private UUID customerId;

    @NotNull
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    private String notes;

    @NotNull @Valid
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotBlank
        private String description;

        @NotNull @Positive
        private BigDecimal quantity;

        @NotNull @Positive
        private BigDecimal rate;

        @NotNull @DecimalMin("0") @DecimalMax("100")
        private BigDecimal taxPct;

        private Integer sortOrder = 0;
    }
}
