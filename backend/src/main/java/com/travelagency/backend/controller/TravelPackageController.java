package com.travelagency.backend.controller;
import com.travelagency.backend.dto.CreatePackageRequest;
import com.travelagency.backend.dto.PackageResponse;
import com.travelagency.backend.dto.UpdatePackageRequest;
import com.travelagency.backend.service.TravelPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController                          // Combina @Controller + @ResponseBody
@RequestMapping("/api/packages")         // Prefijo base de todos los endpoints
@RequiredArgsConstructor
public class TravelPackageController {

    private final TravelPackageService packageService;

    // POST /api/packages — solo ADMIN puede crear
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> createPackage(
            @Valid @RequestBody CreatePackageRequest request) {
        // @Valid activa Bean Validation → valida @NotBlank, @Positive, etc.
        // @RequestBody deserializa el JSON del body al DTO
        PackageResponse response = packageService.createPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        // 201 Created — estándar REST para creación exitosa
    }

    // GET /api/packages — público
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
        // 200 OK — ResponseEntity.ok() es shorthand
    }

    // GET /api/packages/available — público
    // IMPORTANTE: este endpoint debe ir ANTES de /{id} para no confundirse con /available como id
    @GetMapping("/available")
    public ResponseEntity<List<PackageResponse>> getAvailablePackages() {
        return ResponseEntity.ok(packageService.getAvailablePackages());
    }
    @GetMapping("/search")
    public ResponseEntity<List<PackageResponse>> searchPackages(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String packageType) {

        return ResponseEntity.ok(packageService.searchPackages(
                destination, minPrice, maxPrice, startDate, endDate, packageType));
    }

    // GET /api/packages/{id} — público
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackageById(@PathVariable Long id) {
        // @PathVariable extrae {id} de la URL
        return ResponseEntity.ok(packageService.getPackageById(id));
    }



    // PUT /api/packages/{id} — solo ADMIN
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageRequest request) {
        return ResponseEntity.ok(packageService.updatePackage(id, request));
    }

    // DELETE /api/packages/{id} — solo ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.noContent().build();
        // 204 No Content — estándar REST para DELETE exitoso sin body de respuesta
    }

    // POST /api/packages/{id}/publish — solo ADMIN
    // Re-publica un paquete cancelado (transición CANCELLED → AVAILABLE).
    // Verbo POST porque es una acción de dominio, no un update genérico.
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> publishPackage(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.publishPackage(id));
    }
}