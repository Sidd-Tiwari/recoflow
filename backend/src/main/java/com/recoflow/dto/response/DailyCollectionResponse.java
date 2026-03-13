package com.recoflow.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyCollectionResponse {
    private LocalDate date;
    private BigDecimal totalCollected;
    private long matchedCount;
    private long unmatchedCount;
    private int transactionCount;
}
