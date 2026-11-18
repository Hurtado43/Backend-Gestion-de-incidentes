package com.empresa.incidentes.dto;

import com.empresa.incidentes.model.Prioridad;

import java.util.Map;

/** Respuestas de los reportes de seguimiento y mejora de tiempos. */
public final class Reportes {

    private Reportes() {
    }

    /** Tiempos promedio y cumplimiento de SLA para una prioridad. */
    public record TiemposPorPrioridad(Prioridad prioridad, long total, long resueltos,
                                      Double promedioMinutosRespuesta, Double promedioMinutosResolucion,
                                      int slaHorasResolucion, long slaIncumplidos,
                                      double porcentajeCumplimientoSla) {
    }

    /** Carga de trabajo y desempeno de un tecnico. */
    public record DesempenoTecnico(String tecnicoId, String nombre, long asignados, long activos,
                                   long resueltos, Double promedioMinutosResolucion, long slaIncumplidos) {
    }

    /** Vista general para el tablero de la mesa de ayuda. */
    public record Resumen(long total, long activos, long slaIncumplidos,
                          Map<String, Long> porEstado, Map<String, Long> porPrioridad,
                          Map<String, Long> porCategoria) {
    }
}
