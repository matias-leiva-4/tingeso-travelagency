package com.travelagency.backend.repository;

import com.travelagency.backend.model.PackageStatus;
import com.travelagency.backend.model.TravelPackageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para TravelPackageRepository.
 * Verifica los métodos derivados y la query JPQL personalizada searchPackages().
 */
@SpringBootTest
@Transactional
class TravelPackageRepositoryTest {

    @Autowired TravelPackageRepository packageRepository;

    @BeforeEach
    void setUp() {
        packageRepository.save(buildPackage(
                "Tour Atacama Test", "Atacama", 120_000L,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(15),
                PackageStatus.AVAILABLE, "AVENTURA"));

        packageRepository.save(buildPackage(
                "Tour Patagonia Test", "Patagonia", 300_000L,
                LocalDate.now().plusDays(20), LocalDate.now().plusDays(30),
                PackageStatus.SOLD_OUT, "NATURALEZA"));

        packageRepository.save(buildPackage(
                "Tour Cancelado Test", "Santiago", 50_000L,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7),
                PackageStatus.CANCELLED, "CIUDAD"));

        packageRepository.flush();
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Test
    void findByStatus_available_returnsOnlyAvailable() {
        List<TravelPackageEntity> result = packageRepository.findByStatus(PackageStatus.AVAILABLE);

        assertThat(result)
                .isNotEmpty()
                .allMatch(p -> p.getStatus() == PackageStatus.AVAILABLE);
    }

    @Test
    void findByStatus_soldOut_returnsOnlySoldOut() {
        List<TravelPackageEntity> result = packageRepository.findByStatus(PackageStatus.SOLD_OUT);

        assertThat(result)
                .isNotEmpty()
                .allMatch(p -> p.getStatus() == PackageStatus.SOLD_OUT);
    }

    // ── searchPackages (query JPQL personalizada) ─────────────────────────────

    @Test
    void searchPackages_byDestination_returnsMatchingPackages() {
        List<TravelPackageEntity> result = packageRepository.searchPackages(
                LocalDate.now(), "atacama", null, null, null, null, null);

        assertThat(result)
                .isNotEmpty()
                .allMatch(p -> p.getDestination().toLowerCase().contains("atacama"));
    }

    @Test
    void searchPackages_byMinPrice_excludesCheaperPackages() {
        List<TravelPackageEntity> result = packageRepository.searchPackages(
                LocalDate.now(), null, 200_000L, null, null, null, null);

        // El paquete de 120.000 no debe aparecer
        assertThat(result).extracting(TravelPackageEntity::getPrice)
                .allMatch(price -> price >= 200_000L);
    }

    @Test
    void searchPackages_byMaxPrice_excludesExpensivePackages() {
        List<TravelPackageEntity> result = packageRepository.searchPackages(
                LocalDate.now(), null, null, 150_000L, null, null, null);

        assertThat(result)
                .isNotEmpty()
                .allMatch(p -> p.getPrice() <= 150_000L);
    }

    @Test
    void searchPackages_byPackageType_returnsOnlyMatchingType() {
        List<TravelPackageEntity> result = packageRepository.searchPackages(
                LocalDate.now(), null, null, null, null, null, "AVENTURA");

        assertThat(result)
                .isNotEmpty()
                .allMatch(p -> "AVENTURA".equalsIgnoreCase(p.getPackageType()));
    }

    @Test
    void searchPackages_neverReturnsCancelledOrSoldOut() {
        List<TravelPackageEntity> result = packageRepository.searchPackages(
                LocalDate.now(), null, null, null, null, null, null);

        assertThat(result).extracting(TravelPackageEntity::getStatus)
                .doesNotContain(PackageStatus.CANCELLED, PackageStatus.SOLD_OUT);
    }

    @Test
    void searchPackages_onlyReturnsFuturePackages() {
        List<TravelPackageEntity> result = packageRepository.searchPackages(
                LocalDate.now(), null, null, null, null, null, null);

        assertThat(result)
                .allMatch(p -> !p.getStartDate().isBefore(LocalDate.now()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TravelPackageEntity buildPackage(String name, String destination,
                                              Long price, LocalDate start, LocalDate end,
                                              PackageStatus status, String type) {
        TravelPackageEntity p = new TravelPackageEntity();
        p.setName(name);
        p.setDestination(destination);
        p.setDescription("Descripción de " + name);
        p.setPrice(price);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setTotalSlots(20);
        p.setAvailableSlots(20);
        p.setStatus(status);
        p.setPackageType(type);
        return p;
    }
}
