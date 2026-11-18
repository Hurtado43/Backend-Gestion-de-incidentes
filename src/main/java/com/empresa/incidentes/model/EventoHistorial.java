package com.empresa.incidentes.model;

import java.time.Instant;

/** Entrada del historial (auditoria) de un incidente. Se guarda embebida en el documento del incidente. */
public class EventoHistorial {

    private Instant fecha;
    private TipoEvento tipo;
    private String usuarioId;
    private String descripcion;
    private String valorAnterior;
    private String valorNuevo;

    public EventoHistorial() {
    }

    public EventoHistorial(TipoEvento tipo, String usuarioId, String descripcion,
                           String valorAnterior, String valorNuevo) {
        this.fecha = Instant.now();
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.descripcion = descripcion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
    public TipoEvento getTipo() { return tipo; }
    public void setTipo(TipoEvento tipo) { this.tipo = tipo; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }
    public String getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(String valorNuevo) { this.valorNuevo = valorNuevo; }
}
