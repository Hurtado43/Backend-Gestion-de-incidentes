package com.empresa.incidentes.repository;

import com.empresa.incidentes.dto.FiltroIncidentes;
import com.empresa.incidentes.dto.ProblemaRecurrente;
import com.empresa.incidentes.model.Incidente;

import java.time.Instant;
import java.util.List;

/** Consultas que necesitan MongoTemplate (filtros dinamicos y agregaciones). */
public interface IncidenteRepositoryCustom {

    List<Incidente> buscar(FiltroIncidentes filtro);

    /** Agrupa incidentes por categoria + activo afectado y devuelve los grupos que se repiten. */
    List<ProblemaRecurrente> problemasRecurrentes(Instant desde, int minimoOcurrencias);
}
