package com.travelagency.backend.service;

import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    /**
     * Busca o crea el usuario local a partir del JWT de Keycloak.
     * Primera llamada de un usuario nuevo → lo registra automáticamente (shadow-user pattern).
     */
    @Transactional
    public UserEntity findOrCreateFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = resolveEmail(jwt);

        return userRepository.findByKeycloakId(keycloakId)
                .map(user -> updateUserFromJwt(user, jwt))
                .or(() -> userRepository.findByEmail(email)
                        .map(user -> linkExistingUser(user, keycloakId, jwt)))
                .orElseGet(() -> createUserFromJwt(keycloakId, email, jwt));
    }

    /**
     * Igual que findOrCreateFromJwt, pero lanza 403 si el usuario está inactivo.
     * Usar en endpoints donde una cuenta desactivada no debe operar.
     */
    @Transactional
    public UserEntity getActiveUserOrThrow(Jwt jwt) {
        UserEntity user = findOrCreateFromJwt(jwt);
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva.");
        }
        return user;
    }

    /**
     * Verifica si el JWT tiene el rol ADMIN en realm_access.roles de Keycloak.
     * Los roles se leen del token, no de la DB — así cualquier cambio en Keycloak
     * tiene efecto inmediato sin re-sincronizar nada.
     */
    public boolean isAdmin(Jwt jwt) {
        return hasRole(jwt, "ADMIN");
    }

    // ── Métodos privados ──────────────────────────────────────────────────────

    private UserEntity linkExistingUser(UserEntity user, String keycloakId, Jwt jwt) {
        user.setKeycloakId(keycloakId);
        return updateUserFromJwt(user, jwt);
    }

    private UserEntity updateUserFromJwt(UserEntity user, Jwt jwt) {
        user.setFullName(resolveFullName(jwt));
        user.setEmail(resolveEmail(jwt));
        return userRepository.save(user);
    }

    private UserEntity createUserFromJwt(String keycloakId, String email, Jwt jwt) {
        UserEntity user = new UserEntity();
        user.setKeycloakId(keycloakId);
        user.setFullName(resolveFullName(jwt));
        user.setEmail(email);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String resolveEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) return email;

        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) return username + "@keycloak.local";

        return jwt.getSubject() + "@keycloak.local";
    }

    private String resolveFullName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) return name;

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String fullName = ((givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName)).trim();
        if (!fullName.isBlank()) return fullName;

        String username = jwt.getClaimAsString("preferred_username");
        return (username == null || username.isBlank()) ? "Usuario" : username;
    }

    private boolean hasRole(Jwt jwt, String roleName) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return false;

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) return false;

        return roleCollection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(role -> role.equalsIgnoreCase(roleName));
    }
}
