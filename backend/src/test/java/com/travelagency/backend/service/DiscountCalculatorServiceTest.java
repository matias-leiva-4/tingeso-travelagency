package com.travelagency.backend.service;

import com.travelagency.backend.dto.DiscountBreakdown;
import com.travelagency.backend.model.ReservationStatus;
import com.travelagency.backend.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests del DiscountCalculatorService.
 *
 * Usamos ReflectionTestUtils para inyectar los @Value de configuración
 * sin necesidad de levantar el contexto de Spring (test unitario puro).
 */
@ExtendWith(MockitoExtension.class)
class DiscountCalculatorServiceTest {

    @Mock  ReservationRepository reservationRepository;
    @InjectMocks DiscountCalculatorService discountService;

    @BeforeEach
    void injectConfigValues() {
        // Simula los @Value que Spring inyectaría desde application.properties
        ReflectionTestUtils.setField(discountService, "groupThreshold",        4);
        ReflectionTestUtils.setField(discountService, "groupPercent",          10);
        ReflectionTestUtils.setField(discountService, "frequentClientMin",     3);
        ReflectionTestUtils.setField(discountService, "frequentClientPercent", 15);
        ReflectionTestUtils.setField(discountService, "multiPackageDays",      30);
        ReflectionTestUtils.setField(discountService, "multiPackagePercent",   5);
        ReflectionTestUtils.setField(discountService, "maxPercent",            20);
        ReflectionTestUtils.setField(discountService, "promotionPercent",      0);
        ReflectionTestUtils.setField(discountService, "promotionStartDate",    "");
        ReflectionTestUtils.setField(discountService, "promotionEndDate",      "");
    }

    // ── Sin descuentos ─────────────────────────────────────────────────────────

    @Test
    void calculate_noConditionsMet_returnsZeroDiscount() {
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(0L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 2, 100_000L);

        assertThat(result.getTotalDiscountPercent()).isZero();
        assertThat(result.getTotalDiscountAmount()).isZero();
        assertThat(result.getAppliedDescriptions()).isEmpty();
    }

    // ── Descuento por grupo ────────────────────────────────────────────────────

    @Test
    void calculate_groupDiscount_appliedWhenPassengersAtThreshold() {
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(0L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 4, 200_000L);

        assertThat(result.getGroupDiscountPercent()).isEqualTo(10);
        assertThat(result.getAppliedDescriptions())
                .anyMatch(d -> d.contains("grupo"));
    }

    @Test
    void calculate_groupDiscount_notAppliedBelowThreshold() {
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(0L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 3, 100_000L);

        assertThat(result.getGroupDiscountPercent()).isZero();
    }

    // ── Descuento cliente frecuente ────────────────────────────────────────────

    @Test
    void calculate_frequentClientDiscount_applied() {
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(3L);   // exactamente el mínimo
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 1, 100_000L);

        assertThat(result.getFrequentClientDiscountPercent()).isEqualTo(15);
        assertThat(result.getAppliedDescriptions())
                .anyMatch(d -> d.contains("frecuente"));
    }

    @Test
    void calculate_frequentClientDiscount_notAppliedBelowMinimum() {
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(2L);  // menos del mínimo (3)
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 1, 100_000L);

        assertThat(result.getFrequentClientDiscountPercent()).isZero();
    }

    // ── Descuento multi-paquete ────────────────────────────────────────────────

    @Test
    void calculate_multiPackageDiscount_appliedWhenRecentReservationsExist() {
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(0L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(1L);  // ya tiene una reserva reciente

        DiscountBreakdown result = discountService.calculate(1L, 1, 100_000L);

        assertThat(result.getMultiPackageDiscountPercent()).isEqualTo(5);
        assertThat(result.getAppliedDescriptions())
                .anyMatch(d -> d.contains("múltiples paquetes"));
    }

    // ── Tope máximo ───────────────────────────────────────────────────────────

    @Test
    void calculate_allDiscounts_cappedAtMaxPercent() {
        // grupo(10) + frecuente(15) + multi(5) = 30% → recorta a 20%
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(5L);  // cliente frecuente
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(2L);  // multi-paquete

        // 4 pasajeros → grupo
        DiscountBreakdown result = discountService.calculate(1L, 4, 100_000L);

        assertThat(result.getTotalDiscountPercent()).isEqualTo(20);  // tope
        assertThat(result.getTotalDiscountAmount()).isEqualTo(20_000L);  // 20% de 100.000
    }

    @Test
    void calculate_discountAmountNeverExceedsBaseAmount() {
        // Con 20% de descuento sobre 50.000 → 10.000, nunca negativo
        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(5L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(2L);

        DiscountBreakdown result = discountService.calculate(1L, 4, 50_000L);

        long finalAmount = 50_000L - result.getTotalDiscountAmount();
        assertThat(finalAmount).isGreaterThanOrEqualTo(0L);
    }

    // ── Promoción temporal ────────────────────────────────────────────────────

    @Test
    void calculate_promotionActive_appliesExtraDiscount() {
        // Activa una promoción de 5% para hoy
        ReflectionTestUtils.setField(discountService, "promotionPercent",   5);
        ReflectionTestUtils.setField(discountService, "promotionStartDate", "2020-01-01");
        ReflectionTestUtils.setField(discountService, "promotionEndDate",   "2099-12-31");

        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(0L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 1, 100_000L);

        assertThat(result.getPromotionDiscountPercent()).isEqualTo(5);
        assertThat(result.getAppliedDescriptions())
                .anyMatch(d -> d.contains("promocional"));
    }

    @Test
    void calculate_promotionInactive_noExtraDiscount() {
        // Fechas en el pasado → promoción inactiva
        ReflectionTestUtils.setField(discountService, "promotionPercent",   10);
        ReflectionTestUtils.setField(discountService, "promotionStartDate", "2020-01-01");
        ReflectionTestUtils.setField(discountService, "promotionEndDate",   "2020-12-31");

        when(reservationRepository.countByUser_IdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(0L);
        when(reservationRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                eq(1L), eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(0L);

        DiscountBreakdown result = discountService.calculate(1L, 1, 100_000L);

        assertThat(result.getPromotionDiscountPercent()).isZero();
    }
}
