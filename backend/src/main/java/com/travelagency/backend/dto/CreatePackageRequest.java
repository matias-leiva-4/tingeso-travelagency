package com.travelagency.backend.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreatePackageRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El destino es obligatorio")
    private String destination;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio debe ser hoy o en el futuro")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    private Long price;

    private String packageType;


    private String includedServices;


    private String restrictions;

    @NotNull(message = "El total de cupos es obligatorio")
    @Positive(message = "El total de cupos debe ser mayor a 0")
    private Integer totalSlots;
}