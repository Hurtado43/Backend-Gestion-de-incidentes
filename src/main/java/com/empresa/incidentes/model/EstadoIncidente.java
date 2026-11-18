package com.empresa.incidentes.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de un incidente. Cada estado conoce a que estados puede pasar,
 * lo que permite validar el seguimiento hasta su solucion.
 */
public enum EstadoIncidente {
    ABIERTO,
    ASIGNADO,
    EN_PROGRESO,
    EN_ESPERA,
    RESUELTO,
    CERRADO,
    CANCELADO;

    public Set<EstadoIncidente> siguientesPermitidos() {
        return switch (this) {
            case ABIERTO -> EnumSet.of(ASIGNADO, CANCELADO);
            case ASIGNADO -> EnumSet.of(EN_PROGRESO, EN_ESPERA, CANCELADO);
            case EN_PROGRESO -> EnumSet.of(EN_ESPERA, RESUELTO, CANCELADO);
            case EN_ESPERA -> EnumSet.of(EN_PROGRESO, CANCELADO);
            // Desde RESUELTO se puede cerrar o reabrir si la solucion no funciono
            case RESUELTO -> EnumSet.of(CERRADO, EN_PROGRESO);
            case CERRADO, CANCELADO -> EnumSet.noneOf(EstadoIncidente.class);
        };
    }

    public boolean puedeCambiarA(EstadoIncidente destino) {
        return siguientesPermitidos().contains(destino);
    }

    public boolean esFinal() {
        return this == CERRADO || this == CANCELADO;
    }
}
