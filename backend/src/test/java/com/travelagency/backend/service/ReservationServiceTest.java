package com.travelagency.backend.service;

import com.travelagency.backend.dto.CreateReservationRequest;
import com.travelagency.backend.dto.DiscountBreakdown;
import com.travelagency.backend.dto.ReceiptResponse;
import com.travelagency.backend.dto.ReservationResponse;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.*;
import com.travelagency.backend.repository.PaymentRepository;
import com.travelagency.backend.repository.ReservationRepository;
import com.travelagency.backend.repository.TravelPackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock  ReservationRepository reservationRepository;
    @Mock  TravelPackageRepository packageRepository;
    @Mock  PaymentRepository paymentRepository;
    @Mock  DiscountCalculatorService discountCalculatorService;
    @InjectMocks ReservationService reservationService;

    private TravelPackageEntity availablePackage;
    private UserEntity customer;
    private ReservationEntity pendingReservation;

    @BeforeEach
    void setUp() {
        availablePackage = new TravelPackageEntity();
        availablePackage.setId(10L);
        availablePackage.setName("Tour Patagonia");
        availablePackage.setDestination("Patagonia");
        availablePackage.setStartDate(LocalDate.now().plusDays(30));
        availablePackage.setEndDate(LocalDate.now().plusDays(40));
        availablePackage.setPrice(200_000L);
        availablePackage.setTotalSlots(20);
        availablePackage.setAvailableSlots(20);
        availablePackage.setStatus(PackageStatus.AVAILABLE);

        customer = new UserEntity();
        customer.setId(1L);
        customer.setFullName("Juan Perez");
        customer.setEmail("juan@test.cl");
        customer.setPhone("+56912345678");
        customer.setIdentityDocumentType(com.travelagency.backend.model.DocumentType.RUT);
        customer.setIdentityDocumentNumber("12345678-9");
        customer.setActive(true);

        pendingReservation = new ReservationEntity();
        pendingReservation.setId(100L);
        pendingReservation.setUser(customer);
        pendingReservation.setTravelPackage(availablePackage);
        pendingReservation.setPassengerCount(2);
        pendingReservation.setBaseAmount(400_000L);
        pendingReservation.setDiscountAmount(0L);
        pendingReservation.setFinalAmount(400_000L);
        pendingReservation.setStatus(ReservationStatus.PENDING_PAYMENT);
        pendingReservation.setExpiresAt(LocalDateTime.now().plusHours(24));
    }

    // ── createReservation ──────────────────────────────────────────────────────

    @Test
    void createReservation_success_noDiscounts() {
        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(2);

        when(packageRepository.findById(10L)).thenReturn(Optional.of(availablePackage));
        when(discountCalculatorService.calculate(anyLong(), anyInt(), anyLong()))
                .thenReturn(DiscountBreakdown.builder()
                        .totalDiscountPercent(0).totalDiscountAmount(0L)
                        .appliedDescriptions(List.of()).build());
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            ReservationEntity r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.createReservation(req, customer);

        assertThat(response.getFinalAmount()).isEqualTo(400_000L);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        // Verifica que se descontaron los cupos
        assertThat(availablePackage.getAvailableSlots()).isEqualTo(18);
    }

    @Test
    void createReservation_fillsAllSlots_setsSOLD_OUT() {
        availablePackage.setAvailableSlots(3);
        availablePackage.setTotalSlots(3);

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(3);

        when(packageRepository.findById(10L)).thenReturn(Optional.of(availablePackage));
        when(discountCalculatorService.calculate(anyLong(), anyInt(), anyLong()))
                .thenReturn(DiscountBreakdown.builder()
                        .totalDiscountPercent(0).totalDiscountAmount(0L)
                        .appliedDescriptions(List.of()).build());
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            ReservationEntity r = inv.getArgument(0); r.setId(1L); return r;
        });
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reservationService.createReservation(req, customer);

        assertThat(availablePackage.getAvailableSlots()).isZero();
        assertThat(availablePackage.getStatus()).isEqualTo(PackageStatus.SOLD_OUT);
    }

    @Test
    void createReservation_packageNotAvailable_throwsIllegalArgument() {
        availablePackage.setStatus(PackageStatus.SOLD_OUT);
        when(packageRepository.findById(10L)).thenReturn(Optional.of(availablePackage));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(1);

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estado");
    }

    @Test
    void createReservation_insufficientSlots_throwsIllegalArgument() {
        availablePackage.setAvailableSlots(2);
        when(packageRepository.findById(10L)).thenReturn(Optional.of(availablePackage));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(5);  // más que los disponibles

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cupos insuficientes");
    }

    @Test
    void createReservation_packageAlreadyStarted_throwsIllegalArgument() {
        availablePackage.setStartDate(LocalDate.now().minusDays(1));  // ya empezó
        when(packageRepository.findById(10L)).thenReturn(Optional.of(availablePackage));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(1);

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha de inicio pasada");
    }

    @Test
    void createReservation_packageNotFound_throwsResourceNotFoundException() {
        when(packageRepository.findById(999L)).thenReturn(Optional.empty());

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(999L);
        req.setPassengerCount(1);

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReservation_missingPhone_throwsIllegalArgument() {
        customer.setPhone(null);

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(1);

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teléfono");
        // No debe tocar el paquete
        verify(packageRepository, never()).save(any());
    }

    @Test
    void createReservation_missingDocument_throwsIllegalArgument() {
        customer.setIdentityDocumentNumber("   ");  // blank

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(1);

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("número de documento");
        verify(packageRepository, never()).save(any());
    }

    @Test
    void createReservation_missingDocumentType_throwsIllegalArgument() {
        customer.setIdentityDocumentType(null);

        CreateReservationRequest req = new CreateReservationRequest();
        req.setPackageId(10L);
        req.setPassengerCount(1);

        assertThatThrownBy(() -> reservationService.createReservation(req, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de documento");
        verify(packageRepository, never()).save(any());
    }

    // ── cancelReservation ──────────────────────────────────────────────────────

    @Test
    void cancelReservation_success_releasesSlots() {
        availablePackage.setAvailableSlots(18);
        pendingReservation.setPassengerCount(2);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(100L);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(availablePackage.getAvailableSlots()).isEqualTo(20);  // devuelve 2 cupos
    }

    @Test
    void cancelReservation_alreadyConfirmed_throwsIllegalArgument() {
        pendingReservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmada");
    }

    @Test
    void cancelReservation_alreadyCancelled_throwsIllegalArgument() {
        pendingReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void cancelReservation_soldOutPackage_setsAvailableOnRelease() {
        availablePackage.setStatus(PackageStatus.SOLD_OUT);
        availablePackage.setAvailableSlots(0);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reservationService.cancelReservation(100L);

        assertThat(availablePackage.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
    }

    // ── expireUnpaidReservations (@Scheduled) ─────────────────────────────────

    @Test
    void expireUnpaidReservations_expiredReservations_cancelledAndSlotsReleased() {
        availablePackage.setAvailableSlots(18);
        pendingReservation.setExpiresAt(LocalDateTime.now().minusHours(1)); // ya expiró

        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(ReservationStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reservationService.expireUnpaidReservations();

        assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(availablePackage.getAvailableSlots()).isEqualTo(20); // devolvió los 2 cupos
    }

    @Test
    void expireUnpaidReservations_soldOutPackage_setsAvailable() {
        availablePackage.setStatus(PackageStatus.SOLD_OUT);
        availablePackage.setAvailableSlots(0);
        pendingReservation.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(ReservationStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reservationService.expireUnpaidReservations();

        assertThat(availablePackage.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
    }

    @Test
    void expireUnpaidReservations_noExpiredReservations_doesNothing() {
        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(ReservationStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of());

        reservationService.expireUnpaidReservations();

        verify(reservationRepository, never()).save(any());
        verify(packageRepository, never()).save(any());
    }

    // ── getReservationById (sin control de acceso) ─────────────────────────────

    @Test
    void getReservationById_found_returnsResponse() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        ReservationResponse response = reservationService.getReservationById(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void getReservationById_notFound_throwsResourceNotFoundException() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getReservationsByUser ──────────────────────────────────────────────────

    @Test
    void getReservationsByUser_returnsListForUser() {
        when(reservationRepository.findByUser_Id(1L)).thenReturn(List.of(pendingReservation));

        List<ReservationResponse> result = reservationService.getReservationsByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getReservationsByUser_noReservations_returnsEmptyList() {
        when(reservationRepository.findByUser_Id(99L)).thenReturn(List.of());

        List<ReservationResponse> result = reservationService.getReservationsByUser(99L);

        assertThat(result).isEmpty();
    }

    // ── cancelReservationForUser ───────────────────────────────────────────────

    @Test
    void cancelReservationForUser_ownerCanCancel() {
        availablePackage.setAvailableSlots(18);

        // findById se llama dos veces: una en cancelReservationForUser y otra en cancelReservation
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response =
                reservationService.cancelReservationForUser(100L, 1L, false); // userId=1 es el dueño

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancelReservationForUser_adminCanCancelAnyReservation() {
        availablePackage.setAvailableSlots(18);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // userId=99 no es el dueño, pero admin=true
        ReservationResponse response =
                reservationService.cancelReservationForUser(100L, 99L, true);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancelReservationForUser_wrongUser_throwsIllegalArgument() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        // userId=99 no es el dueño (dueño=1) y no es admin
        assertThatThrownBy(() ->
                reservationService.cancelReservationForUser(100L, 99L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otro usuario");
    }

    // ── getReservationByIdForUser ──────────────────────────────────────────────

    @Test
    void getReservationByIdForUser_wrongUser_throwsIllegalArgument() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        // userId=99 no es el dueño (dueño es userId=1), no es admin
        assertThatThrownBy(() -> reservationService.getReservationByIdForUser(100L, 99L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otro usuario");
    }

    @Test
    void getReservationByIdForUser_admin_canSeeAnyReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        // admin=true, userId irrelevante
        ReservationResponse response = reservationService.getReservationByIdForUser(100L, 99L, true);
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getReservationByIdForUser_owner_canSeeOwnReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        ReservationResponse response = reservationService.getReservationByIdForUser(100L, 1L, false);
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    // ── generateReceipt (Épica 6) ──────────────────────────────────────────────

    @Test
    void generateReceipt_confirmedReservation_returnsReceipt() {
        ReservationEntity confirmed = new ReservationEntity();
        confirmed.setId(100L);
        confirmed.setUser(customer);
        confirmed.setTravelPackage(availablePackage);
        confirmed.setPassengerCount(2);
        confirmed.setBaseAmount(400_000L);
        confirmed.setDiscountAmount(20_000L);
        confirmed.setFinalAmount(380_000L);
        confirmed.setDiscountDetails("Descuento por grupo: 5%");
        confirmed.setStatus(ReservationStatus.CONFIRMED);
        confirmed.setExpiresAt(LocalDateTime.now().plusHours(24));
        confirmed.setCreatedAt(LocalDateTime.of(2025, 3, 15, 10, 30));

        PaymentEntity payment = new PaymentEntity();
        payment.setId(1L);
        payment.setReservation(confirmed);
        payment.setAmount(380_000L);
        payment.setCardLastFour("4321");
        payment.setCardHolderName("Juan Perez");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.of(2025, 3, 15, 11, 0));

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(confirmed));
        when(paymentRepository.findByReservationId(100L)).thenReturn(Optional.of(payment));

        ReceiptResponse receipt = reservationService.generateReceipt(100L, 1L, false);

        assertThat(receipt.getReservationId()).isEqualTo(100L);
        assertThat(receipt.getClientName()).isEqualTo("Juan Perez");
        assertThat(receipt.getClientEmail()).isEqualTo("juan@test.cl");
        assertThat(receipt.getPackageName()).isEqualTo("Tour Patagonia");
        assertThat(receipt.getDestination()).isEqualTo("Patagonia");
        assertThat(receipt.getPassengerCount()).isEqualTo(2);
        assertThat(receipt.getBaseAmount()).isEqualTo(400_000L);
        assertThat(receipt.getDiscountAmount()).isEqualTo(20_000L);
        assertThat(receipt.getDiscountDetails()).isEqualTo("Descuento por grupo: 5%");
        assertThat(receipt.getFinalAmount()).isEqualTo(380_000L);
        assertThat(receipt.getCardLastFour()).isEqualTo("4321");
        assertThat(receipt.getPaymentStatus()).isEqualTo("COMPLETED");
        assertThat(receipt.getPaymentDate()).isEqualTo(LocalDateTime.of(2025, 3, 15, 11, 0));
    }

    @Test
    void generateReceipt_notConfirmed_throwsIllegalArgument() {
        // pendingReservation is PENDING_PAYMENT
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pendingReservation));

        assertThatThrownBy(() -> reservationService.generateReceipt(100L, 1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmadas");
    }

    @Test
    void generateReceipt_notFound_throwsResourceNotFoundException() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.generateReceipt(999L, 1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateReceipt_wrongUser_throwsIllegalArgument() {
        ReservationEntity confirmed = new ReservationEntity();
        confirmed.setId(100L);
        confirmed.setUser(customer); // owner is userId=1
        confirmed.setTravelPackage(availablePackage);
        confirmed.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(confirmed));

        // userId=99 is not the owner, not admin
        assertThatThrownBy(() -> reservationService.generateReceipt(100L, 99L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otro usuario");
    }

    @Test
    void generateReceipt_admin_canSeeAnyReceipt() {
        ReservationEntity confirmed = new ReservationEntity();
        confirmed.setId(100L);
        confirmed.setUser(customer);
        confirmed.setTravelPackage(availablePackage);
        confirmed.setPassengerCount(2);
        confirmed.setBaseAmount(400_000L);
        confirmed.setDiscountAmount(0L);
        confirmed.setFinalAmount(400_000L);
        confirmed.setStatus(ReservationStatus.CONFIRMED);
        confirmed.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(confirmed));
        when(paymentRepository.findByReservationId(100L)).thenReturn(Optional.empty());

        // admin=true, userId=99 is not the owner but should still work
        ReceiptResponse receipt = reservationService.generateReceipt(100L, 99L, true);

        assertThat(receipt.getReservationId()).isEqualTo(100L);
        // No payment found, payment fields should be null
        assertThat(receipt.getPaymentDate()).isNull();
        assertThat(receipt.getCardLastFour()).isNull();
        assertThat(receipt.getPaymentStatus()).isNull();
    }

    @Test
    void generateReceipt_withClientDocument_includesDocument() {
        customer.setIdentityDocumentType(DocumentType.RUT);
        customer.setIdentityDocumentNumber("12345678-9");

        ReservationEntity confirmed = new ReservationEntity();
        confirmed.setId(100L);
        confirmed.setUser(customer);
        confirmed.setTravelPackage(availablePackage);
        confirmed.setPassengerCount(1);
        confirmed.setBaseAmount(200_000L);
        confirmed.setDiscountAmount(0L);
        confirmed.setFinalAmount(200_000L);
        confirmed.setStatus(ReservationStatus.CONFIRMED);
        confirmed.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(confirmed));
        when(paymentRepository.findByReservationId(100L)).thenReturn(Optional.empty());

        ReceiptResponse receipt = reservationService.generateReceipt(100L, 1L, false);

        assertThat(receipt.getClientDocument()).isEqualTo("RUT: 12345678-9");
    }
}
