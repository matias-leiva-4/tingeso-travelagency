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

    @Query("""
    SELECT p FROM TravelPackageEntity p
    WHERE p.status = com.travelagency.backend.model.PackageStatus.AVAILABLE
    AND p.startDate >= :today
    AND (CAST(:destination AS string) IS NULL
         OR LOWER(p.destination) LIKE LOWER(CONCAT('%', CAST(:destination AS string), '%')))
    AND (:minPrice IS NULL OR p.price >= :minPrice)
    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
    AND (:startDate IS NULL OR p.startDate >= :startDate)
    AND (:endDate   IS NULL OR p.endDate   <= :endDate)
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
