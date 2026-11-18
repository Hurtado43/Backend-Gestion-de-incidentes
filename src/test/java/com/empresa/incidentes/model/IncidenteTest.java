package com.empresa.incidentes.model;

import com.empresa.incidentes.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de las reglas del ciclo de vida (sin base de datos). */
class IncidenteTest {

    private Incidente incidente;
    private Usuario tecnico;

    @BeforeEach
    void preparar() {
        incidente = new Incidente();
        incidente.setTitulo("No enciende el PC");
        incidente.setPrioridad(Prioridad.ALTA);
        incidente.registrarCreacion("INC-000001", "u1");

        tecnico = new Usuario();
        tecnico.setId("t1");
        tecnico.setNombre("Tecnico Uno");
        tecnico.setRol(Rol.TECNICO);
    }

    @Test
    void alCrearQuedaAbiertoConSlaYEventoDeCreacion() {
        assertThat(incidente.getEstado()).isEqualTo(EstadoIncidente.ABIERTO);
        assertThat(incidente.getFechaLimiteSla()).isEqualTo(incidente.getFechaCreacion().plusSeconds(8 * 3600));
        assertThat(incidente.getHistorial()).extracting(EventoHistorial::getTipo).containsExactly(TipoEvento.CREACION);
    }

    @Test
    void asignarPasaAAsignadoYRegistraHistorial() {
        incidente.asignar(tecnico, "admin");
        assertThat(incidente.getEstado()).isEqualTo(EstadoIncidente.ASIGNADO);
        assertThat(incidente.getTecnicoAsignadoId()).isEqualTo("t1");
        assertThat(incidente.getHistorial()).extracting(EventoHistorial::getTipo)
                .containsExactly(TipoEvento.CREACION, TipoEvento.ASIGNACION, TipoEvento.CAMBIO_ESTADO);
    }

    @Test
    void noSePuedeAsignarAUnUsuarioQueNoEsTecnico() {
        Usuario reportante = new Usuario();
        reportante.setNombre("Ana");
        reportante.setRol(Rol.REPORTANTE);
        assertThatThrownBy(() -> incidente.asignar(reportante, "admin"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void transicionInvalidaEsRechazada() {
        assertThatThrownBy(() -> incidente.cambiarEstado(EstadoIncidente.CERRADO, "admin", null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se puede pasar de ABIERTO a CERRADO");
    }

    @Test
    void flujoCompletoHastaCierreCalculaTiempos() {
        incidente.asignar(tecnico, "admin");
        incidente.cambiarEstado(EstadoIncidente.EN_PROGRESO, "t1", null);
        incidente.resolver("Se cambio la fuente de poder", "t1");
        incidente.cambiarEstado(EstadoIncidente.CERRADO, "u1", "Confirmado por el usuario");

        assertThat(incidente.getEstado()).isEqualTo(EstadoIncidente.CERRADO);
        assertThat(incidente.minutosPrimeraRespuesta()).isNotNull();
        assertThat(incidente.minutosResolucion()).isNotNull();
        assertThat(incidente.slaIncumplido()).isFalse();
        assertThatThrownBy(() -> incidente.cambiarPrioridad(Prioridad.BAJA, "admin"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void reabrirLimpiaLaSolucionAnterior() {
        incidente.asignar(tecnico, "admin");
        incidente.cambiarEstado(EstadoIncidente.EN_PROGRESO, "t1", null);
        incidente.resolver("Reinicio", "t1");
        incidente.cambiarEstado(EstadoIncidente.EN_PROGRESO, "u1", "Sigue fallando");

        assertThat(incidente.getSolucion()).isNull();
        assertThat(incidente.getFechaResolucion()).isNull();
        assertThatThrownBy(() -> incidente.cambiarEstado(EstadoIncidente.RESUELTO, "t1", null))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
