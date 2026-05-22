package com.travelagency.backend.service;

import com.travelagency.backend.dto.PackageRankingResponse;
import com.travelagency.backend.dto.ReservationDetailResponse;
import com.travelagency.backend.dto.SalesReportResponse;
import com.travelagency.backend.model.ReservationEntity;
import com.travelagency.backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de reportes administrativos (Épica 7).
 * Solo operaciones de lectura — @Transactional(readOnly = true) para optimización.
 *
 * Diseño: los cálculos se hacen en Java sobre la lista ya traída de la BD,
 * excepto el ranking que agrupa directamente en JPQL para eficiencia.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReservationRepository reservationRepository;

    // ══════════════════════════════════════════════════════════════
    //  REPORTE 1 — Ventas por período
    // ══════════════════════════════════════════════════════════════

    /**
     * Agrega reservas CONFIRMED entre [from, to] inclusive.
     * Excluye CANCELLED (regla del enunciado) y PENDING_PAYMENT.
     *
     * @param from fecha de inicio del período (inclusive)
     * @param to   fecha de fin del período (inclusive)
     * @return totalReservations, totalPassengers y totalRevenue del período
     */
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "La fecha de fin ('to') debe ser igual o posterior a la fecha de inicio ('from').");
        }

        List<ReservationEntity> confirmed = reservationRepository.findConfirmedBetween(from, to);

        long totalRevenue    = confirmed.stream().mapToLong(ReservationEntity::getFinalAmount).sum();
        long totalPassengers = confirmed.stream().mapToLong(r -> r.getPassengerCount().longValue()).sum();

        List<ReservationDetailResponse> details = confirmed.stream()
                .map(this::toDetailResponse)
                .toList();

        return SalesReportResponse.builder()
                .from(from)
                .to(to)
                .totalReservations(confirmed.size())
                .totalPassengers(totalPassengers)
                .totalRevenue(totalRevenue)
                .reservations(details)
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    //  REPORTE 2 — Ranking de paquetes por período
    // ══════════════════════════════════════════════════════════════

    /**
     * Ranking de paquetes ordenado por número de reservas descendente.
     * La query JPQL hace el GROUP BY en la BD: eficiente aunque la tabla sea grande.
     *
     * @param from fecha de inicio del período (inclusive)
     * @param to   fecha de fin del período (inclusive)
     * @return lista de paquetes con sus totales, ordenada de mayor a menor reservas
     */
    @Transactional(readOnly = true)
    public List<PackageRankingResponse> getPackageRanking(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "La fecha de fin ('to') debe ser igual o posterior a la fecha de inicio ('from').");
        }

        return reservationRepository.findRankedPackagesBetween(from, to)
                .stream()
                .map(row -> PackageRankingResponse.builder()
                        .packageId((Long) row[0])
                        .packageName((String) row[1])
                        .destination((String) row[2])
                        .totalReservations((Long) row[3])
                        .totalPassengers((Long) row[4])
                        .totalRevenue((Long) row[5])
                        .build())
                .toList();
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPPING HELPER
    // ══════════════════════════════════════════════════════════════

    /**
     * Maps a ReservationEntity to a ReservationDetailResponse for the sales report.
     * Assumes user and travelPackage are already fetched (JOIN FETCH in query).
     */
    private ReservationDetailResponse toDetailResponse(ReservationEntity r) {
        return ReservationDetailResponse.builder()
                .reservationId(r.getId())
                .reservationDate(r.getCreatedAt())
                .clientName(r.getUser().getFullName())
                .clientEmail(r.getUser().getEmail())
                .packageName(r.getTravelPackage().getName())
                .destination(r.getTravelPackage().getDestination())
                .passengerCount(r.getPassengerCount())
                .baseAmount(r.getBaseAmount())
                .discountAmount(r.getDiscountAmount())
                .finalAmount(r.getFinalAmount())
                .status(r.getStatus().name())
                .build();
    }
}
