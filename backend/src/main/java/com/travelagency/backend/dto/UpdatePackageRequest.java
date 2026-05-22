package com.travelagency.backend.dto;

import com.travelagency.backend.model.PackageStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdatePackageRequest {
    private String name;
    private String destination;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

    @Positive(message = "El precio debe ser mayor a 0.")
    private Long price;

    private String packageType;
    private String includedServices;
    private String restrictions;
    @Positive(message = "El total de cupos debe ser mayor a 0.")
    private Integer totalSlots;


    private PackageStatus status;
}
