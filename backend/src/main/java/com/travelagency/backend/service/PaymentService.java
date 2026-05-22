package com.travelagency.backend.service;

import com.travelagency.backend.dto.PaymentResponse;
import com.travelagency.backend.dto.ProcessPaymentRequest;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.PaymentEntity;
import com.travelagency.backend.model.PaymentStatus;
import com.travelagency.backend.model.ReservationEntity;
import com.travelagency.backend.model.ReservationStatus;
import com.travelagency.backend.repository.PaymentRepository;
import com.travelagency.backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request, Long userId, boolean admin) {

        // 1. Buscar la reserva
        ReservationEntity reservation = reservationRepository
                .findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con id: " + request.getReservationId()));

        if (!admin && !reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("No puedes pagar reservas de otro usuario.");
        }

        // 2. REGLA: no se puede pagar una reserva cancelada
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede pagar una reserva cancelada.");
        }

        // 3. REGLA: no se puede pagar una reserva ya confirmada (ya tiene pago)
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Esta reserva ya fue pagada y confirmada.");
        }

        // 4. REGLA: verificar que no exista ya un pago para esta reserva
        if (paymentRepository.existsByReservationId(reservation.getId())) {
            throw new IllegalArgumentException("Ya existe un pago para esta reserva.");
        }

        // 5. Crear el pago (simulado — siempre exitoso)
        PaymentEntity payment = new PaymentEntity();
        payment.setReservation(reservation);
        payment.setAmount(reservation.getFinalAmount());  // monto completo
        payment.setCardLastFour(request.getCardLastFour());
        payment.setCardHolderName(request.getCardHolderName());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        // 6. REGLA: el pago auto-confirma la reserva
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        PaymentEntity saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    private PaymentResponse toResponse(PaymentEntity p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .reservationId(p.getReservation().getId())
                .amount(p.getAmount())
                .cardLastFour(p.getCardLastFour())
                .cardHolderName(p.getCardHolderName())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .build();
    }

}
