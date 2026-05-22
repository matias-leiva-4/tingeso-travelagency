package com.travelagency.backend.controller;

import com.travelagency.backend.dto.UpdateProfileRequest;
import com.travelagency.backend.dto.UserResponse;
import com.travelagency.backend.model.DocumentType;
import com.travelagency.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @InjectMocks UserController controller;

    private Jwt      mockJwt;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "kc-001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        sampleResponse = UserResponse.builder()
                .id(1L).keycloakId("kc-001")
                .fullName("María González").email("maria@test.cl")
                .phone("+56912345678").nationality("Chilena")
                .active(true).createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/users/me ─────────────────────────────────────────────────────

    @Test
    void getMe_returns200WithUserResponse() {
        when(userService.getProfile(mockJwt)).thenReturn(sampleResponse);

        ResponseEntity<UserResponse> response = controller.getMe(mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFullName()).isEqualTo("María González");
        assertThat(response.getBody().getActive()).isTrue();
        verify(userService).getProfile(mockJwt);
    }

    // ── PUT /api/users/me/profile ─────────────────────────────────────────────

    @Test
    void updateProfile_returns200WithUpdatedResponse() {
        UserResponse updated = UserResponse.builder()
                .id(1L).keycloakId("kc-001")
                .fullName("María González").email("maria@test.cl")
                .phone("+56999999999")
                .identityDocumentType(DocumentType.RUT)
                .identityDocumentNumber("12.345.678-9")
                .nationality("Chilena")
                .active(true).createdAt(LocalDateTime.now())
                .build();

        when(userService.updateProfile(eq(mockJwt), any())).thenReturn(updated);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhone("+56999999999");
        req.setIdentityDocumentType(DocumentType.RUT);
        req.setIdentityDocumentNumber("12.345.678-9");
        req.setNationality("Chilena");

        ResponseEntity<UserResponse> response = controller.updateProfile(mockJwt, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPhone()).isEqualTo("+56999999999");
        assertThat(response.getBody().getIdentityDocumentNumber()).isEqualTo("12.345.678-9");
        verify(userService).updateProfile(eq(mockJwt), any(UpdateProfileRequest.class));
    }
}
