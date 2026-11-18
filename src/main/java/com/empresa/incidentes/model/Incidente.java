package com.empresa.incidentes.model;

import com.empresa.incidentes.exception.ReglaNegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Incidente (ticket) reportado por un usuario. Es la raiz del agregado: el historial y los
 * comentarios viven embebidos dentro del mismo documento de MongoDB.
 *
 * Las reglas del ciclo de vida (asignar, cambiar estado, resolver...) se implementan aqui
 * para que ninguna capa pueda dejar al incidente en un estado invalido.
 */
@Document(collection = "incidentes")
@CompoundIndex(name = "idx_categoria_activo", def = "{'categoria': 1, 'activoAfectado': 1}")
public class Incidente {

    @Id
    private String id;
    @Indexed(unique = true)
    private String codigo;
    private String titulo;
    private String descripcion;
    private Categoria categoria;
    private Prioridad prioridad;
    @Indexed
    private EstadoIncidente estado;
    /** Equipo o servicio afectado (por ejemplo "PC-0045" o "VPN"). Se usa para detectar recurrencias. */
    private String activoAfectado;

    private String reportadoPorId;
    @Indexed
    private String tecnicoAsignadoId;

    private Instant fechaCreacion;
    private Instant fechaAsignacion;
    private Instant fechaPrimeraRespuesta;
    private Instant fechaResolucion;
    private Instant fechaCierre;
    private Instant fechaLimiteSla;

    private String solucion;

    private List<Comentario> comentarios = new ArrayList<>();
    private List<EventoHistorial> historial = new ArrayList<>();

    // ------------------------------------------------------------------ reglas de negocio

    /** Inicializa un incidente recien reportado: estado ABIERTO, SLA y primer evento del historial. */
    public void registrarCreacion(String codigo, String usuarioId) {
        this.codigo = codigo;
        this.estado = EstadoIncidente.ABIERTO;
        this.fechaCreacion = Instant.now();
        this.reportadoPorId = usuarioId;
        recalcularSla();
        registrarEvento(TipoEvento.CREACION, usuarioId, "Incidente reportado", null, estado.name());
    }

    /** Asigna (o reasigna) el incidente a un tecnico. */
    public void asignar(Usuario tecnico, String usuarioId) {
        validarNoFinalizado();
        if (!tecnico.esTecnico() || !tecnico.isActivo()) {
            throw new ReglaNegocioException("El usuario " + tecnico.getNombre() + " no es un técnico activo");
        }
        String anterior = tecnicoAsignadoId;
        this.tecnicoAsignadoId = tecnico.getId();
        this.fechaAsignacion = Instant.now();
        registrarEvento(TipoEvento.ASIGNACION, usuarioId,
                "Asignado a " + tecnico.getNombre(), anterior, tecnico.getId());
        if (estado == EstadoIncidente.ABIERTO) {
            cambiarEstado(EstadoIncidente.ASIGNADO, usuarioId, "Asignación automática");
        }
    }

    /** Cambia el estado validando las transiciones permitidas del ciclo de vida. */
    public void cambiarEstado(EstadoIncidente nuevo, String usuarioId, String motivo) {
        if (!estado.puedeCambiarA(nuevo)) {
            throw new ReglaNegocioException(
                    "No se puede pasar de " + estado + " a " + nuevo + ". Permitidos: " + estado.siguientesPermitidos());
        }
        if (nuevo == EstadoIncidente.RESUELTO && (solucion == null || solucion.isBlank())) {
            throw new ReglaNegocioException("Para resolver un incidente use la operación 'resolver' indicando la solución");
        }
        if (nuevo != EstadoIncidente.CANCELADO && nuevo != EstadoIncidente.ASIGNADO && tecnicoAsignadoId == null) {
            throw new ReglaNegocioException("El incidente debe tener un técnico asignado");
        }
        EstadoIncidente anterior = estado;
        this.estado = nuevo;
        Instant ahora = Instant.now();
        switch (nuevo) {
            case EN_PROGRESO -> {
                if (fechaPrimeraRespuesta == null) {
                    fechaPrimeraRespuesta = ahora;
                }
                if (anterior == EstadoIncidente.RESUELTO) { // reapertura: la solucion anterior queda en el historial
                    fechaResolucion = null;
                    solucion = null;
                }
            }
            case RESUELTO -> fechaResolucion = ahora;
            case CERRADO, CANCELADO -> fechaCierre = ahora;
            default -> { }
        }
        registrarEvento(TipoEvento.CAMBIO_ESTADO, usuarioId,
                motivo != null ? motivo : "Cambio de estado", anterior.name(), nuevo.name());
    }

    /** Registra la solucion aplicada y deja el incidente RESUELTO. */
    public void resolver(String solucionAplicada, String usuarioId) {
        if (solucionAplicada == null || solucionAplicada.isBlank()) {
            throw new ReglaNegocioException("La solución es obligatoria");
        }
        this.solucion = solucionAplicada;
        registrarEvento(TipoEvento.RESOLUCION, usuarioId, "Solución registrada", null, solucionAplicada);
        cambiarEstado(EstadoIncidente.RESUELTO, usuarioId, "Incidente resuelto");
    }

    /** Cambia la prioridad y recalcula la fecha limite de SLA. */
    public void cambiarPrioridad(Prioridad nueva, String usuarioId) {
        validarNoFinalizado();
        Prioridad anterior = prioridad;
        this.prioridad = nueva;
        recalcularSla();
        registrarEvento(TipoEvento.CAMBIO_PRIORIDAD, usuarioId, "Cambio de prioridad",
                anterior != null ? anterior.name() : null, nueva.name());
    }

    public void agregarComentario(String autorId, String texto) {
        comentarios.add(new Comentario(autorId, texto));
        registrarEvento(TipoEvento.COMENTARIO, autorId, texto, null, null);
    }

    public void actualizarDatos(String nuevoTitulo, String nuevaDescripcion, Categoria nuevaCategoria,
                                String nuevoActivo, String usuarioId) {
        validarNoFinalizado();
        if (nuevoTitulo != null) titulo = nuevoTitulo;
        if (nuevaDescripcion != null) descripcion = nuevaDescripcion;
        if (nuevaCategoria != null) categoria = nuevaCategoria;
        if (nuevoActivo != null) activoAfectado = nuevoActivo;
        registrarEvento(TipoEvento.ACTUALIZACION, usuarioId, "Datos del incidente actualizados", null, null);
    }

    // ------------------------------------------------------------------ metricas

    /** Minutos desde la creacion hasta que un tecnico empezo a trabajar (null si aun no hay respuesta). */
    public Long minutosPrimeraRespuesta() {
        return fechaPrimeraRespuesta == null ? null
                : Duration.between(fechaCreacion, fechaPrimeraRespuesta).toMinutes();
    }

    /** Minutos desde la creacion hasta la resolucion (null si no esta resuelto). */
    public Long minutosResolucion() {
        return fechaResolucion == null ? null
                : Duration.between(fechaCreacion, fechaResolucion).toMinutes();
    }

    /** true si el incidente supero (o ya supera) su fecha limite de resolucion. */
    public boolean slaIncumplido() {
        if (fechaLimiteSla == null) return false;
        Instant referencia = fechaResolucion != null ? fechaResolucion
                : (estado == EstadoIncidente.CANCELADO ? fechaLimiteSla : Instant.now());
        return referencia.isAfter(fechaLimiteSla);
    }

    // ------------------------------------------------------------------ auxiliares

    private void recalcularSla() {
        if (prioridad != null && fechaCreacion != null) {
            fechaLimiteSla = fechaCreacion.plus(Duration.ofHours(prioridad.getHorasResolucion()));
        }
    }

    private void validarNoFinalizado() {
        if (estado != null && estado.esFinal()) {
            throw new ReglaNegocioException("El incidente " + codigo + " está " + estado + " y no admite cambios");
        }
    }

    private void registrarEvento(TipoEvento tipo, String usuarioId, String descripcion,
                                 String anterior, String nuevo) {
        historial.add(new EventoHistorial(tipo, usuarioId, descripcion, anterior, nuevo));
    }

    // ------------------------------------------------------------------ getters / setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public EstadoIncidente getEstado() { return estado; }
    public void setEstado(EstadoIncidente estado) { this.estado = estado; }
    public String getActivoAfectado() { return activoAfectado; }
    public void setActivoAfectado(String activoAfectado) { this.activoAfectado = activoAfectado; }
    public String getReportadoPorId() { return reportadoPorId; }
    public void setReportadoPorId(String reportadoPorId) { this.reportadoPorId = reportadoPorId; }
    public String getTecnicoAsignadoId() { return tecnicoAsignadoId; }
    public void setTecnicoAsignadoId(String tecnicoAsignadoId) { this.tecnicoAsignadoId = tecnicoAsignadoId; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Instant getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(Instant fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
    public Instant getFechaPrimeraRespuesta() { return fechaPrimeraRespuesta; }
    public void setFechaPrimeraRespuesta(Instant fechaPrimeraRespuesta) { this.fechaPrimeraRespuesta = fechaPrimeraRespuesta; }
    public Instant getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(Instant fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public Instant getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Instant fechaCierre) { this.fechaCierre = fechaCierre; }
    public Instant getFechaLimiteSla() { return fechaLimiteSla; }
    public void setFechaLimiteSla(Instant fechaLimiteSla) { this.fechaLimiteSla = fechaLimiteSla; }
    public String getSolucion() { return solucion; }
    public void setSolucion(String solucion) { this.solucion = solucion; }
    public List<Comentario> getComentarios() { return comentarios; }
    public void setComentarios(List<Comentario> comentarios) { this.comentarios = comentarios; }
    public List<EventoHistorial> getHistorial() { return historial; }
    public void setHistorial(List<EventoHistorial> historial) { this.historial = historial; }
}
