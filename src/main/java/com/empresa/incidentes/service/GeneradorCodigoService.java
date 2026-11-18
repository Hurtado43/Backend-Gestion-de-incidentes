package com.empresa.incidentes.service;

import com.empresa.incidentes.model.Contador;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/** Genera codigos consecutivos legibles (INC-000001) usando un contador atomico en MongoDB. */
@Service
public class GeneradorCodigoService {

    private static final String SECUENCIA_INCIDENTES = "incidentes";

    private final MongoTemplate mongoTemplate;

    public GeneradorCodigoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String siguienteCodigoIncidente() {
        Contador contador = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(SECUENCIA_INCIDENTES)),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Contador.class);
        return String.format("INC-%06d", contador.getValor());
    }
}
