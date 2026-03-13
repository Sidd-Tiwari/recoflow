package com.recoflow.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private UUID tenantId;
    private String name;
    private List<String> roles;
}
