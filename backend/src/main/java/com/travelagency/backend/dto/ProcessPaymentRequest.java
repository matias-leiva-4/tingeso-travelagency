package com.travelagency.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProcessPaymentRequest {

    @NotNull(message = "El ID de la reserva es obligatorio.")
    private Long reservationId;

    @NotBlank(message = "Los ultimos cuatro digitos son obligatorios.")
    @Size(min = 4, max = 4, message = "Deben ser exactamento 4 digitos.")
    private String cardLastFour;

    @NotBlank(message = "El nombre del titular es obligatorio.")
    private String cardHolderName;



}

