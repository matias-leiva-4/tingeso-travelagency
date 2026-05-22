package com.travelagency.backend.controller;

import com.travelagency.backend.dto.PackageRankingResponse;
import com.travelagency.backend.dto.SalesReportResponse;
import com.travelagency.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller de reportes administrativos (Épica 7).
 * Todos los endpoints son GET (sin efectos secundarios) y solo para ADMIN.
 *
 * Endpoints:
 *   GET /api/reports/sales?from=YYYY-MM-DD&to=YYYY-MM-DD
 *   GET /api/reports/packages/ranking?from=YYYY-MM-DD&to=YYYY-MM-DD
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/sales?from=2025-01-01&to=2025-03-31
     *
     * Retorna ventas totales en el período: cantidad de reservas CONFIRMED,
     * total de pasajeros y monto recaudado (excluye CANCELLED).
     */
    @GetMapping("/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getSalesReport(from, to));
    }

    /**
     * GET /api/reports/packages/ranking?from=2025-01-01&to=2025-03-31
     *
     * Retorna ranking de paquetes por número de reservas CONFIRMED en el período,
     * de mayor a menor. Incluye también total de pasajeros y revenue por paquete.
     */
    @GetMapping("/packages/ranking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PackageRankingResponse>> getPackageRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getPackageRanking(from, to));
    }
}
