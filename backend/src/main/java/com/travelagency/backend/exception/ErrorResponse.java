package com.travelagency.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

// DTO que estructura la respuesta de error en JSON
@Data
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;   // Cuándo ocurrió
    private int status;                // HTTP status code numérico (ej: 404)
    private String error;              // Nombre del error (ej: "Not Found")
    private String message;            // Mensaje descriptivo
    private String path;               // Endpoint que falló (ej: "/api/packages/99")
}