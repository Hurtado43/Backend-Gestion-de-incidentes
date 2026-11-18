package com.empresa.incidentes.model;

/**
 * Prioridad del incidente. Cada prioridad define su acuerdo de nivel de servicio (SLA):
 * horas maximas para la primera respuesta y para la resolucion.
 */
public enum Prioridad {
    BAJA(24, 72),
    MEDIA(8, 24),
    ALTA(2, 8),
    CRITICA(1, 4);

    private final int horasRespuesta;
    private final int horasResolucion;

    Prioridad(int horasRespuesta, int horasResolucion) {
        this.horasRespuesta = horasRespuesta;
        this.horasResolucion = horasResolucion;
    }

    public int getHorasRespuesta() {
        return horasRespuesta;
    }

    public int getHorasResolucion() {
        return horasResolucion;
    }
}
