package com.travelagency.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountBreakdown {

    private int groupDiscountPercent;
    private int frequentClientDiscountPercent;
    private int multiPackageDiscountPercent;
    private int promotionDiscountPercent;

    // Suma de los anteriores, con tope máximo aplicado (ej: 20%)
    private int totalDiscountPercent;

    // En pesos chilenos: Math.round(baseAmount * totalDiscountPercent / 100.0)
    private Long totalDiscountAmount;

    // Textos para mostrar al usuario: ["Descuento por grupo (4 pasajeros): 10%"]
    private List<String> appliedDescriptions;

    public DiscountBreakdown(int multiPackageDiscountPercent) {
        this.multiPackageDiscountPercent = multiPackageDiscountPercent;
    }
}