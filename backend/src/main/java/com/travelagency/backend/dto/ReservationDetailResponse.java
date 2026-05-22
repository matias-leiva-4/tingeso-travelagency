package com.travelagency.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual reservation detail inside the sales report.
 * Each row represents one CONFIRMED reservation with client and package info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailResponse {

    private Long reservationId;
    private LocalDateTime reservationDate;

    // Client info
    private String clientName;
    private String clientEmail;

    // Package info
    private String packageName;
    private String destination;

    // Amounts
    private Integer passengerCount;
    private Long baseAmount;
    private Long discountAmount;
    private Long finalAmount;

    // Status (always CONFIRMED in sales report, but kept for completeness)
    private String status;
}
