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

    // Los parametros numericos y de fecha NUNCA llegan null aqui:
    // el service los sustituye por sentinelas (0/MAX, 1900/9999) antes de
    // llamar a este metodo. Asi evitamos el bug de Postgres + Hibernate 6
    // que no infiere el tipo de un parametro ? cuando llega null.
    //
    // Strings (destination, packageType) si pueden llegar null, por eso usan
    // CAST(:param AS string) IS NULL que Hibernate maneja correctamente.
    @Query("""
    SELECT p FROM TravelPackageEntity p
    WHERE p.status = com.travelagency.backend.model.PackageStatus.AVAILABLE
    AND p.startDate >= :today
    AND (CAST(:destination AS string) IS NULL
         OR LOWER(p.destination) LIKE LOWER(CONCAT('%', CAST(:destination AS string), '%')))
    AND p.price >= :minPrice
    AND p.price <= :maxPrice
    AND p.startDate >= :startDate
    AND p.endDate   <= :endDate
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
