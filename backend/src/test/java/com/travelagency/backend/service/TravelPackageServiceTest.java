package com.travelagency.backend.service;

import com.travelagency.backend.dto.CreatePackageRequest;
import com.travelagency.backend.dto.PackageResponse;
import com.travelagency.backend.dto.UpdatePackageRequest;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.PackageStatus;
import com.travelagency.backend.model.TravelPackageEntity;
import com.travelagency.backend.repository.TravelPackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelPackageServiceTest {

    @Mock  TravelPackageRepository packageRepository;
    @InjectMocks TravelPackageService packageService;

    private TravelPackageEntity samplePackage;

    @BeforeEach
    void setUp() {
        samplePackage = new TravelPackageEntity();
        samplePackage.setId(1L);
        samplePackage.setName("Tour Atacama");
        samplePackage.setDestination("Atacama");
        samplePackage.setDescription("Desierto y cielo estrellado");
        samplePackage.setStartDate(LocalDate.now().plusDays(10));
        samplePackage.setEndDate(LocalDate.now().plusDays(15));
        samplePackage.setPrice(150_000L);
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(20);
        samplePackage.setStatus(PackageStatus.AVAILABLE);
    }

    // ── createPackage ──────────────────────────────────────────────────────────

    @Test
    void createPackage_success_setsAvailableSlotsEqualToTotal() {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setName("Tour Atacama");
        req.setDestination("Atacama");
        req.setDescription("Desierto");
        req.setStartDate(LocalDate.now().plusDays(5));
        req.setEndDate(LocalDate.now().plusDays(10));
        req.setPrice(100_000L);
        req.setTotalSlots(30);

        when(packageRepository.save(any())).thenAnswer(inv -> {
            TravelPackageEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        PackageResponse response = packageService.createPackage(req);

        assertThat(response.getAvailableSlots()).isEqualTo(30);
        assertThat(response.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
        verify(packageRepository).save(any(TravelPackageEntity.class));
    }

    @Test
    void createPackage_endDateBeforeStartDate_throwsIllegalArgument() {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setName("Tour");
        req.setDestination("X");
        req.setDescription("desc");
        req.setStartDate(LocalDate.now().plusDays(10));
        req.setEndDate(LocalDate.now().plusDays(5));  // antes de start
        req.setPrice(50_000L);
        req.setTotalSlots(10);

        assertThatThrownBy(() -> packageService.createPackage(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha de término");
    }

    @Test
    void createPackage_endDateSameAsStartDate_throwsIllegalArgument() {
        LocalDate same = LocalDate.now().plusDays(5);
        CreatePackageRequest req = new CreatePackageRequest();
        req.setName("Tour"); req.setDestination("X"); req.setDescription("d");
        req.setStartDate(same); req.setEndDate(same);
        req.setPrice(10_000L); req.setTotalSlots(5);

        assertThatThrownBy(() -> packageService.createPackage(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updatePackage ──────────────────────────────────────────────────────────

    @Test
    void updatePackage_cancelled_throwsIllegalArgument() {
        samplePackage.setStatus(PackageStatus.CANCELLED);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> packageService.updatePackage(1L, new UpdatePackageRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void updatePackage_reduceSlotsBelowReserved_throwsIllegalArgument() {
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(15);  // 5 slots reservados
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setTotalSlots(3);  // menos que los 5 reservados

        assertThatThrownBy(() -> packageService.updatePackage(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cupos totales");
    }

    @Test
    void updatePackage_changeDatesWithReservations_throwsIllegalArgument() {
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(15);  // 5 reservados
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setStartDate(LocalDate.now().plusDays(20));  // intenta cambiar fecha

        assertThatThrownBy(() -> packageService.updatePackage(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechas");
    }

    @Test
    void updatePackage_setAvailableWhenZeroSlots_throwsIllegalArgument() {
        samplePackage.setTotalSlots(10);
        samplePackage.setAvailableSlots(0);   // sin cupos
        samplePackage.setStatus(PackageStatus.SOLD_OUT);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setStatus(PackageStatus.AVAILABLE);  // intenta publicar sin cupos

        assertThatThrownBy(() -> packageService.updatePackage(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cupos disponibles");
    }

    @Test
    void updatePackage_increaseTotalSlots_updatesAvailableCorrectly() {
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(15);  // 5 reservados
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setTotalSlots(30);  // aumentar de 20 a 30

        PackageResponse response = packageService.updatePackage(1L, req);

        // availableSlots = 30 - 5 (reservados) = 25
        assertThat(response.getAvailableSlots()).isEqualTo(25);
        assertThat(response.getTotalSlots()).isEqualTo(30);
    }

    // ── deletePackage ──────────────────────────────────────────────────────────

    @Test
    void deletePackage_setsStatusCancelled() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        packageService.deletePackage(1L);

        assertThat(samplePackage.getStatus()).isEqualTo(PackageStatus.CANCELLED);
        verify(packageRepository).save(samplePackage);
    }

    @Test
    void deletePackage_alreadyCancelled_throwsIllegalArgument() {
        samplePackage.setStatus(PackageStatus.CANCELLED);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> packageService.deletePackage(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletePackage_withActiveReservations_throwsIllegalArgument() {
        // 20 totales, 7 disponibles → 13 reservados → no se puede cancelar
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(7);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> packageService.deletePackage(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservas activas");
        verify(packageRepository, never()).save(any());
    }

    // ── publishPackage ─────────────────────────────────────────────────────────

    @Test
    void publishPackage_setsStatusAvailable() {
        samplePackage.setStatus(PackageStatus.CANCELLED);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PackageResponse response = packageService.publishPackage(1L);

        assertThat(samplePackage.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
        assertThat(response.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
        verify(packageRepository).save(samplePackage);
    }

    @Test
    void publishPackage_notCancelled_throwsIllegalArgument() {
        // Ya está AVAILABLE — no aplica re-publicar
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> packageService.publishPackage(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelados");
        verify(packageRepository, never()).save(any());
    }

    @Test
    void publishPackage_startDateInPast_throwsIllegalArgument() {
        samplePackage.setStatus(PackageStatus.CANCELLED);
        samplePackage.setStartDate(LocalDate.now().minusDays(1));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> packageService.publishPackage(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya pasó");
        verify(packageRepository, never()).save(any());
    }

    @Test
    void publishPackage_noAvailableSlots_throwsIllegalArgument() {
        samplePackage.setStatus(PackageStatus.CANCELLED);
        samplePackage.setAvailableSlots(0);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        assertThatThrownBy(() -> packageService.publishPackage(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cupos");
        verify(packageRepository, never()).save(any());
    }

    @Test
    void publishPackage_packageNotFound_throwsResourceNotFound() {
        when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService.publishPackage(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPackageById ─────────────────────────────────────────────────────────

    @Test
    void getPackageById_notFound_throwsResourceNotFoundException() {
        when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService.getPackageById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPackageById_found_returnsResponse() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));

        PackageResponse response = packageService.getPackageById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Tour Atacama");
    }

    // ── getAvailablePackages ───────────────────────────────────────────────────

    @Test
    void getAvailablePackages_returnsOnlyAvailablePackages() {
        TravelPackageEntity soldOut = new TravelPackageEntity();
        soldOut.setId(2L); soldOut.setStatus(PackageStatus.SOLD_OUT);

        when(packageRepository.findByStatus(PackageStatus.AVAILABLE))
                .thenReturn(List.of(samplePackage));

        List<PackageResponse> result = packageService.getAvailablePackages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PackageStatus.AVAILABLE);
    }

    // ── getAllPackages ─────────────────────────────────────────────────────────

    @Test
    void getAllPackages_returnsMappedList() {
        TravelPackageEntity second = new TravelPackageEntity();
        second.setId(2L);
        second.setName("Tour Atacama 2");
        second.setDestination("Atacama");
        second.setStartDate(LocalDate.now().plusDays(20));
        second.setEndDate(LocalDate.now().plusDays(25));
        second.setPrice(200_000L);
        second.setTotalSlots(10);
        second.setAvailableSlots(10);
        second.setStatus(PackageStatus.SOLD_OUT);

        when(packageRepository.findAll()).thenReturn(List.of(samplePackage, second));

        List<PackageResponse> result = packageService.getAllPackages();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(PackageStatus.AVAILABLE);
        assertThat(result.get(1).getStatus()).isEqualTo(PackageStatus.SOLD_OUT);
        verify(packageRepository).findAll();
    }

    // ── searchPackages ─────────────────────────────────────────────────────────

    @Test
    void searchPackages_delegatesToRepository() {
        // El service ahora sustituye nulls numericos/fechas por sentinelas
        // (0, Long.MAX_VALUE, LocalDate(1900,1,1), LocalDate(9999,12,31))
        // para evitar el bug de Postgres + Hibernate 6 con parametros null.
        when(packageRepository.searchPackages(
                any(LocalDate.class), eq("Atacama"),
                eq(0L), eq(Long.MAX_VALUE),
                eq(LocalDate.of(1900, 1, 1)), eq(LocalDate.of(9999, 12, 31)),
                isNull()))
                .thenReturn(List.of(samplePackage));

        List<PackageResponse> result =
                packageService.searchPackages("Atacama", null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDestination()).isEqualTo("Atacama");
        verify(packageRepository).searchPackages(
                any(LocalDate.class), eq("Atacama"),
                eq(0L), eq(Long.MAX_VALUE),
                eq(LocalDate.of(1900, 1, 1)), eq(LocalDate.of(9999, 12, 31)),
                isNull());
    }

    @Test
    void searchPackages_noFilters_returnsAll() {
        // Sin filtros: strings null, numericos/fechas sustituidos por sentinelas.
        when(packageRepository.searchPackages(
                any(LocalDate.class), isNull(),
                eq(0L), eq(Long.MAX_VALUE),
                eq(LocalDate.of(1900, 1, 1)), eq(LocalDate.of(9999, 12, 31)),
                isNull()))
                .thenReturn(List.of(samplePackage));

        List<PackageResponse> result =
                packageService.searchPackages(null, null, null, null, null, null);

        assertThat(result).hasSize(1);
    }

    // ── updatePackage — auto status-sync ──────────────────────────────────────

    @Test
    void updatePackage_reduceSlotsToZero_autoSetsSoldOut() {
        // Paquete con 20 cupos totales, 15 disponibles (5 reservados)
        // → reducir totalSlots a 5 deja availableSlots = 0 → auto SOLD_OUT
        samplePackage.setTotalSlots(20);
        samplePackage.setAvailableSlots(15);  // 5 reservados
        samplePackage.setStatus(PackageStatus.AVAILABLE);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setTotalSlots(5);  // 5 total - 5 reservados = 0 disponibles

        PackageResponse response = packageService.updatePackage(1L, req);

        assertThat(response.getAvailableSlots()).isZero();
        assertThat(response.getStatus()).isEqualTo(PackageStatus.SOLD_OUT);
    }

    @Test
    void updatePackage_soldOutPackageGetsMoreSlots_autoSetsAvailable() {
        // Paquete SOLD_OUT sin cupos disponibles → agregar cupos → auto AVAILABLE
        samplePackage.setTotalSlots(10);
        samplePackage.setAvailableSlots(0);  // todos reservados
        samplePackage.setStatus(PackageStatus.SOLD_OUT);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setTotalSlots(15);  // agrega 5 cupos

        PackageResponse response = packageService.updatePackage(1L, req);

        assertThat(response.getAvailableSlots()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
    }

    @Test
    void updatePackage_nameAndPrice_updatesFieldsOnly() {
        // Sin cambios de slots ni fechas → solo actualiza nombre y precio
        when(packageRepository.findById(1L)).thenReturn(Optional.of(samplePackage));
        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePackageRequest req = new UpdatePackageRequest();
        req.setName("Tour Atacama Premium");
        req.setPrice(200_000L);

        PackageResponse response = packageService.updatePackage(1L, req);

        assertThat(response.getName()).isEqualTo("Tour Atacama Premium");
        assertThat(response.getPrice()).isEqualTo(200_000L);
        // El status no debería cambiar (AVAILABLE con cupos)
        assertThat(response.getStatus()).isEqualTo(PackageStatus.AVAILABLE);
    }
}
