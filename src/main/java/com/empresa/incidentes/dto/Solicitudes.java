package com.empresa.incidentes.dto;

import com.empresa.incidentes.model.Categoria;
import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.Prioridad;
import com.empresa.incidentes.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos (JSON) que recibe la API. Como el proyecto no implementa autenticacion,
 * cada operacion indica en "usuarioId" quien la realiza, para dejarlo en el historial.
 */
public final class Solicitudes {

    private Solicitudes() {
    }

    public record UsuarioRequest(
            @NotBlank(message = "es obligatorio") String nombre,
            @NotBlank(message = "es obligatorio") @Email(message = "no es un email válido") String email,
            @NotNull(message = "es obligatorio") Rol rol,
            String departamento,
            Categoria especialidad) {
    }

    public record IncidenteRequest(
            @NotBlank(message = "es obligatorio") @Size(max = 120) String titulo,
            @NotBlank(message = "es obligatorio") String descripcion,
            @NotNull(message = "es obligatoria") Categoria categoria,
            @NotNull(message = "es obligatoria") Prioridad prioridad,
            String activoAfectado,
            @NotBlank(message = "es obligatorio") String reportadoPorId) {
    }

    public record ActualizarIncidenteRequest(
            @Size(max = 120) String titulo,
            String descripcion,
            Categoria categoria,
            String activoAfectado,
            @NotBlank(message = "es obligatorio") String usuarioId) {
    }

    public record AsignacionRequest(
            @NotBlank(message = "es obligatorio") String tecnicoId,
            @NotBlank(message = "es obligatorio") String usuarioId) {
    }

    public record CambioEstadoRequest(
            @NotNull(message = "es obligatorio") EstadoIncidente estado,
            @NotBlank(message = "es obligatorio") String usuarioId,
            String motivo) {
    }

    public record CambioPrioridadRequest(
            @NotNull(message = "es obligatoria") Prioridad prioridad,
            @NotBlank(message = "es obligatorio") String usuarioId) {
    }

    public record ComentarioRequest(
            @NotBlank(message = "es obligatorio") String autorId,
            @NotBlank(message = "es obligatorio") String texto) {
    }

    public record ResolucionRequest(
            @NotBlank(message = "es obligatoria") String solucion,
            @NotBlank(message = "es obligatorio") String usuarioId) {
    }
}
