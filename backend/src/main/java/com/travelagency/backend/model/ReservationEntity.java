package com.travelagency.backend.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"user","travelPackage"})
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackageEntity travelPackage;

    @Column(nullable = false)
    private Integer passengerCount;

    // Precio base ANTES de descuentos: package.price * passengerCount
    @Column(nullable = false)
    private Long baseAmount;

    // Cuánto se descontó en total (en pesos chilenos)
    @Column(nullable = false)
    private Long discountAmount;

    // Lo que el usuario REALMENTE paga: baseAmount - discountAmount
    @Column(nullable = false)
    private Long finalAmount;

    // Texto legible: "Descuento por grupo: 10% | Cliente frecuente: 15%"
    @Column(columnDefinition = "TEXT")
    private String discountDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING_PAYMENT;

    // Cuándo vence el plazo para pagar
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
