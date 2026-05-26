package com.travelagency.backend.repository;

import com.travelagency.backend.model.PackageStatus;
import com.travelagency.backend.model.TravelPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelPackageRepository extends JpaRepository<TravelPackageEntity, Long> {

    List<TravelPackageEntity> findByStatus(PackageStatus status);

    List<TravelPackageEntity> findByDestinationContainingIgnoreCase(String destination);

    // Nota: los CAST explicitos son obligatorios cuando se pasan parametros
    // potencialmente null. Sin ellos, PostgreSQL no puede inferir el tipo del
    // parametro ($n) y devuelve "could not determine data type of parameter".
    @Query("""
    SELECT p FROM TravelPackageEntity p
    WHERE p.status = com.travelagency.backend.model.PackageStatus.AVAILABLE
    AND p.startDate >= :today
    AND (CAST(:destination AS string) IS NULL
         OR LOWER(p.destination) LIKE LOWER(CONCAT('%', CAST(:destination AS string), '%')))
    AND (CAST(:minPrice AS long) IS NULL OR p.price >= CAST(:minPrice AS long))
    AND (CAST(:maxPrice AS long) IS NULL OR p.price <= CAST(:maxPrice AS long))
    AND (CAST(:startDate AS date) IS NULL OR p.startDate >= CAST(:startDate AS date))
    AND (CAST(:endDate   AS date) IS NULL OR p.endDate   <= CAST(:endDate   AS date))
    AND (CAST(:packageType AS string) IS NULL
         OR LOWER(p.packageType) = LOWER(CAST(:packageType AS string)))
    ORDER BY p.startDate ASC
    """)
    List<TravelPackageEntity> searchPackages(
            @Param("today") LocalDate today,
            @Param("destination") String destination,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("startDate")   LocalDate startDate,
            @Param("endDate")     LocalDate endDate,
            @Param("packageType") String packageType
    );
}
