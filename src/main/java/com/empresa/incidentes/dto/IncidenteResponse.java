package com.empresa.incidentes.dto;

import com.empresa.incidentes.model.Categoria;
import com.empresa.incidentes.model.Comentario;
import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.EventoHistorial;
import com.empresa.incidentes.model.Incidente;
import com.empresa.incidentes.model.Prioridad;

import java.time.Instant;
import java.util.List;

/** Representacion del incidente que devuelve la API, con metricas calculadas. */
public record IncidenteResponse(
        String id,
        String codigo,
        String titulo,
        String descripcion,
        Categoria categoria,
        Prioridad prioridad,
        EstadoIncidente estado,
        String activoAfectado,
        String reportadoPorId,
        String tecnicoAsignadoId,
        Instant fechaCreacion,
        Instant fechaAsignacion,
        Instant fechaPrimeraRespuesta,
        Instant fechaResolucion,
        Instant fechaCierre,
        Instant fechaLimiteSla,
        String solucion,
        Long minutosPrimeraRespuesta,
        Long minutosResolucion,
        boolean slaIncumplido,
        List<EstadoIncidente> siguientesEstados,
        List<Comentario> comentarios,
        List<EventoHistorial> historial) {

    public static IncidenteResponse de(Incidente i) {
        return new IncidenteResponse(
                i.getId(), i.getCodigo(), i.getTitulo(), i.getDescripcion(), i.getCategoria(),
                i.getPrioridad(), i.getEstado(), i.getActivoAfectado(), i.getReportadoPorId(),
                i.getTecnicoAsignadoId(), i.getFechaCreacion(), i.getFechaAsignacion(),
                i.getFechaPrimeraRespuesta(), i.getFechaResolucion(), i.getFechaCierre(),
                i.getFechaLimiteSla(), i.getSolucion(), i.minutosPrimeraRespuesta(),
                i.minutosResolucion(), i.slaIncumplido(),
                i.getEstado().siguientesPermitidos().stream().toList(),
                i.getComentarios(), i.getHistorial());
    }
}
