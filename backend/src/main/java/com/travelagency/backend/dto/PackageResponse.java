package com.travelagency.backend.dto;

import com.travelagency.backend.model.PackageStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {
    private Long id;
    private String name;
    private String destination;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long price;
    private String packageType;
    private String includedServices;
    private String restrictions;
    private Integer totalSlots;
    private Integer availableSlots;
    private PackageStatus status;
    private LocalDateTime createdAt;
}