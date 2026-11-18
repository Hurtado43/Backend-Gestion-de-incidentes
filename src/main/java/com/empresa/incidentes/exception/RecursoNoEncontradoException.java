package com.empresa.incidentes.exception;

/** Se lanza cuando no existe el recurso solicitado (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super(recurso + " no encontrado: " + id);
    }
}
