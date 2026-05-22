package com.travelagency.backend.dto;

import com.travelagency.backend.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long id;

    // Datos del usuario (denormalizados — evita una segunda llamada al frontend)
    private Long userId;
    private String userFullName;

    // Datos del paquete (denormalizados)
    private Long packageId;
    private String packageName;
    private String packageDestination;

    private Integer passengerCount;

    // Desglose de montos en CLP
    private Long baseAmount;      // Precio sin descuentos
    private Long discountAmount;  // Total descontado
    private Long finalAmount;     // Lo que el usuario paga

    // Descripción legible de cada descuento aplicado
    private List<String> appliedDiscounts;

    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}