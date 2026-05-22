package com.travelagency.backend.service;

import com.travelagency.backend.dto.CreateReservationRequest;
import com.travelagency.backend.dto.DiscountBreakdown;
import com.travelagency.backend.dto.ReceiptResponse;
import com.travelagency.backend.dto.ReservationResponse;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.*;
import com.travelagency.backend.repository.PaymentRepository;
import com.travelagency.backend.repository.ReservationRepository;
import com.travelagency.backend.repository.TravelPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TravelPackageRepository packageRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountCalculatorService     discountCalculatorService;

    @Value("${reservation.payment-window-hours:24}")
    private int paymentWindowHours;

    // ══════════════════════════════════════════════════════════════
    //  CREAR RESERVA
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, UserEntity user) {

        // 0. Validar que el perfil del usuario esté completo.
        // Interpretación de Épica 1: el documento de identidad es información
        // "relevante para viajes" — no se puede emitir una reserva sin saber
        // a quién pertenece (teléfono de contacto + documento).
        validateUserProfileForReservation(user);

        // 1. Cargar entidades ──────────────────────────────────────────
        TravelPackageEntity pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paquete con id:" + request.getPackageId() + " no encontrado."));

        // 2. Validar estado del paquete ────────────────────────────────
        if (pkg.getStatus() != PackageStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    "No se puede reservar un paquete en estado: " + pkg.getStatus());
        }

        // 3. Validar que el paquete no haya comenzado ya ───────────────
        if (pkg.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "El paquete ya no está disponible (fecha de inicio pasada).");
        }

        // 4. Validar cupos ────────────────────────────────────────────
        if (request.getPassengerCount() > pkg.getAvailableSlots()) {
            throw new IllegalArgumentException(
                    "Cupos insuficientes. Disponibles: " + pkg.getAvailableSlots()
                            + ", solicitados: " + request.getPassengerCount());
        }

        // 5. Calcular montos y descuentos ──────────────────────────────
        long baseAmount = pkg.getPrice() * request.getPassengerCount();

        DiscountBreakdown breakdown = discountCalculatorService.calculate(
                user.getId(), request.getPassengerCount(), baseAmount);

        long finalAmount = baseAmount - breakdown.getTotalDiscountAmount();

        // 6. Construir la reserva ──────────────────────────────────────
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(user);
        reservation.setTravelPackage(pkg);
        reservation.setPassengerCount(request.getPassengerCount());
        reservation.setBaseAmount(baseAmount);
        reservation.setDiscountAmount(breakdown.getTotalDiscountAmount());
        reservation.setFinalAmount(finalAmount);
        reservation.setDiscountDetails(String.join(" | ", breakdown.getAppliedDescriptions()));
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);
        reservation.setExpiresAt(LocalDateTime.now().plusHours(paymentWindowHours));

        // 7. Descontar cupos del paquete ───────────────────────────────
        pkg.setAvailableSlots(pkg.getAvailableSlots() - request.getPassengerCount());
        if (pkg.getAvailableSlots() == 0) {
            pkg.setStatus(PackageStatus.SOLD_OUT);
        }
        packageRepository.save(pkg);

        return toResponse(reservationRepository.save(reservation),
                breakdown.getAppliedDescriptions());
    }

    // ══════════════════════════════════════════════════════════════
    //  CANCELAR RESERVA
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        ReservationEntity reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva con id:" + id + " no encontrada."));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("No se puede cancelar una reserva ya confirmada.");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("La reserva ya está cancelada.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        // Devolver cupos al paquete
        TravelPackageEntity pkg = reservation.getTravelPackage();
        pkg.setAvailableSlots(pkg.getAvailableSlots() + reservation.getPassengerCount());
        if (pkg.getStatus() == PackageStatus.SOLD_OUT) {
            pkg.setStatus(PackageStatus.AVAILABLE);
        }
        packageRepository.save(pkg);

        return toResponse(reservationRepository.save(reservation), List.of());
    }

    // ══════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        return toResponse(reservationRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reserva con id:" + id + " no encontrada.")),
                List.of());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationByIdForUser(Long id, Long userId, boolean admin) {
        ReservationEntity reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva con id:" + id + " no encontrada."));

        validateReservationOwner(reservation, userId, admin);
        return toResponse(reservation, List.of());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUser(Long userId) {
        return reservationRepository.findByUser_Id(userId)
                .stream()
                .map(r -> toResponse(r, List.of()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse cancelReservationForUser(Long id, Long userId, boolean admin) {
        ReservationEntity reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva con id:" + id + " no encontrada."));

        validateReservationOwner(reservation, userId, admin);
        return cancelReservation(id);
    }

    // ══════════════════════════════════════════════════════════════
    //  RECEIPT / COMPROBANTE (Épica 6)
    // ══════════════════════════════════════════════════════════════

    /**
     * Generates a receipt for a confirmed reservation.
     * Only CONFIRMED reservations have receipts (they must have been paid).
     *
     * @param reservationId the reservation to generate a receipt for
     * @param userId        the requesting user's ID
     * @param admin         whether the requesting user is an admin
     * @return receipt DTO with all reservation, client, package and payment details
     */
    @Transactional(readOnly = true)
    public ReceiptResponse generateReceipt(Long reservationId, Long userId, boolean admin) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva con id:" + reservationId + " no encontrada."));

        // Access control: users see own, admin sees all
        validateReservationOwner(reservation, userId, admin);

        // Only CONFIRMED reservations have receipts
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Solo se puede generar comprobante para reservas confirmadas. Estado actual: "
                            + reservation.getStatus());
        }

        UserEntity client = reservation.getUser();
        TravelPackageEntity pkg = reservation.getTravelPackage();

        // Build client document string from type + number
        String clientDocument = null;
        if (client.getIdentityDocumentType() != null && client.getIdentityDocumentNumber() != null) {
            clientDocument = client.getIdentityDocumentType() + ": " + client.getIdentityDocumentNumber();
        }

        // Look up payment info
        PaymentEntity payment = paymentRepository.findByReservationId(reservationId)
                .orElse(null);

        return ReceiptResponse.builder()
                .reservationId(reservation.getId())
                .reservationDate(reservation.getCreatedAt())
                .clientName(client.getFullName())
                .clientEmail(client.getEmail())
                .clientDocument(clientDocument)
                .packageName(pkg.getName())
                .destination(pkg.getDestination())
                .packageStartDate(pkg.getStartDate())
                .packageEndDate(pkg.getEndDate())
                .passengerCount(reservation.getPassengerCount())
                .baseAmount(reservation.getBaseAmount())
                .discountAmount(reservation.getDiscountAmount())
                .discountDetails(reservation.getDiscountDetails())
                .finalAmount(reservation.getFinalAmount())
                .paymentDate(payment != null ? payment.getPaidAt() : null)
                .cardLastFour(payment != null ? payment.getCardLastFour() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : null)
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    //  TAREA PROGRAMADA — expiración de reservas
    // ══════════════════════════════════════════════════════════════

    /**
     * Cada 60 segundos busca reservas PENDING_PAYMENT cuyo plazo de pago ha vencido.
     * Por cada una: cancela la reserva y devuelve los cupos al paquete.
     *
     * Regla de negocio: "If reservation expires unpaid: release slots".
     * @Scheduled es un mecanismo básico de Spring (no requiere dependencias extra).
     * fixedRate = 60_000 ms → se ejecuta cada minuto independientemente de cuánto tarda.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireUnpaidReservations() {
        List<ReservationEntity> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING_PAYMENT, LocalDateTime.now());

        if (expired.isEmpty()) return;

        log.info("Expirando {} reservas no pagadas...", expired.size());

        for (ReservationEntity reservation : expired) {
            reservation.setStatus(ReservationStatus.CANCELLED);

            TravelPackageEntity pkg = reservation.getTravelPackage();
            pkg.setAvailableSlots(pkg.getAvailableSlots() + reservation.getPassengerCount());
            if (pkg.getStatus() == PackageStatus.SOLD_OUT) {
                pkg.setStatus(PackageStatus.AVAILABLE);
            }
            packageRepository.save(pkg);
            reservationRepository.save(reservation);

            log.info("Reserva {} expirada. Cupos devueltos al paquete {}.",
                    reservation.getId(), pkg.getId());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPEO ENTIDAD → DTO
    // ══════════════════════════════════════════════════════════════
    private ReservationResponse toResponse(ReservationEntity r, List<String> discounts) {
        List<String> displayDiscounts = discounts.isEmpty() && r.getDiscountDetails() != null
                ? List.of(r.getDiscountDetails().split(" \\| "))
                : discounts;

        return ReservationResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userFullName(r.getUser().getFullName())
                .packageId(r.getTravelPackage().getId())
                .packageName(r.getTravelPackage().getName())
                .packageDestination(r.getTravelPackage().getDestination())
                .passengerCount(r.getPassengerCount())
                .baseAmount(r.getBaseAmount())
                .discountAmount(r.getDiscountAmount())
                .finalAmount(r.getFinalAmount())
                .appliedDiscounts(displayDiscounts)
                .status(r.getStatus())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private void validateReservationOwner(ReservationEntity reservation, Long userId, boolean admin) {
        if (admin) {
            return;
        }

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("No puedes acceder a reservas de otro usuario.");
        }
    }

    /**
     * Valida que el usuario tenga los campos mínimos para emitir una reserva:
     *  - phone
     *  - identityDocumentType
     *  - identityDocumentNumber
     *
     * Nationality queda como opcional. Acumulamos los campos faltantes para
     * devolver un único mensaje claro al cliente en vez de fallar de uno en uno.
     */
    private void validateUserProfileForReservation(UserEntity user) {
        List<String> missing = new java.util.ArrayList<>();
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            missing.add("teléfono");
        }
        if (user.getIdentityDocumentType() == null) {
            missing.add("tipo de documento");
        }
        if (user.getIdentityDocumentNumber() == null || user.getIdentityDocumentNumber().isBlank()) {
            missing.add("número de documento");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes completar tu perfil antes de reservar. Faltan: "
                            + String.join(", ", missing) + ".");
        }
    }
}
