package com.empresa.incidentes.service;

import com.empresa.incidentes.dto.FiltroIncidentes;
import com.empresa.incidentes.dto.Solicitudes.ActualizarIncidenteRequest;
import com.empresa.incidentes.dto.Solicitudes.AsignacionRequest;
import com.empresa.incidentes.dto.Solicitudes.CambioEstadoRequest;
import com.empresa.incidentes.dto.Solicitudes.CambioPrioridadRequest;
import com.empresa.incidentes.dto.Solicitudes.ComentarioRequest;
import com.empresa.incidentes.dto.Solicitudes.IncidenteRequest;
import com.empresa.incidentes.dto.Solicitudes.ResolucionRequest;
import com.empresa.incidentes.exception.RecursoNoEncontradoException;
import com.empresa.incidentes.exception.ReglaNegocioException;
import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.EventoHistorial;
import com.empresa.incidentes.model.Incidente;
import com.empresa.incidentes.model.Usuario;
import com.empresa.incidentes.repository.IncidenteRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Casos de uso de incidentes: reportar, asignar a tecnicos, priorizar y hacer seguimiento
 * hasta la solucion. Las reglas del ciclo de vida estan en la entidad {@link Incidente}.
 */
@Service
public class IncidenteService {

    /** Estados que cuentan como carga de trabajo pendiente de un tecnico. */
    static final Set<EstadoIncidente> ESTADOS_ACTIVOS =
            EnumSet.of(EstadoIncidente.ASIGNADO, EstadoIncidente.EN_PROGRESO, EstadoIncidente.EN_ESPERA);

    private final IncidenteRepository incidenteRepository;
    private final UsuarioService usuarioService;
    private final GeneradorCodigoService generadorCodigo;

    public IncidenteService(IncidenteRepository incidenteRepository, UsuarioService usuarioService,
                            GeneradorCodigoService generadorCodigo) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioService = usuarioService;
        this.generadorCodigo = generadorCodigo;
    }

    public Incidente reportar(IncidenteRequest req) {
        Usuario reportante = usuarioActivo(req.reportadoPorId());
        Incidente incidente = new Incidente();
        incidente.setTitulo(req.titulo());
        incidente.setDescripcion(req.descripcion());
        incidente.setCategoria(req.categoria());
        incidente.setPrioridad(req.prioridad());
        incidente.setActivoAfectado(normalizarActivo(req.activoAfectado()));
        incidente.registrarCreacion(generadorCodigo.siguienteCodigoIncidente(), reportante.getId());
        return incidenteRepository.save(incidente);
    }

    public List<Incidente> buscar(FiltroIncidentes filtro) {
        return incidenteRepository.buscar(filtro);
    }

    /** Busca por id de MongoDB o por codigo legible (INC-000001). */
    public Incidente obtener(String idOCodigo) {
        return incidenteRepository.findById(idOCodigo)
                .or(() -> incidenteRepository.findByCodigo(idOCodigo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Incidente", idOCodigo));
    }

    public Incidente actualizar(String id, ActualizarIncidenteRequest req) {
        Incidente incidente = obtener(id);
        usuarioActivo(req.usuarioId());
        incidente.actualizarDatos(req.titulo(), req.descripcion(), req.categoria(),
                normalizarActivo(req.activoAfectado()), req.usuarioId());
        return incidenteRepository.save(incidente);
    }

    public Incidente asignar(String id, AsignacionRequest req) {
        Incidente incidente = obtener(id);
        usuarioActivo(req.usuarioId());
        Usuario tecnico = usuarioService.obtener(req.tecnicoId());
        incidente.asignar(tecnico, req.usuarioId());
        return incidenteRepository.save(incidente);
    }

    /**
     * Asignacion automatica: elige al tecnico activo con menos incidentes pendientes,
     * dando preferencia a los especialistas en la categoria del incidente.
     */
    public Incidente asignarAutomaticamente(String id, String usuarioId) {
        Incidente incidente = obtener(id);
        usuarioActivo(usuarioId);
        List<Usuario> tecnicos = usuarioService.listarTecnicos();
        if (tecnicos.isEmpty()) {
            throw new ReglaNegocioException("No hay técnicos activos disponibles");
        }
        Usuario elegido = tecnicos.stream()
                .min(Comparator
                        .comparing((Usuario t) -> t.getEspecialidad() == incidente.getCategoria() ? 0 : 1)
                        .thenComparingLong(t -> incidenteRepository
                                .countByTecnicoAsignadoIdAndEstadoIn(t.getId(), ESTADOS_ACTIVOS)))
                .orElseThrow();
        incidente.asignar(elegido, usuarioId);
        return incidenteRepository.save(incidente);
    }

    public Incidente cambiarEstado(String id, CambioEstadoRequest req) {
        Incidente incidente = obtener(id);
        usuarioActivo(req.usuarioId());
        incidente.cambiarEstado(req.estado(), req.usuarioId(), req.motivo());
        return incidenteRepository.save(incidente);
    }

    public Incidente cambiarPrioridad(String id, CambioPrioridadRequest req) {
        Incidente incidente = obtener(id);
        usuarioActivo(req.usuarioId());
        incidente.cambiarPrioridad(req.prioridad(), req.usuarioId());
        return incidenteRepository.save(incidente);
    }

    public Incidente comentar(String id, ComentarioRequest req) {
        Incidente incidente = obtener(id);
        usuarioActivo(req.autorId());
        incidente.agregarComentario(req.autorId(), req.texto());
        return incidenteRepository.save(incidente);
    }

    public Incidente resolver(String id, ResolucionRequest req) {
        Incidente incidente = obtener(id);
        usuarioActivo(req.usuarioId());
        incidente.resolver(req.solucion(), req.usuarioId());
        return incidenteRepository.save(incidente);
    }

    public List<EventoHistorial> historial(String id) {
        return obtener(id).getHistorial();
    }

    /** Lista de incidentes previos ya resueltos sobre el mismo activo: ayuda a reutilizar soluciones. */
    public List<Incidente> antecedentes(String id) {
        Incidente incidente = obtener(id);
        if (incidente.getActivoAfectado() == null) {
            return List.of();
        }
        return incidenteRepository
                .findByActivoAfectadoAndSolucionNotNullOrderByFechaCreacionDesc(incidente.getActivoAfectado())
                .stream()
                .filter(otro -> !otro.getId().equals(incidente.getId()))
                .toList();
    }

    public void eliminar(String id) {
        Incidente incidente = obtener(id);
        if (incidente.getEstado() != EstadoIncidente.ABIERTO && incidente.getEstado() != EstadoIncidente.CANCELADO) {
            throw new ReglaNegocioException("Solo se pueden eliminar incidentes ABIERTOS o CANCELADOS; "
                    + "para el resto use el cambio de estado a CANCELADO");
        }
        incidenteRepository.delete(incidente);
    }

    private Usuario usuarioActivo(String usuarioId) {
        Usuario usuario = usuarioService.obtener(usuarioId);
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario " + usuario.getNombre() + " está inactivo");
        }
        return usuario;
    }

    /** Normaliza el activo afectado ("  pc-0045 " -> "PC-0045") para que las recurrencias se agrupen bien. */
    private String normalizarActivo(String activo) {
        return (activo == null || activo.isBlank()) ? null : activo.trim().toUpperCase();
    }
}
