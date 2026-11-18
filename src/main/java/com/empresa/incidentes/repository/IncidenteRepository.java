package com.empresa.incidentes.repository;

import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.Incidente;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IncidenteRepository extends MongoRepository<Incidente, String>, IncidenteRepositoryCustom {

    Optional<Incidente> findByCodigo(String codigo);

    List<Incidente> findByTecnicoAsignadoId(String tecnicoId);

    long countByTecnicoAsignadoIdAndEstadoIn(String tecnicoId, Collection<EstadoIncidente> estados);

    List<Incidente> findByFechaCreacionAfter(Instant desde);

    List<Incidente> findByActivoAfectadoAndSolucionNotNullOrderByFechaCreacionDesc(String activoAfectado);
}
