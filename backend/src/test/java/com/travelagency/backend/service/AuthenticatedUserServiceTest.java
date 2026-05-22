package com.travelagency.backend.service;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock  UserRepository userRepository;
    @InjectMocks AuthenticatedUserService authService;

    private static final String KC_ID  = "kc-abc-123";
    private static final String EMAIL  = "user@test.cl";
    private static final String NAME   = "Juan Pérez";

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** JWT con todos los claims completos (caso normal). */
    private Jwt fullJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", KC_ID)
                .claim("email", EMAIL)
                .claim("name", NAME)
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    /** JWT sin email (usa preferred_username como fallback). */
    private Jwt jwtNoEmail() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", KC_ID)
                .claim("preferred_username", "juanp")
                .claim("given_name", "Juan")
                .claim("family_name", "Pérez")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    /** JWT con rol ADMIN en realm_access. */
    private Jwt adminJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", KC_ID)
                .claim("email", EMAIL)
                .claim("name", NAME)
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "CUSTOMER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // ── findOrCreateFromJwt ───────────────────────────────────────────────────

    @Test
    void findOrCreate_existingByKeycloakId_updatesAndReturns() {
        UserEntity existing = new UserEntity();
        existing.setId(1L);
        existing.setKeycloakId(KC_ID);
        existing.setFullName("Nombre viejo");
        existing.setEmail(EMAIL);
        existing.setActive(true);

        when(userRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = authService.findOrCreateFromJwt(fullJwt());

        // Debe actualizar el nombre desde el JWT
        assertThat(result.getFullName()).isEqualTo(NAME);
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreate_newUser_createsAndSaves() {
        when(userRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserEntity result = authService.findOrCreateFromJwt(fullJwt());

        assertThat(result.getKeycloakId()).isEqualTo(KC_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getActive()).isTrue();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void findOrCreate_existingByEmail_linksKeycloakId() {
        // Usuario creado antes de tener Keycloak — se enlaza por email
        UserEntity byEmail = new UserEntity();
        byEmail.setId(2L);
        byEmail.setEmail(EMAIL);
        byEmail.setFullName(NAME);
        byEmail.setActive(true);

        when(userRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(byEmail));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = authService.findOrCreateFromJwt(fullJwt());

        assertThat(result.getKeycloakId()).isEqualTo(KC_ID);
    }

    @Test
    void findOrCreate_jwtWithoutEmail_usesPreferredUsernameAsFallback() {
        when(userRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = authService.findOrCreateFromJwt(jwtNoEmail());

        // email fallback: preferred_username + "@keycloak.local"
        assertThat(result.getEmail()).contains("juanp");
        // nombre fallback: given_name + family_name
        assertThat(result.getFullName()).contains("Juan");
    }

    // ── getActiveUserOrThrow ──────────────────────────────────────────────────

    @Test
    void getActiveUserOrThrow_activeUser_returnsUser() {
        UserEntity active = new UserEntity();
        active.setKeycloakId(KC_ID);
        active.setEmail(EMAIL);
        active.setFullName(NAME);
        active.setActive(true);

        when(userRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.of(active));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = authService.getActiveUserOrThrow(fullJwt());

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void getActiveUserOrThrow_inactiveUser_throws403() {
        UserEntity inactive = new UserEntity();
        inactive.setKeycloakId(KC_ID);
        inactive.setEmail(EMAIL);
        inactive.setFullName(NAME);
        inactive.setActive(false);

        when(userRepository.findByKeycloakId(KC_ID)).thenReturn(Optional.of(inactive));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.getActiveUserOrThrow(fullJwt()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ── isAdmin ───────────────────────────────────────────────────────────────

    @Test
    void isAdmin_adminJwt_returnsTrue() {
        assertThat(authService.isAdmin(adminJwt())).isTrue();
    }

    @Test
    void isAdmin_customerJwt_returnsFalse() {
        assertThat(authService.isAdmin(fullJwt())).isFalse();
    }

    @Test
    void isAdmin_jwtWithoutRealmAccess_returnsFalse() {
        Jwt noRoles = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("sub", KC_ID)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThat(authService.isAdmin(noRoles)).isFalse();
    }
}
