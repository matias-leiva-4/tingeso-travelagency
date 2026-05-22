package com.travelagency.backend.controller;

import com.travelagency.backend.dto.UpdateProfileRequest;
import com.travelagency.backend.dto.UserResponse;
import com.travelagency.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de usuarios.
 * Solo orquesta HTTP ↔ UserService. Sin lógica de negocio ni acceso a repositorios.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * Devuelve el perfil del usuario autenticado.
     * Si es la primera vez que inicia sesión, lo registra automáticamente (shadow-user).
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getProfile(jwt));
    }

    /**
     * PUT /api/users/me/profile
     * Permite al usuario actualizar sus datos de contacto.
     * El nombre y email los gestiona Keycloak, no este endpoint.
     */
    @PutMapping("/me/profile")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(jwt, request));
    }
}
