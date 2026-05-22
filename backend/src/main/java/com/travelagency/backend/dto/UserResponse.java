package com.travelagency.backend.dto;

import com.travelagency.backend.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String keycloakId;
    private String fullName;
    private String email;
    private String phone;
    private DocumentType identityDocumentType;
    private String identityDocumentNumber;
    private String nationality;
    private Boolean active;
    private LocalDateTime createdAt;
}
