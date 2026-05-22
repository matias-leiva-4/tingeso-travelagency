package com.travelagency.backend.service;

import com.travelagency.backend.dto.UpdateProfileRequest;
import com.travelagency.backend.dto.UserResponse;
import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de la capa de usuarios.
 * Responsabilidades:
 *  - Obtener el perfil del usuario autenticado
 *  - Actualizar los datos de contacto editables por el usuario
 *
 * El nombre y el email los sincroniza Keycloak a través de AuthenticatedUserService.
 * Aquí solo gestionamos los campos que el usuario cambia dentro de la app.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;

    /**
     * Devuelve el perfil del usuario activo. Lanza 403 si la cuenta está inactiva.
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Jwt jwt) {
        UserEntity user = authenticatedUserService.getActiveUserOrThrow(jwt);
        return toResponse(user);
    }

    /**
     * Actualiza los campos de contacto del usuario autenticado.
     * Solo modifica: phone, identityDocumentType, identityDocumentNumber, nationality.
     * No permite cambiar nombre ni email (eso es responsabilidad de Keycloak).
     */
    @Transactional
    public UserResponse updateProfile(Jwt jwt, UpdateProfileRequest request) {
        UserEntity user = authenticatedUserService.getActiveUserOrThrow(jwt);

        user.setPhone(request.getPhone());
        user.setIdentityDocumentType(request.getIdentityDocumentType());
        user.setIdentityDocumentNumber(request.getIdentityDocumentNumber());
        user.setNationality(request.getNationality());

        return toResponse(userRepository.save(user));
    }

    // ── Mapper entidad → DTO ──────────────────────────────────────────────────
    // Privado: nadie fuera de esta clase necesita convertir un UserEntity a UserResponse.
    // Si en el futuro fuera necesario, se puede extraer a un @Component Mapper.

    private static UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .identityDocumentType(user.getIdentityDocumentType())
                .identityDocumentNumber(user.getIdentityDocumentNumber())
                .nationality(user.getNationality())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
