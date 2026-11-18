package com.empresa.incidentes.model;

/** Rol que cumple un usuario dentro de la plataforma. */
public enum Rol {
    /** Empleado que reporta problemas tecnologicos. */
    REPORTANTE,
    /** Tecnico de soporte que atiende incidentes. */
    TECNICO,
    /** Supervisor / administrador de la mesa de ayuda. */
    ADMINISTRADOR
}
