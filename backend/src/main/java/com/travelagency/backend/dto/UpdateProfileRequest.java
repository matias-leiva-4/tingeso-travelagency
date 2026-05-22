package com.travelagency.backend.dto;

import com.travelagency.backend.model.DocumentType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para la actualización de perfil del usuario autenticado.
 * Solo contiene los campos que el usuario puede modificar por sí mismo.
 * Nombre y email los gestiona Keycloak, no este endpoint.
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String phone;

    private DocumentType identityDocumentType;

    @Size(max = 30, message = "El número de documento no puede superar 30 caracteres")
    private String identityDocumentNumber;

    @Size(max = 60, message = "La nacionalidad no puede superar 60 caracteres")
    private String nationality;
}
