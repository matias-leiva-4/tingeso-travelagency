package com.travelagency.backend.dto;

import com.travelagency.backend.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long reservationId;
    private Long amount;
    private String cardLastFour;
    private String cardHolderName;
    private PaymentStatus status;
    private LocalDateTime paidAt;

}
