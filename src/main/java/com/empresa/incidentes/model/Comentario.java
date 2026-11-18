package com.empresa.incidentes.model;

import java.time.Instant;

/** Comentario de seguimiento sobre un incidente (embebido en el incidente). */
public class Comentario {

    private String autorId;
    private String texto;
    private Instant fecha;

    public Comentario() {
    }

    public Comentario(String autorId, String texto) {
        this.autorId = autorId;
        this.texto = texto;
        this.fecha = Instant.now();
    }

    public String getAutorId() { return autorId; }
    public void setAutorId(String autorId) { this.autorId = autorId; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
