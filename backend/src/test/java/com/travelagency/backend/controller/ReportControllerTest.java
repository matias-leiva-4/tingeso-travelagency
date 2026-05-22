package com.travelagency.backend.controller;

import com.travelagency.backend.dto.PackageRankingResponse;
import com.travelagency.backend.dto.SalesReportResponse;
import com.travelagency.backend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock ReportService reportService;
    @InjectMocks ReportController controller;

    private final LocalDate FROM = LocalDate.of(2025, 1, 1);
    private final LocalDate TO   = LocalDate.of(2025, 3, 31);

    // ── GET /api/reports/sales ────────────────────────────────────────────────

    @Test
    void getSalesReport_returns200WithResponse() {
        SalesReportResponse report = SalesReportResponse.builder()
                .from(FROM).to(TO)
                .totalReservations(10).totalPassengers(25).totalRevenue(2_500_000L)
                .build();

        when(reportService.getSalesReport(FROM, TO)).thenReturn(report);

        ResponseEntity<SalesReportResponse> response = controller.getSalesReport(FROM, TO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalReservations()).isEqualTo(10);
        assertThat(response.getBody().getTotalRevenue()).isEqualTo(2_500_000L);
        verify(reportService).getSalesReport(FROM, TO);
    }

    // ── GET /api/reports/packages/ranking ────────────────────────────────────

    @Test
    void getPackageRanking_returns200WithList() {
        List<PackageRankingResponse> ranking = List.of(
                PackageRankingResponse.builder()
                        .packageId(1L).packageName("Tour Atacama").destination("Atacama")
                        .totalReservations(5).totalPassengers(12).totalRevenue(1_800_000L)
                        .build());

        when(reportService.getPackageRanking(FROM, TO)).thenReturn(ranking);

        ResponseEntity<List<PackageRankingResponse>> response =
                controller.getPackageRanking(FROM, TO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPackageName()).isEqualTo("Tour Atacama");
        verify(reportService).getPackageRanking(FROM, TO);
    }

    @Test
    void getPackageRanking_emptyPeriod_returns200WithEmptyList() {
        when(reportService.getPackageRanking(FROM, TO)).thenReturn(List.of());

        ResponseEntity<List<PackageRankingResponse>> response =
                controller.getPackageRanking(FROM, TO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
