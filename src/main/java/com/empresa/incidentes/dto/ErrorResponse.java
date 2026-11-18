package com.empresa.incidentes.dto;

import java.time.Instant;
import java.util.List;

/** Formato uniforme de los errores que devuelve la API. */
public record ErrorResponse(int status, String error, String mensaje, String ruta,
                            List<String> detalles, Instant fecha) {

    public ErrorResponse(int status, String error, String mensaje, String ruta, List<String> detalles) {
        this(status, error, mensaje, ruta, detalles, Instant.now());
    }
}
