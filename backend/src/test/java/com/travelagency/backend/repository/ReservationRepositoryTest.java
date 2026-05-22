package com.travelagency.backend.repository;

import com.travelagency.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para ReservationRepository.
 * Verifica tanto los métodos derivados como las queries JPQL de reportes.
 */
@SpringBootTest
@Transactional
class ReservationRepositoryTest {

    @Autowired ReservationRepository   reservationRepository;
    @Autowired UserRepository          userRepository;
    @Autowired TravelPackageRepository packageRepository;

    private UserEntity          user;
    private TravelPackageEntity pkg;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setKeycloakId("kc-resv-repo-test");
        user.setFullName("Carlos Reserva");
        user.setEmail("carlos.resv.repo@test.cl");
        user.setActive(true);
        user = userRepository.save(user);

        pkg = new TravelPackageEntity();
        pkg.setName("Tour Repo Test");
        pkg.setDestination("Valparaíso");
        pkg.setDescription("Test");
        pkg.setPrice(100_000L);
        pkg.setStartDate(LocalDate.now().plusDays(10));
        pkg.setEndDate(LocalDate.now().plusDays(15));
        pkg.setTotalSlots(20);
        pkg.setAvailableSlots(20);
        pkg.setStatus(PackageStatus.AVAILABLE);
        pkg = packageRepository.save(pkg);

        reservationRepository.flush();
    }

    // ── findByUser_Id ──────────────────────────────────────────────────────────

    @Test
    void findByUser_Id_returnsAllReservationsOfUser() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 2));
        reservationRepository.save(buildReservation(ReservationStatus.PENDING_PAYMENT, 1));
        reservationRepository.flush();

        List<ReservationEntity> result = reservationRepository.findByUser_Id(user.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getUser().getId().equals(user.getId()));
    }

    @Test
    void findByUser_Id_unknownUser_returnsEmpty() {
        List<ReservationEntity> result = reservationRepository.findByUser_Id(99999L);

        assertThat(result).isEmpty();
    }

    // ── countByUser_IdAndStatus ────────────────────────────────────────────────

    @Test
    void countByUser_IdAndStatus_countsOnlyMatchingStatus() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 2));
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 1));
        reservationRepository.save(buildReservation(ReservationStatus.PENDING_PAYMENT, 3));
        reservationRepository.flush();

        long count = reservationRepository
                .countByUser_IdAndStatus(user.getId(), ReservationStatus.CONFIRMED);

        assertThat(count).isEqualTo(2);
    }

    // ── countByUser_IdAndStatusInAndCreatedAtAfter ─────────────────────────────

    @Test
    void countByStatusInAndCreatedAtAfter_countsRecentActiveReservations() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 1));
        reservationRepository.save(buildReservation(ReservationStatus.PENDING_PAYMENT, 1));
        reservationRepository.flush();

        long count = reservationRepository.countByUser_IdAndStatusInAndCreatedAtAfter(
                user.getId(),
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING_PAYMENT),
                LocalDateTime.now().minusDays(1));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByStatusInAndCreatedAtAfter_futureWindow_returnsZero() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 1));
        reservationRepository.flush();

        // Ventana que empieza en el futuro → ninguna reserva recién creada entra
        long count = reservationRepository.countByUser_IdAndStatusInAndCreatedAtAfter(
                user.getId(),
                List.of(ReservationStatus.CONFIRMED),
                LocalDateTime.now().plusHours(1));

        assertThat(count).isZero();
    }

    // ── findByStatusAndExpiresAtBefore ─────────────────────────────────────────

    @Test
    void findByStatusAndExpiresAtBefore_returnsExpiredPendingReservations() {
        ReservationEntity expired = buildReservation(ReservationStatus.PENDING_PAYMENT, 1);
        expired.setExpiresAt(LocalDateTime.now().minusHours(2));
        reservationRepository.save(expired);

        ReservationEntity notExpired = buildReservation(ReservationStatus.PENDING_PAYMENT, 1);
        notExpired.setExpiresAt(LocalDateTime.now().plusHours(24));
        reservationRepository.save(notExpired);
        reservationRepository.flush();

        List<ReservationEntity> result = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING_PAYMENT, LocalDateTime.now());

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(r -> r.getExpiresAt().isBefore(LocalDateTime.now()));
    }

    // ── findConfirmedBetween (Épica 7) ─────────────────────────────────────────

    @Test
    void findConfirmedBetween_returnsOnlyConfirmedInPeriod() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 2));
        reservationRepository.save(buildReservation(ReservationStatus.CANCELLED, 1));
        reservationRepository.save(buildReservation(ReservationStatus.PENDING_PAYMENT, 1));
        reservationRepository.flush();

        List<ReservationEntity> result = reservationRepository.findConfirmedBetween(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED);
    }

    @Test
    void findConfirmedBetween_outsidePeriod_returnsEmpty() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 1));
        reservationRepository.flush();

        List<ReservationEntity> result = reservationRepository.findConfirmedBetween(
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(20));

        assertThat(result).isEmpty();
    }

    // ── findRankedPackagesBetween (Épica 7) ────────────────────────────────────

    @Test
    void findRankedPackagesBetween_aggregatesCorrectly() {
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 2));
        reservationRepository.save(buildReservation(ReservationStatus.CONFIRMED, 3));
        reservationRepository.flush();

        List<Object[]> rows = reservationRepository.findRankedPackagesBetween(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        assertThat(rows).isNotEmpty();
        Object[] row = rows.get(0);
        assertThat(row[0]).isEqualTo(pkg.getId());        // packageId
        assertThat((Long) row[3]).isEqualTo(2L);          // 2 reservas
        assertThat((Long) row[4]).isEqualTo(5L);          // 2+3 pasajeros
        assertThat((Long) row[5]).isEqualTo(500_000L);    // (2+3)*100.000
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReservationEntity buildReservation(ReservationStatus status, int passengers) {
        ReservationEntity r = new ReservationEntity();
        r.setUser(user);
        r.setTravelPackage(pkg);
        r.setPassengerCount(passengers);
        r.setBaseAmount(pkg.getPrice() * passengers);
        r.setDiscountAmount(0L);
        r.setFinalAmount(pkg.getPrice() * passengers);
        r.setStatus(status);
        r.setExpiresAt(LocalDateTime.now().plusHours(24));
        return r;
    }
}
