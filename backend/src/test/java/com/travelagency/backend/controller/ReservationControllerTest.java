package com.travelagency.backend.controller;

import com.travelagency.backend.dto.CreateReservationRequest;
import com.travelagency.backend.dto.ReceiptResponse;
import com.travelagency.backend.dto.ReservationResponse;
import com.travelagency.backend.model.ReservationStatus;
import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.service.AuthenticatedUserService;
import com.travelagency.backend.service.ReservationService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock ReservationService        reservationService;
    @Mock AuthenticatedUserService  authenticatedUserService;
    @InjectMocks ReservationController controller;

    private UserEntity customer;
    private Jwt        mockJwt;
    private ReservationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        customer = new UserEntity();
        customer.setId(1L);
        customer.setFullName("Ana López");
        customer.setActive(true);

        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "kc-001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        sampleResponse = ReservationResponse.builder()
                .id(100L).userId(1L).userFullName("Ana López")
                .packageId(10L).packageName("Tour Atacama")
                .passengerCount(2).baseAmount(400_000L)
                .discountAmount(0L).finalAmount(400_000L)
                .appliedDiscounts(List.of())
                .status(ReservationStatus.PENDING_PAYMENT)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    // ── POST /api/reservations ────────────────────────────────────────────────

    @Test
    void createReservation_returns201WithBody() {
        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(reservationService.createReservation(any(), eq(customer))).thenReturn(sampleResponse);

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(2);

        ResponseEntity<ReservationResponse> response = controller.createReservation(req, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(reservationService).createReservation(any(), eq(customer));
    }

    // ── GET /api/reservations/{id} ────────────────────────────────────────────

    @Test
    void getReservationById_returns200() {
        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(false);
        when(reservationService.getReservationByIdForUser(100L, 1L, false))
                .thenReturn(sampleResponse);

        ResponseEntity<ReservationResponse> response =
                controller.getReservationById(100L, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(100L);
    }

    // ── GET /api/reservations ────────────────────────────────────────────────

    @Test
    void getReservationsByUser_customerSeesOwnReservations() {
        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(false);
        when(reservationService.getReservationsByUser(1L)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ReservationResponse>> response =
                controller.getReservationsByUser(null, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        // Un cliente sin userId param siempre ve las suyas propias
        verify(reservationService).getReservationsByUser(1L);
    }

    @Test
    void getReservationsByUser_adminWithUserIdParam_seesTargetUser() {
        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(true);
        when(reservationService.getReservationsByUser(55L)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ReservationResponse>> response =
                controller.getReservationsByUser(55L, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Admin con userId=55 debe ver las reservas del usuario 55
        verify(reservationService).getReservationsByUser(55L);
    }

    // ── DELETE /api/reservations/{id} ─────────────────────────────────────────

    @Test
    void cancelReservation_returns200WithCancelledStatus() {
        ReservationResponse cancelled = ReservationResponse.builder()
                .id(100L).userId(1L).userFullName("Ana López")
                .packageId(10L).packageName("Tour Atacama")
                .passengerCount(2).baseAmount(400_000L)
                .discountAmount(0L).finalAmount(400_000L)
                .appliedDiscounts(List.of())
                .status(ReservationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(false);
        when(reservationService.cancelReservationForUser(100L, 1L, false)).thenReturn(cancelled);

        ResponseEntity<ReservationResponse> response =
                controller.cancelReservation(100L, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    // ── GET /api/reservations/{id}/receipt ─────────────────────────────────────

    @Test
    void getReceipt_returns200WithReceipt() {
        ReceiptResponse receipt = ReceiptResponse.builder()
                .reservationId(100L)
                .clientName("Ana Lopez")
                .clientEmail("ana@test.cl")
                .packageName("Tour Atacama")
                .destination("Atacama")
                .passengerCount(2)
                .baseAmount(400_000L)
                .discountAmount(0L)
                .finalAmount(400_000L)
                .paymentStatus("COMPLETED")
                .build();

        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(false);
        when(reservationService.generateReceipt(100L, 1L, false)).thenReturn(receipt);

        ResponseEntity<ReceiptResponse> response = controller.getReceipt(100L, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getReservationId()).isEqualTo(100L);
        assertThat(response.getBody().getPaymentStatus()).isEqualTo("COMPLETED");
        verify(reservationService).generateReceipt(100L, 1L, false);
    }

    @Test
    void getReceipt_adminCanSeeAnyReceipt() {
        ReceiptResponse receipt = ReceiptResponse.builder()
                .reservationId(200L)
                .clientName("Otro Usuario")
                .build();

        when(authenticatedUserService.findOrCreateFromJwt(mockJwt)).thenReturn(customer);
        when(authenticatedUserService.isAdmin(mockJwt)).thenReturn(true);
        when(reservationService.generateReceipt(200L, 1L, true)).thenReturn(receipt);

        ResponseEntity<ReceiptResponse> response = controller.getReceipt(200L, mockJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reservationService).generateReceipt(200L, 1L, true);
    }
}
