package com.recoflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCustomerRequest {
    @NotBlank
    private String name;
    private String phone;
    private String email;
    private String gstin;
    private String vpaHint;
    private String notes;
}
