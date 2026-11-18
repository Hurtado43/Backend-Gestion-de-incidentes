package com.empresa.incidentes.repository;

import com.empresa.incidentes.dto.FiltroIncidentes;
import com.empresa.incidentes.dto.ProblemaRecurrente;
import com.empresa.incidentes.model.Incidente;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

public class IncidenteRepositoryCustomImpl implements IncidenteRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public IncidenteRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Incidente> buscar(FiltroIncidentes f) {
        Query query = new Query();
        if (f.estado() != null) query.addCriteria(Criteria.where("estado").is(f.estado()));
        if (f.prioridad() != null) query.addCriteria(Criteria.where("prioridad").is(f.prioridad()));
        if (f.categoria() != null) query.addCriteria(Criteria.where("categoria").is(f.categoria()));
        if (f.tecnicoId() != null) query.addCriteria(Criteria.where("tecnicoAsignadoId").is(f.tecnicoId()));
        if (f.reportadoPorId() != null) query.addCriteria(Criteria.where("reportadoPorId").is(f.reportadoPorId()));
        if (f.texto() != null && !f.texto().isBlank()) {
            Pattern patron = Pattern.compile(Pattern.quote(f.texto()), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("titulo").regex(patron),
                    Criteria.where("descripcion").regex(patron)));
        }
        query.with(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return mongoTemplate.find(query, Incidente.class);
    }

    @Override
    public List<ProblemaRecurrente> problemasRecurrentes(Instant desde, int minimoOcurrencias) {
        Aggregation agregacion = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("fechaCreacion").gte(desde)
                        .and("activoAfectado").nin(null, "")),
                Aggregation.group("categoria", "activoAfectado")
                        .count().as("cantidad")
                        .max("fechaCreacion").as("ultimaOcurrencia")
                        .addToSet("codigo").as("codigos"),
                Aggregation.match(Criteria.where("cantidad").gte(minimoOcurrencias)),
                Aggregation.project("cantidad", "ultimaOcurrencia", "codigos")
                        .and("_id.categoria").as("categoria")
                        .and("_id.activoAfectado").as("activoAfectado")
                        .andExclude("_id"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "cantidad")));
        return mongoTemplate.aggregate(agregacion, Incidente.class, ProblemaRecurrente.class).getMappedResults();
    }
}
