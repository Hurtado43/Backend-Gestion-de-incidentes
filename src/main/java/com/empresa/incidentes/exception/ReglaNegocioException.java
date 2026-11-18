package com.empresa.incidentes.exception;

/** Se lanza cuando una operacion viola una regla del negocio (HTTP 409 / 400). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
