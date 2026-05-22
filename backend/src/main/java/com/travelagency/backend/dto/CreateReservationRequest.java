package com.travelagency.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReservationRequest {

    private Long userId;
    // Deprecated: with Keycloak the backend uses the authenticated JWT user.

    @NotNull(message = "El ID del paquete es requerido.")
    private Long packageId;

    @NotNull(message = "La cantidad de pasajeros es requerida.")
    @Min(value = 1, message = "La cantidad de pasajeros debe ser mayor a 0.")
    private Integer passengerCount;
}
