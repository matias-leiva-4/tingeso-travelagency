package com.travelagency.backend.controller;

import com.travelagency.backend.dto.CreatePackageRequest;
import com.travelagency.backend.dto.PackageResponse;
import com.travelagency.backend.dto.UpdatePackageRequest;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.PackageStatus;
import com.travelagency.backend.service.TravelPackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del TravelPackageController.
 *
 * Estrategia: llamamos los métodos del controller directamente (sin HTTP/MockMvc).
 * Esto es suficiente para verificar que el controller:
 *   - Devuelve el ResponseEntity con el status correcto
 *   - Delega correctamente al service
 *   - No contiene lógica de negocio propia
 *
 * JaCoCo cuenta estas líneas como cubiertas igual que con MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class TravelPackageControllerTest {

    @Mock TravelPackageService packageService;
    @InjectMocks TravelPackageController controller;

    private PackageResponse sample;

    @BeforeEach
    void setUp() {
        sample = PackageResponse.builder()
                .id(1L).name("Tour Atacama").destination("Atacama")
                .description("Desierto").price(150_000L)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .totalSlots(20).availableSlots(20)
                .status(PackageStatus.AVAILABLE)
                .build();
    }

    // ── GET /api/packages ─────────────────────────────────────────────────────

    @Test
    void getAllPackages_returns200WithList() {
        when(packageService.getAllPackages()).thenReturn(List.of(sample));

        ResponseEntity<List<PackageResponse>> response = controller.getAllPackages();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Tour Atacama");
    }

    // ── GET /api/packages/available ───────────────────────────────────────────

    @Test
    void getAvailablePackages_returns200() {
        when(packageService.getAvailablePackages()).thenReturn(List.of(sample));

        ResponseEntity<List<PackageResponse>> response = controller.getAvailablePackages();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    // ── GET /api/packages/{id} ────────────────────────────────────────────────

    @Test
    void getPackageById_found_returns200() {
        when(packageService.getPackageById(1L)).thenReturn(sample);

        ResponseEntity<PackageResponse> response = controller.getPackageById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getPackageById_notFound_propagatesException() {
        when(packageService.getPackageById(99L))
                .thenThrow(new ResourceNotFoundException("No encontrado"));

        assertThatThrownBy(() -> controller.getPackageById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── GET /api/packages/search ──────────────────────────────────────────────

    @Test
    void searchPackages_returns200WithResults() {
        when(packageService.searchPackages(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(sample));

        ResponseEntity<List<PackageResponse>> response =
                controller.searchPackages("Atacama", null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    // ── POST /api/packages ────────────────────────────────────────────────────

    @Test
    void createPackage_returns201WithBody() {
        when(packageService.createPackage(any())).thenReturn(sample);

        ResponseEntity<PackageResponse> response =
                controller.createPackage(new CreatePackageRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getName()).isEqualTo("Tour Atacama");
        verify(packageService).createPackage(any(CreatePackageRequest.class));
    }

    // ── PUT /api/packages/{id} ────────────────────────────────────────────────

    @Test
    void updatePackage_returns200WithBody() {
        when(packageService.updatePackage(eq(1L), any())).thenReturn(sample);

        ResponseEntity<PackageResponse> response =
                controller.updatePackage(1L, new UpdatePackageRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(packageService).updatePackage(eq(1L), any(UpdatePackageRequest.class));
    }

    // ── DELETE /api/packages/{id} ─────────────────────────────────────────────

    @Test
    void deletePackage_returns204NoContent() {
        doNothing().when(packageService).deletePackage(1L);

        ResponseEntity<Void> response = controller.deletePackage(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(packageService).deletePackage(1L);
    }
}
