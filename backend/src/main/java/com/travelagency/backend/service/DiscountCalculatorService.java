package com.travelagency.backend.service;

import com.travelagency.backend.dto.DiscountBreakdown;
import com.travelagency.backend.model.ReservationStatus;
import com.travelagency.backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountCalculatorService {

    @Value("${discount.group.threshold:4}")      private int groupThreshold;
    @Value("${discount.group.percent:10}")        private int groupPercent;
    @Value("${discount.frequent-client.min-reservations:3}") private int frequentClientMin;
    @Value("${discount.frequent-client.percent:15}") private int frequentClientPercent;
    @Value("${discount.multi-package.days:30}")    private int multiPackageDays;
    @Value("${discount.multi-package.percent:5}")  private int multiPackagePercent;
    @Value("${discount.max-percent:20}")           private int maxPercent;
    @Value("${discount.promotion.percent:0}")      private int promotionPercent;
    @Value("${discount.promotion.start-date:}")    private String promotionStartDate;
    @Value("${discount.promotion.end-date:}")      private String promotionEndDate;

    private final ReservationRepository reservationRepository;

    public DiscountBreakdown calculate(Long userId, int passengerCount, long baseAmount) {
        List<String> descriptions = new ArrayList<>();

        // ─ 1. Descuento por grupo ─────────────────────────────────────
        int groupPct = 0;
        if (passengerCount >= groupThreshold) {
            groupPct = groupPercent;
            descriptions.add("Descuento por grupo (" + passengerCount + " pasajeros): " + groupPercent + "%");
        }

        // ─ 2. Descuento cliente frecuente ────────────────────────────
        int frequentPct = 0;
        long confirmedCount = reservationRepository
                .countByUser_IdAndStatus(userId, ReservationStatus.CONFIRMED);
        if (confirmedCount >= frequentClientMin) {
            frequentPct = frequentClientPercent;
            descriptions.add("Descuento cliente frecuente (" + confirmedCount + " reservas pagadas): " + frequentClientPercent + "%");
        }

        // ─ 3. Descuento por múltiples paquetes ───────────────────────
        // Solo contamos reservas CONFIRMED (pagadas) dentro de la ventana, así una
        // reserva PENDING_PAYMENT que luego expire no infla el descuento.
        int multiPct = 0;
        LocalDateTime windowStart = LocalDateTime.now().minusDays(multiPackageDays);
        long recentCount = reservationRepository
                .countByUser_IdAndStatusAndCreatedAtAfter(
                        userId,
                        ReservationStatus.CONFIRMED,
                        windowStart);
        if (recentCount > 0) {
            multiPct = multiPackagePercent;
            descriptions.add("Descuento por múltiples paquetes: " + multiPackagePercent + "%");
        }

        // ─ 4. Promoción temporal ─────────────────────────────────────
        int promoPct = 0;
        if (isPromotionActive()) {
            promoPct = promotionPercent;
            descriptions.add("Descuento promocional: " + promotionPercent + "%");
        }

        // ─ 5. Aplicar tope máximo ─────────────────────────────────────
        int total = Math.min(groupPct + frequentPct + multiPct + promoPct, maxPercent);

        // ─ 6. Calcular monto descontado en CLP ───────────────────────
        long discountAmount = Math.round(baseAmount * total / 100.0);

        return DiscountBreakdown.builder()
                .groupDiscountPercent(groupPct)
                .frequentClientDiscountPercent(frequentPct)
                .multiPackageDiscountPercent(multiPct)
                .promotionDiscountPercent(promoPct)
                .totalDiscountPercent(total)
                .totalDiscountAmount(discountAmount)
                .appliedDescriptions(descriptions)
                .build();
    }

    private boolean isPromotionActive() {
        if (promotionStartDate == null || promotionStartDate.isBlank()) return false;
        if (promotionEndDate   == null || promotionEndDate.isBlank())   return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(LocalDate.parse(promotionStartDate))
                && !today.isAfter(LocalDate.parse(promotionEndDate));
    }
}