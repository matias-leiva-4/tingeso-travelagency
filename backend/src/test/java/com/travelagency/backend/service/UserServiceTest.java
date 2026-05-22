package com.travelagency.backend.service;

import com.travelagency.backend.dto.UpdateProfileRequest;
import com.travelagency.backend.dto.UserResponse;
import com.travelagency.backend.model.DocumentType;
import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock  AuthenticatedUserService authenticatedUserService;
    @Mock  UserRepository userRepository;
    @InjectMocks UserService userService;

    private UserEntity activeUser;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        activeUser = new UserEntity();
        activeUser.setId(1L);
        activeUser.setKeycloakId("kc-001");
        activeUser.setFullName("María González");
        activeUser.setEmail("maria@test.cl");
        activeUser.setActive(true);
        activeUser.setCreatedAt(LocalDateTime.now());

        // JWT mínimo para que Spring no lo rechace
        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "kc-001")
                .claim("email", "maria@test.cl")
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .build();
    }

    @Test
    void getProfile_activeUser_returnsUserResponse() {
        when(authenticatedUserService.getActiveUserOrThrow(mockJwt)).thenReturn(activeUser);

        UserResponse response = userService.getProfile(mockJwt);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("María González");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void getProfile_inactiveUser_throwsForbidden() {
        when(authenticatedUserService.getActiveUserOrThrow(mockJwt))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva."));

        assertThatThrownBy(() -> userService.getProfile(mockJwt))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void updateProfile_success_persistsChanges() {
        when(authenticatedUserService.getActiveUserOrThrow(mockJwt)).thenReturn(activeUser);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("+56912345678");
        request.setIdentityDocumentType(DocumentType.RUT);
        request.setIdentityDocumentNumber("12.345.678-9");
        request.setNationality("Chilena");

        UserResponse response = userService.updateProfile(mockJwt, request);

        assertThat(response.getPhone()).isEqualTo("+56912345678");
        assertThat(response.getIdentityDocumentType()).isEqualTo(DocumentType.RUT);
        assertThat(response.getIdentityDocumentNumber()).isEqualTo("12.345.678-9");
        assertThat(response.getNationality()).isEqualTo("Chilena");
        verify(userRepository).save(activeUser);
    }

    @Test
    void updateProfile_inactiveUser_throwsForbidden() {
        when(authenticatedUserService.getActiveUserOrThrow(mockJwt))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva."));

        assertThatThrownBy(() -> userService.updateProfile(mockJwt, new UpdateProfileRequest()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
