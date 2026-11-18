package com.empresa.incidentes.model;

/** Tipo de evento registrado en el historial de un incidente. */
public enum TipoEvento {
    CREACION,
    ASIGNACION,
    CAMBIO_ESTADO,
    CAMBIO_PRIORIDAD,
    COMENTARIO,
    RESOLUCION,
    ACTUALIZACION
}
