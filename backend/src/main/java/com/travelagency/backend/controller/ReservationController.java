package com.travelagency.backend.controller;

import com.travelagency.backend.dto.CreateReservationRequest;
import com.travelagency.backend.dto.ReceiptResponse;
import com.travelagency.backend.dto.ReservationResponse;
import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.service.AuthenticatedUserService;
import com.travelagency.backend.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthenticatedUserService authenticatedUserService;

    /** POST /api/reservations — crea reserva para el usuario autenticado por Keycloak */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity user = authenticatedUserService.findOrCreateFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, user));
    }

    /** GET /api/reservations/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity user = authenticatedUserService.findOrCreateFromJwt(jwt);
        boolean admin = authenticatedUserService.isAdmin(jwt);
        return ResponseEntity.ok(reservationService.getReservationByIdForUser(id, user.getId(), admin));
    }

    /** GET /api/reservations — clientes ven sus reservas; admin puede filtrar por userId */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity user = authenticatedUserService.findOrCreateFromJwt(jwt);
        boolean admin = authenticatedUserService.isAdmin(jwt);
        Long targetUserId = admin && userId != null ? userId : user.getId();
        return ResponseEntity.ok(reservationService.getReservationsByUser(targetUserId));
    }

    /** DELETE /api/reservations/{id} — soft cancel, libera cupos */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity user = authenticatedUserService.findOrCreateFromJwt(jwt);
        boolean admin = authenticatedUserService.isAdmin(jwt);
        return ResponseEntity.ok(reservationService.cancelReservationForUser(id, user.getId(), admin));
    }

    /** GET /api/reservations/{id}/receipt — receipt/comprobante for a confirmed reservation */
    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('USER','CUSTOMER','ADMIN')")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity user = authenticatedUserService.findOrCreateFromJwt(jwt);
        boolean admin = authenticatedUserService.isAdmin(jwt);
        return ResponseEntity.ok(reservationService.generateReceipt(id, user.getId(), admin));
    }
}
