package com.empresa.incidentes.dto;

import com.empresa.incidentes.model.Categoria;

import java.time.Instant;
import java.util.List;

/** Grupo de incidentes de la misma categoria sobre el mismo activo: candidato a problema de fondo. */
public record ProblemaRecurrente(Categoria categoria, String activoAfectado, long cantidad,
                                 Instant ultimaOcurrencia, List<String> codigos) {
}
