package com.empresa.incidentes.controller;

import com.empresa.incidentes.dto.ProblemaRecurrente;
import com.empresa.incidentes.dto.Reportes.DesempenoTecnico;
import com.empresa.incidentes.dto.Reportes.Resumen;
import com.empresa.incidentes.dto.Reportes.TiemposPorPrioridad;
import com.empresa.incidentes.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Problemas recurrentes y tiempos de respuesta")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Totales por estado, prioridad y categoria")
    public Resumen resumen() {
        return reporteService.resumen();
    }

    @GetMapping("/problemas-recurrentes")
    @Operation(summary = "Activos con incidentes repetidos de la misma categoria")
    public List<ProblemaRecurrente> recurrentes(@RequestParam(defaultValue = "30") int dias,
                                                @RequestParam(defaultValue = "3") int minimo) {
        return reporteService.problemasRecurrentes(dias, minimo);
    }

    @GetMapping("/tiempos")
    @Operation(summary = "Tiempos promedio de respuesta/resolucion y cumplimiento de SLA por prioridad")
    public List<TiemposPorPrioridad> tiempos(@RequestParam(defaultValue = "30") int dias) {
        return reporteService.tiemposPorPrioridad(dias);
    }

    @GetMapping("/tecnicos")
    @Operation(summary = "Carga de trabajo y desempeno por tecnico")
    public List<DesempenoTecnico> tecnicos() {
        return reporteService.desempenoTecnicos();
    }
}
