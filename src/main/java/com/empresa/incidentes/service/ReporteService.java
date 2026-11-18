package com.empresa.incidentes.service;

import com.empresa.incidentes.dto.ProblemaRecurrente;
import com.empresa.incidentes.dto.Reportes.DesempenoTecnico;
import com.empresa.incidentes.dto.Reportes.Resumen;
import com.empresa.incidentes.dto.Reportes.TiemposPorPrioridad;
import com.empresa.incidentes.model.Incidente;
import com.empresa.incidentes.model.Prioridad;
import com.empresa.incidentes.model.Usuario;
import com.empresa.incidentes.repository.IncidenteRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Explota el historial de incidentes para identificar problemas recurrentes
 * y medir los tiempos de respuesta y resolucion.
 */
@Service
public class ReporteService {

    private final IncidenteRepository incidenteRepository;
    private final UsuarioService usuarioService;

    public ReporteService(IncidenteRepository incidenteRepository, UsuarioService usuarioService) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioService = usuarioService;
    }

    public List<ProblemaRecurrente> problemasRecurrentes(int dias, int minimoOcurrencias) {
        return incidenteRepository.problemasRecurrentes(desde(dias), minimoOcurrencias);
    }

    public List<TiemposPorPrioridad> tiemposPorPrioridad(int dias) {
        Map<Prioridad, List<Incidente>> porPrioridad = incidenteRepository.findByFechaCreacionAfter(desde(dias))
                .stream().collect(Collectors.groupingBy(Incidente::getPrioridad));
        return Arrays.stream(Prioridad.values())
                .map(p -> {
                    List<Incidente> lista = porPrioridad.getOrDefault(p, List.of());
                    long resueltos = lista.stream().filter(i -> i.getFechaResolucion() != null).count();
                    long incumplidos = lista.stream().filter(Incidente::slaIncumplido).count();
                    double cumplimiento = lista.isEmpty() ? 100.0
                            : redondear(100.0 * (lista.size() - incumplidos) / lista.size());
                    return new TiemposPorPrioridad(p, lista.size(), resueltos,
                            promedio(lista, Incidente::minutosPrimeraRespuesta),
                            promedio(lista, Incidente::minutosResolucion),
                            p.getHorasResolucion(), incumplidos, cumplimiento);
                })
                .toList();
    }

    public List<DesempenoTecnico> desempenoTecnicos() {
        return usuarioService.listarTecnicos().stream()
                .map(this::desempeno)
                .sorted((a, b) -> Long.compare(b.activos(), a.activos()))
                .toList();
    }

    public Resumen resumen() {
        List<Incidente> todos = incidenteRepository.findAll();
        long activos = todos.stream().filter(i -> !i.getEstado().esFinal()
                && i.getFechaResolucion() == null).count();
        long incumplidos = todos.stream().filter(Incidente::slaIncumplido).count();
        return new Resumen(todos.size(), activos, incumplidos,
                contar(todos, i -> i.getEstado().name()),
                contar(todos, i -> i.getPrioridad().name()),
                contar(todos, i -> i.getCategoria().name()));
    }

    private DesempenoTecnico desempeno(Usuario tecnico) {
        List<Incidente> asignados = incidenteRepository.findByTecnicoAsignadoId(tecnico.getId());
        long activos = asignados.stream()
                .filter(i -> IncidenteService.ESTADOS_ACTIVOS.contains(i.getEstado())).count();
        long resueltos = asignados.stream().filter(i -> i.getFechaResolucion() != null).count();
        long incumplidos = asignados.stream().filter(Incidente::slaIncumplido).count();
        return new DesempenoTecnico(tecnico.getId(), tecnico.getNombre(), asignados.size(), activos,
                resueltos, promedio(asignados, Incidente::minutosResolucion), incumplidos);
    }

    private static Instant desde(int dias) {
        return Instant.now().minus(Duration.ofDays(dias));
    }

    private static Double promedio(List<Incidente> lista, Function<Incidente, Long> metrica) {
        OptionalDouble prom = lista.stream().map(metrica).filter(Objects::nonNull)
                .mapToLong(Long::longValue).average();
        return prom.isPresent() ? redondear(prom.getAsDouble()) : null;
    }

    private static Map<String, Long> contar(List<Incidente> lista, Function<Incidente, String> clave) {
        return lista.stream().collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()));
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
