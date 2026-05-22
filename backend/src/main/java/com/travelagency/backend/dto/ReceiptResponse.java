package com.travelagency.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for a reservation receipt/comprobante (Epica 6).
 * Only generated for CONFIRMED reservations that have been paid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    // Reservation info
    private Long reservationId;
    private LocalDateTime reservationDate;

    // Client info
    private String clientName;
    private String clientEmail;
    private String clientDocument;

    // Package info
    private String packageName;
    private String destination;
    private LocalDate packageStartDate;
    private LocalDate packageEndDate;

    // Amounts
    private Integer passengerCount;
    private Long baseAmount;
    private Long discountAmount;
    private String discountDetails;
    private Long finalAmount;

    // Payment info
    private LocalDateTime paymentDate;
    private String cardLastFour;
    private String paymentStatus;
}
