package com.travelagency.backend.repository;

import com.travelagency.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para PaymentRepository.
 * Verifica existsByReservationId y findByReservationId.
 */
@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    @Autowired PaymentRepository      paymentRepository;
    @Autowired ReservationRepository  reservationRepository;
    @Autowired UserRepository         userRepository;
    @Autowired TravelPackageRepository packageRepository;

    private ReservationEntity reservation;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setKeycloakId("kc-pay-repo-test");
        user.setFullName("Pago Repo Test");
        user.setEmail("pago.repo.test@example.cl");
        user.setActive(true);
        user = userRepository.save(user);

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setName("Tour Pago Repo");
        pkg.setDestination("Lima");
        pkg.setDescription("Test pago");
        pkg.setPrice(200_000L);
        pkg.setStartDate(LocalDate.now().plusDays(5));
        pkg.setEndDate(LocalDate.now().plusDays(10));
        pkg.setTotalSlots(10);
        pkg.setAvailableSlots(10);
        pkg.setStatus(PackageStatus.AVAILABLE);
        pkg = packageRepository.save(pkg);

        reservation = new ReservationEntity();
        reservation.setUser(user);
        reservation.setTravelPackage(pkg);
        reservation.setPassengerCount(1);
        reservation.setBaseAmount(200_000L);
        reservation.setDiscountAmount(0L);
        reservation.setFinalAmount(200_000L);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(LocalDateTime.now().plusHours(24));
        reservation = reservationRepository.save(reservation);
        reservationRepository.flush();
    }

    // ── existsByReservationId ─────────────────────────────────────────────────

    @Test
    void existsByReservationId_withPayment_returnsTrue() {
        paymentRepository.save(buildPayment());
        paymentRepository.flush();

        assertThat(paymentRepository.existsByReservationId(reservation.getId())).isTrue();
    }

    @Test
    void existsByReservationId_withoutPayment_returnsFalse() {
        assertThat(paymentRepository.existsByReservationId(reservation.getId())).isFalse();
    }

    // ── findByReservationId ───────────────────────────────────────────────────

    @Test
    void findByReservationId_existingPayment_returnsPayment() {
        PaymentEntity saved = paymentRepository.save(buildPayment());
        paymentRepository.flush();

        Optional<PaymentEntity> result = paymentRepository.findByReservationId(reservation.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getAmount()).isEqualTo(200_000L);
        assertThat(result.get().getCardLastFour()).isEqualTo("4321");
    }

    @Test
    void findByReservationId_noPayment_returnsEmpty() {
        Optional<PaymentEntity> result = paymentRepository.findByReservationId(reservation.getId());

        assertThat(result).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PaymentEntity buildPayment() {
        PaymentEntity p = new PaymentEntity();
        p.setReservation(reservation);
        p.setAmount(200_000L);
        p.setCardLastFour("4321");
        p.setCardHolderName("PAGO REPO TEST");
        p.setStatus(PaymentStatus.COMPLETED);
        p.setPaidAt(LocalDateTime.now());
        return p;
    }
}
