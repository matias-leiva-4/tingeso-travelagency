package com.travelagency.backend.controller;


import com.travelagency.backend.dto.*;
import com.travelagency.backend.model.UserEntity;
import com.travelagency.backend.service.AuthenticatedUserService;
import com.travelagency.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthenticatedUserService authenticatedUserService;

    // POST /api/payments — usuario autenticado
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserEntity user = authenticatedUserService.findOrCreateFromJwt(jwt);
        boolean admin = authenticatedUserService.isAdmin(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request, user.getId(), admin));
    }
}
