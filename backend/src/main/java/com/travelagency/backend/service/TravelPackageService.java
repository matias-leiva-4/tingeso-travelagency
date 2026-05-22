package com.travelagency.backend.service;

import com.travelagency.backend.dto.CreatePackageRequest;
import com.travelagency.backend.dto.PackageResponse;
import com.travelagency.backend.dto.UpdatePackageRequest;
import com.travelagency.backend.exception.ResourceNotFoundException;
import com.travelagency.backend.model.PackageStatus;
import com.travelagency.backend.model.TravelPackageEntity;
import com.travelagency.backend.repository.TravelPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelPackageService {

    private final TravelPackageRepository packageRepository;

    @Transactional
    public PackageResponse createPackage(CreatePackageRequest request) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("La fecha de término debe ser posterior a la fecha de inicio.");
        }

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setName(request.getName());
        pkg.setDestination(request.getDestination());
        pkg.setDescription(request.getDescription());
        pkg.setStartDate(request.getStartDate());
        pkg.setEndDate(request.getEndDate());
        pkg.setPrice(request.getPrice());
        pkg.setPackageType(request.getPackageType());
        pkg.setIncludedServices(request.getIncludedServices());
        pkg.setRestrictions(request.getRestrictions());
        pkg.setTotalSlots(request.getTotalSlots());
        pkg.setAvailableSlots(request.getTotalSlots());
        pkg.setStatus(PackageStatus.AVAILABLE);

        TravelPackageEntity saved = packageRepository.save(pkg);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getAllPackages() {
        return packageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PackageResponse getPackageById(Long id) {
        TravelPackageEntity pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El paquete con id:" + id + "no fue encontrado."));
        return toResponse(pkg);
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getAvailablePackages() {
        return packageRepository.findByStatus(PackageStatus.AVAILABLE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PackageResponse updatePackage(Long id, UpdatePackageRequest request) {
        TravelPackageEntity pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El paquete con id:" + id + "no fue encontrado."));

        if (pkg.getStatus() == PackageStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede actualizar un paquete cancelado.");
        }

        if (request.getName() != null)        pkg.setName(request.getName());
        if (request.getDestination() != null) pkg.setDestination(request.getDestination());
        if (request.getDescription() != null) pkg.setDescription(request.getDescription());
        if (request.getPrice() != null)       pkg.setPrice(request.getPrice());
        if (request.getPackageType() != null) pkg.setPackageType(request.getPackageType());
        if (request.getIncludedServices() != null) pkg.setIncludedServices(request.getIncludedServices());
        if (request.getRestrictions()!= null) pkg.setRestrictions(request.getRestrictions());
        int reservedSlots = pkg.getTotalSlots() - pkg.getAvailableSlots();
        if (request.getTotalSlots() != null) {
            if (request.getTotalSlots() < reservedSlots) {
                throw new IllegalArgumentException(
                        "No se pueden reducir los cupos totales por debajo de los ya reservados (" + reservedSlots + ").");
            }
            pkg.setTotalSlots(request.getTotalSlots());
            pkg.setAvailableSlots(request.getTotalSlots() - reservedSlots);
        }
        // Regla Épica 2: si el paquete tiene reservas activas, no se permite modificar
        // fechas. Comparamos contra el valor actual (no solo si vienen != null) porque el
        // frontend siempre manda las fechas presentes — bloqueamos solo si REALMENTE cambian.
        boolean startDateChanged = request.getStartDate() != null
                && !request.getStartDate().equals(pkg.getStartDate());
        boolean endDateChanged   = request.getEndDate() != null
                && !request.getEndDate().equals(pkg.getEndDate());
        if (reservedSlots > 0 && (startDateChanged || endDateChanged)) {
            throw new IllegalArgumentException(
                    "No se pueden modificar las fechas de un paquete con reservas registradas. " +
                    "Hay " + reservedSlots + " cupo(s) reservado(s).");
        }
        LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : pkg.getStartDate();
        LocalDate newEnd   = request.getEndDate()   != null ? request.getEndDate()   : pkg.getEndDate();
        if (!newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("La fecha de término debe ser posterior a la fecha de inicio.");
        }

        if (request.getStartDate() != null) pkg.setStartDate(request.getStartDate());
        if (request.getEndDate()   != null) pkg.setEndDate(request.getEndDate());

        if (request.getStatus() == PackageStatus.AVAILABLE && pkg.getAvailableSlots() == 0) {
            throw new IllegalArgumentException(
                    "No se puede publicar el paquete como disponible: no tiene cupos disponibles.");
        }
        // Actualizar status explícito si el admin lo mandó
        if (request.getStatus() != null) {
            pkg.setStatus(request.getStatus());
        }

        // Sincronizar status automático basado en availableSlots (si no se mandó status explícito)
        if (request.getStatus() == null) {
            if (pkg.getAvailableSlots() == 0) {
                pkg.setStatus(PackageStatus.SOLD_OUT);
            } else if (pkg.getStatus() == PackageStatus.SOLD_OUT) {
                pkg.setStatus(PackageStatus.AVAILABLE);
            }
        }

        return toResponse(packageRepository.save(pkg));
    }

    /**
     * Re-publica un paquete que fue previamente cancelado.
     * Solo permite la transición CANCELLED → AVAILABLE. Otras transiciones
     * (ej. SOLD_OUT → AVAILABLE) las gestiona updatePackage o el flujo de cancelación
     * de reservas, que devuelve cupos automáticamente.
     *
     * Reglas:
     *  - El paquete debe estar actualmente en estado CANCELLED.
     *  - La fecha de inicio no puede estar en el pasado (no se publica algo vencido).
     *  - Debe tener al menos un cupo disponible (regla del enunciado Épica 2:
     *    "Un paquete no puede publicarse como disponible si no tiene cupos").
     */
    @Transactional
    public PackageResponse publishPackage(Long id) {
        TravelPackageEntity pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El paquete con id:" + id + " no fue encontrado."));

        if (pkg.getStatus() != PackageStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Solo se pueden re-publicar paquetes cancelados. Estado actual: "
                            + pkg.getStatus());
        }
        if (pkg.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "No se puede publicar un paquete cuya fecha de inicio ya pasó.");
        }
        if (pkg.getAvailableSlots() == null || pkg.getAvailableSlots() <= 0) {
            throw new IllegalArgumentException(
                    "No se puede publicar un paquete sin cupos disponibles.");
        }

        pkg.setStatus(PackageStatus.AVAILABLE);
        return toResponse(packageRepository.save(pkg));
    }

    @Transactional
    public void deletePackage(Long id) {
        TravelPackageEntity pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El paquete con id:" + id + "no fue encontrado."));

        if (pkg.getStatus() == PackageStatus.CANCELLED) {
            throw new IllegalArgumentException("El paquete ya está cancelado.");
        }

        // Regla Épica 2: un paquete con reservas activas (PENDING_PAYMENT o CONFIRMED)
        // no debe poder cancelarse porque romperíamos la consistencia de esas reservas.
        // reservedSlots = totalSlots - availableSlots cuenta exactamente las reservas vivas:
        // cuando una reserva se cancela o expira, sus cupos se devuelven al paquete.
        int reservedSlots = pkg.getTotalSlots() - pkg.getAvailableSlots();
        if (reservedSlots > 0) {
            throw new IllegalArgumentException(
                    "No se puede cancelar un paquete con reservas activas. " +
                    "Hay " + reservedSlots + " cupo(s) reservado(s). " +
                    "Primero cancela las reservas asociadas.");
        }

        pkg.setStatus(PackageStatus.CANCELLED);
        packageRepository.save(pkg);
    }
    @Transactional(readOnly = true)
    public List<PackageResponse> searchPackages(
            String destination,
            Long minPrice,
            Long maxPrice,
            LocalDate startDate,
            LocalDate endDate,
            String packageType) {

        return packageRepository.searchPackages(
                        LocalDate.now(),   // "hoy" como parámetro → testeable
                        destination, minPrice, maxPrice,
                        startDate, endDate, packageType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PackageResponse toResponse(TravelPackageEntity pkg) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .destination(pkg.getDestination())
                .description(pkg.getDescription())
                .startDate(pkg.getStartDate())
                .endDate(pkg.getEndDate())
                .price(pkg.getPrice())
                .packageType(pkg.getPackageType())
                .includedServices(pkg.getIncludedServices())
                .restrictions(pkg.getRestrictions())
                .totalSlots(pkg.getTotalSlots())
                .availableSlots(pkg.getAvailableSlots())
                .status(pkg.getStatus())
                .createdAt(pkg.getCreatedAt())
                .build();
    }
}