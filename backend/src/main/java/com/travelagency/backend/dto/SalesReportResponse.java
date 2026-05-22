package com.travelagency.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para el reporte de ventas por período.
 * Agrega solo reservas CONFIRMED (excluye CANCELLED y PENDING_PAYMENT).
 * Includes both aggregate totals and a detailed list of each reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {

    /** Fecha de inicio del período consultado. */
    private LocalDate from;

    /** Fecha de fin del período consultado. */
    private LocalDate to;

    /** Número de reservas CONFIRMED en el período. */
    private long totalReservations;

    /** Suma de pasajeros en todas las reservas confirmadas. */
    private long totalPassengers;

    /** Suma de finalAmount de todas las reservas confirmadas (en CLP). */
    private long totalRevenue;

    /** Detailed list of each confirmed reservation in the period. */
    private List<ReservationDetailResponse> reservations;
}
