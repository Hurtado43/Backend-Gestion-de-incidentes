package com.empresa.incidentes.controller;

import com.empresa.incidentes.dto.FiltroIncidentes;
import com.empresa.incidentes.dto.IncidenteResponse;
import com.empresa.incidentes.dto.Solicitudes.ActualizarIncidenteRequest;
import com.empresa.incidentes.dto.Solicitudes.AsignacionRequest;
import com.empresa.incidentes.dto.Solicitudes.CambioEstadoRequest;
import com.empresa.incidentes.dto.Solicitudes.CambioPrioridadRequest;
import com.empresa.incidentes.dto.Solicitudes.ComentarioRequest;
import com.empresa.incidentes.dto.Solicitudes.IncidenteRequest;
import com.empresa.incidentes.dto.Solicitudes.ResolucionRequest;
import com.empresa.incidentes.model.Categoria;
import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.EventoHistorial;
import com.empresa.incidentes.model.Prioridad;
import com.empresa.incidentes.service.IncidenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidentes")
@Tag(name = "Incidentes", description = "Reporte, asignacion, priorizacion y seguimiento de incidentes")
public class IncidenteController {

    private final IncidenteService incidenteService;

    public IncidenteController(IncidenteService incidenteService) {
        this.incidenteService = incidenteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reportar un incidente")
    public IncidenteResponse reportar(@Valid @RequestBody IncidenteRequest req) {
        return IncidenteResponse.de(incidenteService.reportar(req));
    }

    @GetMapping
    @Operation(summary = "Listar incidentes con filtros opcionales")
    public List<IncidenteResponse> listar(@RequestParam(required = false) EstadoIncidente estado,
                                          @RequestParam(required = false) Prioridad prioridad,
                                          @RequestParam(required = false) Categoria categoria,
                                          @RequestParam(required = false) String tecnicoId,
                                          @RequestParam(required = false) String reportadoPorId,
                                          @RequestParam(required = false) String texto) {
        FiltroIncidentes filtro = new FiltroIncidentes(estado, prioridad, categoria, tecnicoId, reportadoPorId, texto);
        return incidenteService.buscar(filtro).stream().map(IncidenteResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un incidente por id o por codigo (INC-000001)")
    public IncidenteResponse obtener(@PathVariable String id) {
        return IncidenteResponse.de(incidenteService.obtener(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar titulo, descripcion, categoria o activo afectado")
    public IncidenteResponse actualizar(@PathVariable String id, @Valid @RequestBody ActualizarIncidenteRequest req) {
        return IncidenteResponse.de(incidenteService.actualizar(id, req));
    }

    @PatchMapping("/{id}/asignar")
    @Operation(summary = "Asignar el incidente a un tecnico")
    public IncidenteResponse asignar(@PathVariable String id, @Valid @RequestBody AsignacionRequest req) {
        return IncidenteResponse.de(incidenteService.asignar(id, req));
    }

    @PatchMapping("/{id}/asignar-automatico")
    @Operation(summary = "Asignar al tecnico con menos carga (prefiere especialistas de la categoria)")
    public IncidenteResponse asignarAutomatico(@PathVariable String id, @RequestParam String usuarioId) {
        return IncidenteResponse.de(incidenteService.asignarAutomaticamente(id, usuarioId));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado (valida las transiciones permitidas)")
    public IncidenteResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoRequest req) {
        return IncidenteResponse.de(incidenteService.cambiarEstado(id, req));
    }

    @PatchMapping("/{id}/prioridad")
    @Operation(summary = "Cambiar la prioridad (recalcula el SLA)")
    public IncidenteResponse cambiarPrioridad(@PathVariable String id, @Valid @RequestBody CambioPrioridadRequest req) {
        return IncidenteResponse.de(incidenteService.cambiarPrioridad(id, req));
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un comentario de seguimiento")
    public IncidenteResponse comentar(@PathVariable String id, @Valid @RequestBody ComentarioRequest req) {
        return IncidenteResponse.de(incidenteService.comentar(id, req));
    }

    @PatchMapping("/{id}/resolver")
    @Operation(summary = "Registrar la solucion y marcar el incidente como RESUELTO")
    public IncidenteResponse resolver(@PathVariable String id, @Valid @RequestBody ResolucionRequest req) {
        return IncidenteResponse.de(incidenteService.resolver(id, req));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Historial completo de cambios del incidente")
    public List<EventoHistorial> historial(@PathVariable String id) {
        return incidenteService.historial(id);
    }

    @GetMapping("/{id}/antecedentes")
    @Operation(summary = "Incidentes resueltos anteriormente sobre el mismo activo (soluciones reutilizables)")
    public List<IncidenteResponse> antecedentes(@PathVariable String id) {
        return incidenteService.antecedentes(id).stream().map(IncidenteResponse::de).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un incidente ABIERTO o CANCELADO")
    public void eliminar(@PathVariable String id) {
        incidenteService.eliminar(id);
    }
}
