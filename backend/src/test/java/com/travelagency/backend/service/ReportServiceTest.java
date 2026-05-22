package com.travelagency.backend.service;

import com.travelagency.backend.dto.PackageRankingResponse;
import com.travelagency.backend.dto.ReservationDetailResponse;
import com.travelagency.backend.dto.SalesReportResponse;
import com.travelagency.backend.model.*;
import com.travelagency.backend.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock  ReservationRepository reservationRepository;
    @InjectMocks ReportService reportService;

    private final LocalDate FROM = LocalDate.of(2025, 1, 1);
    private final LocalDate TO   = LocalDate.of(2025, 3, 31);

    private UserEntity customer;
    private TravelPackageEntity pkg;

    @BeforeEach
    void setUp() {
        customer = new UserEntity();
        customer.setId(1L);
        customer.setFullName("Pedro Soto");
        customer.setEmail("pedro@test.cl");

        pkg = new TravelPackageEntity();
        pkg.setId(10L);
        pkg.setName("Tour Norte");
        pkg.setDestination("Atacama");
    }

    // ── getSalesReport ─────────────────────────────────────────────────────────

    @Test
    void getSalesReport_sumsTotalsCorrectly() {
        ReservationEntity r1 = confirmedReservation(300_000L, 2);
        ReservationEntity r2 = confirmedReservation(200_000L, 1);

        when(reservationRepository.findConfirmedBetween(FROM, TO)).thenReturn(List.of(r1, r2));

        SalesReportResponse response = reportService.getSalesReport(FROM, TO);

        assertThat(response.getTotalReservations()).isEqualTo(2);
        assertThat(response.getTotalRevenue()).isEqualTo(500_000L);
        assertThat(response.getTotalPassengers()).isEqualTo(3);
        assertThat(response.getFrom()).isEqualTo(FROM);
        assertThat(response.getTo()).isEqualTo(TO);
    }

    @Test
    void getSalesReport_includesReservationDetailList() {
        ReservationEntity r1 = confirmedReservation(300_000L, 2);
        r1.setId(1L);
        r1.setDiscountAmount(15_000L);
        r1.setCreatedAt(LocalDateTime.of(2025, 2, 10, 14, 30));

        when(reservationRepository.findConfirmedBetween(FROM, TO)).thenReturn(List.of(r1));

        SalesReportResponse response = reportService.getSalesReport(FROM, TO);

        assertThat(response.getReservations()).hasSize(1);
        ReservationDetailResponse detail = response.getReservations().get(0);
        assertThat(detail.getReservationId()).isEqualTo(1L);
        assertThat(detail.getClientName()).isEqualTo("Pedro Soto");
        assertThat(detail.getClientEmail()).isEqualTo("pedro@test.cl");
        assertThat(detail.getPackageName()).isEqualTo("Tour Norte");
        assertThat(detail.getDestination()).isEqualTo("Atacama");
        assertThat(detail.getPassengerCount()).isEqualTo(2);
        assertThat(detail.getBaseAmount()).isEqualTo(300_000L);
        assertThat(detail.getDiscountAmount()).isEqualTo(15_000L);
        assertThat(detail.getFinalAmount()).isEqualTo(300_000L);
        assertThat(detail.getStatus()).isEqualTo("CONFIRMED");
        assertThat(detail.getReservationDate()).isEqualTo(LocalDateTime.of(2025, 2, 10, 14, 30));
    }

    @Test
    void getSalesReport_noReservationsInPeriod_returnsZeros() {
        when(reservationRepository.findConfirmedBetween(FROM, TO)).thenReturn(List.of());

        SalesReportResponse response = reportService.getSalesReport(FROM, TO);

        assertThat(response.getTotalReservations()).isZero();
        assertThat(response.getTotalRevenue()).isZero();
        assertThat(response.getTotalPassengers()).isZero();
        assertThat(response.getReservations()).isEmpty();
    }

    @Test
    void getSalesReport_toBeforeFrom_throwsIllegalArgument() {
        assertThatThrownBy(() -> reportService.getSalesReport(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'to'");
    }

    // ── getPackageRanking ──────────────────────────────────────────────────────

    @Test
    void getPackageRanking_returnsResponsesMappedFromRows() {
        // No usamos List.of(row) porque Java desempaqueta el Object[] como varargs
        // y produce List<Object> en lugar de List<Object[]>.
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{10L, "Tour Norte", "Atacama", 5L, 12L, 1_500_000L});
        when(reservationRepository.findRankedPackagesBetween(FROM, TO)).thenReturn(rows);

        List<PackageRankingResponse> ranking = reportService.getPackageRanking(FROM, TO);

        assertThat(ranking).hasSize(1);
        PackageRankingResponse first = ranking.get(0);
        assertThat(first.getPackageId()).isEqualTo(10L);
        assertThat(first.getPackageName()).isEqualTo("Tour Norte");
        assertThat(first.getTotalReservations()).isEqualTo(5L);
        assertThat(first.getTotalPassengers()).isEqualTo(12L);
        assertThat(first.getTotalRevenue()).isEqualTo(1_500_000L);
    }

    @Test
    void getPackageRanking_emptyPeriod_returnsEmptyList() {
        List<Object[]> emptyRows = new ArrayList<>();
        when(reservationRepository.findRankedPackagesBetween(FROM, TO)).thenReturn(emptyRows);

        List<PackageRankingResponse> ranking = reportService.getPackageRanking(FROM, TO);

        assertThat(ranking).isEmpty();
    }

    @Test
    void getPackageRanking_toBeforeFrom_throwsIllegalArgument() {
        assertThatThrownBy(() -> reportService.getPackageRanking(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReservationEntity confirmedReservation(long finalAmount, int passengers) {
        ReservationEntity r = new ReservationEntity();
        r.setUser(customer);
        r.setTravelPackage(pkg);
        r.setFinalAmount(finalAmount);
        r.setPassengerCount(passengers);
        r.setBaseAmount(finalAmount);
        r.setDiscountAmount(0L);
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setExpiresAt(LocalDateTime.now().plusHours(24));
        return r;
    }
}
