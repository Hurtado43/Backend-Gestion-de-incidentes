package com.empresa.incidentes.dto;

import com.empresa.incidentes.model.Categoria;
import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.Prioridad;

/** Criterios opcionales para listar incidentes (todos pueden ser null). */
public record FiltroIncidentes(EstadoIncidente estado, Prioridad prioridad, Categoria categoria,
                               String tecnicoId, String reportadoPorId, String texto) {
}
