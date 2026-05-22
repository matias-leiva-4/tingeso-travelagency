package com.travelagency.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para el ranking de paquetes por período.
 * Cada elemento representa un paquete con sus totales de reservas, pasajeros e ingresos.
 * La lista viene ordenada por totalReservations DESC desde el repositorio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageRankingResponse {

    private Long packageId;
    private String packageName;
    private String destination;
    private long totalReservations;
    private long totalPassengers;
    private long totalRevenue;
}
