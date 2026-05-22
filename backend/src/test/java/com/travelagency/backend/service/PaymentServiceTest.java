package com.travelagency.backend.service;

import com.travelagency.backend.dto.PaymentResponse;
import com.travelagency.backend.dto.ProcessPaymentRequest;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.*;
import com.travelagency.backend.repository.PaymentRepository;
import com.travelagency.backend.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock  PaymentRepository paymentRepository;
    @Mock  ReservationRepository reservationRepository;
    @InjectMocks PaymentService paymentService;

    private ReservationEntity pendingReservation;
    private ProcessPaymentRequest validRequest;
    private UserEntity customer;

    @BeforeEach
    void setUp() {
        customer = new UserEntity();
        customer.setId(1L);
        customer.setFullName("Ana López");

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setId(10L);

        pendingReservation = new ReservationEntity();
        pendingReservation.setId(100L);
        pendingReservation.setUser(customer);
        pendingReservation.setTravelPackage(pkg);
        pendingReservation.setFinalAmount(300_000L);
        pendingReservation.setPassengerCount(2);
        pendingReservation.setBaseAmount(300_000L);
        pendingReservation.setDiscountAmount(0L);
        pendingReservation.setStatus(ReservationStatus.PENDING_PAYMENT);
        pendingReservation.setExpiresAt(LocalDateTime.now().plusHours(24));

        validRequest = new ProcessPaymentRequest();
        validRequest.setReservationId(100L);
        validRequest.setCardLastFour("1234");
        validRequest.setCardHolderName("ANA LOPEZ");
    }

    // ── processPayment ─────────────────────────────────────────────────────────

    @Test
    void processPayment_success_confirmsReservationAndReturnsPayment() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(paymentRepository.existsByReservationId(100L)).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            PaymentEntity p = inv.getArgument(0);
            p.setId(1L);
            p.setPaidAt(LocalDateTime.now());
            return p;
        });

        PaymentResponse response = paymentService.processPayment(validRequest, 1L, false);

        assertThat(response.getAmount()).isEqualTo(300_000L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void processPayment_cancelledReservation_throwsIllegalArgument() {
        pendingReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        assertThatThrownBy(() -> paymentService.processPayment(validRequest, 1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void processPayment_alreadyConfirmed_throwsIllegalArgument() {
        pendingReservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        assertThatThrownBy(() -> paymentService.processPayment(validRequest, 1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pagada");
    }

    @Test
    void processPayment_duplicatePayment_throwsIllegalArgument() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(paymentRepository.existsByReservationId(100L)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.processPayment(validRequest, 1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un pago");
    }

    @Test
    void processPayment_wrongUser_throwsIllegalArgument() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        // userId=99 no es el dueño (userId=1), no es admin
        assertThatThrownBy(() -> paymentService.processPayment(validRequest, 99L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otro usuario");
    }

    @Test
    void processPayment_adminCanPayAnyReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(paymentRepository.existsByReservationId(100L)).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            PaymentEntity p = inv.getArgument(0);
            p.setId(1L);
            p.setPaidAt(LocalDateTime.now());
            return p;
        });

        // admin=true, userId irrelevante (99)
        PaymentResponse response = paymentService.processPayment(validRequest, 99L, true);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void processPayment_reservationNotFound_throwsResourceNotFoundException() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        validRequest.setReservationId(999L);
        assertThatThrownBy(() -> paymentService.processPayment(validRequest, 1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
