package com.travelagency.backend.repository;

import com.travelagency.backend.model.ReservationEntity;
import com.travelagency.backend.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    // Guion bajo navega la relación: user_Id → user.getId()
    List<ReservationEntity> findByUser_Id(Long userId);

    // Para descuento cliente frecuente: contar reservas CONFIRMADAS del usuario
    long countByUser_IdAndStatus(Long userId, ReservationStatus status);

    // Para descuento multi-paquete: reservas recientes en cualquier estado activo
    long countByUser_IdAndStatusInAndCreatedAtAfter(
            Long userId,
            List<ReservationStatus> statuses,
            LocalDateTime after
    );

    // Para descuento multi-paquete: cuenta reservas CONFIRMED dentro de la ventana.
    // Se exige CONFIRMED (pagadas) para evitar que reservas PENDING_PAYMENT que luego
    // expiren inflen artificialmente el descuento.
    long countByUser_IdAndStatusAndCreatedAtAfter(
            Long userId,
            ReservationStatus status,
            LocalDateTime after
    );

    // Para el @Scheduled que expira reservas no pagadas
    List<ReservationEntity> findByStatusAndExpiresAtBefore(
            ReservationStatus status,
            LocalDateTime now
    );

    // ── Épica 7 — Reportes ──────────────────────────────────────────────────

    /**
     * Retorna todas las reservas CONFIRMED cuya fecha de creación cae dentro del período.
     * CAST(createdAt AS localdate) convierte LocalDateTime → LocalDate para comparar
     * contra los parámetros de fecha sin necesidad de horas/minutos.
     * JOIN FETCH for user and travelPackage avoids N+1 queries when mapping to detail DTOs.
     * Usado en el reporte de ventas por período.
     */
    @Query("""
        SELECT r FROM ReservationEntity r
        JOIN FETCH r.user
        JOIN FETCH r.travelPackage
        WHERE r.status = com.travelagency.backend.model.ReservationStatus.CONFIRMED
        AND CAST(r.createdAt AS localdate) BETWEEN :from AND :to
    """)
    List<ReservationEntity> findConfirmedBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Ranking de paquetes: agrupa por paquete, cuenta reservas y suma pasajeros/revenue.
     * Retorna Object[] por fila: [packageId, packageName, destination,
     *                             count(reservas), sum(pasajeros), sum(revenue)]
     * Ordenado por count DESC, then by total revenue DESC as tiebreaker.
     * Usado en el reporte de ranking por período.
     */
    @Query("""
        SELECT r.travelPackage.id,
               r.travelPackage.name,
               r.travelPackage.destination,
               COUNT(r),
               SUM(r.passengerCount),
               SUM(r.finalAmount)
        FROM ReservationEntity r
        WHERE r.status = com.travelagency.backend.model.ReservationStatus.CONFIRMED
        AND CAST(r.createdAt AS localdate) BETWEEN :from AND :to
        GROUP BY r.travelPackage.id, r.travelPackage.name, r.travelPackage.destination
        ORDER BY COUNT(r) DESC, SUM(r.finalAmount) DESC
    """)
    List<Object[]> findRankedPackagesBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}