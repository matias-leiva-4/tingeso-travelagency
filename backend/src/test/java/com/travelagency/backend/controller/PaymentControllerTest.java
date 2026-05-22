package com.travelagency.backend.controller;

import com.travelagency.backend.dto.PaymentResponse;
import com.travelagency.backend.dto.ProcessPaymentRequest;
import com.travelagency.backend.model.PaymentStatus;
import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.service.AuthenticatedUserService;
import com.travelagency.backend.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock PaymentService           paymentService;
    @Mock AuthenticatedUserService authenticatedUserService;
    @InjectMocks PaymentController controller;

    private UserEntity customer;
    private Jwt        mockJwt;

    @BeforeEach
    void setUp() {
        customer = new UserEntity();
        customer.setId(1L);
        customer.setFullName("Pedro Soto");
        customer.setActive(true);

        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "kc-002")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // ── POST /api/payments ────────────────────────────────────────────────────

    @Test
    void processPayment_returns201WithCompletedStatus() {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L).reservationId(100L).amount(300_000L)
                .cardLastFour("1234").cardHolderName("PEDRO SOTO")
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();

        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(false);
        when(paymentService.processPayment(any(), eq(1L), eq(false))).thenReturn(response);

        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setReservationId(100L);
        req.setCardLastFour("1234");
        req.setCardHolderName("PEDRO SOTO");

        ResponseEntity<PaymentResponse> result = controller.processPayment(req, mockJwt);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getBody().getAmount()).isEqualTo(300_000L);
        verify(paymentService).processPayment(any(), eq(1L), eq(false));
    }

    @Test
    void processPayment_delegatesToServiceWithCorrectUserId() {
        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(false);
        when(paymentService.processPayment(any(), eq(1L), eq(false)))
                .thenReturn(PaymentResponse.builder()
                        .id(1L).reservationId(100L).amount(100_000L)
                        .cardLastFour("9999").cardHolderName("P")
                        .status(PaymentStatus.COMPLETED).paidAt(LocalDateTime.now())
                        .build());

        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setReservationId(100L);
        req.setCardLastFour("9999");
        req.setCardHolderName("P");

        controller.processPayment(req, mockJwt);

        // Verifica que pasa el userId del usuario autenticado (1L) y no otro
        verify(paymentService).processPayment(any(), eq(1L), eq(false));
    }
}
