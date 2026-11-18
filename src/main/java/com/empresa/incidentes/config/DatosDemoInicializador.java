package com.empresa.incidentes.config;

import com.empresa.incidentes.model.Categoria;
import com.empresa.incidentes.model.Comentario;
import com.empresa.incidentes.model.EstadoIncidente;
import com.empresa.incidentes.model.EventoHistorial;
import com.empresa.incidentes.model.Incidente;
import com.empresa.incidentes.model.Prioridad;
import com.empresa.incidentes.model.Rol;
import com.empresa.incidentes.model.Usuario;
import com.empresa.incidentes.repository.IncidenteRepository;
import com.empresa.incidentes.repository.UsuarioRepository;
import com.empresa.incidentes.service.GeneradorCodigoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Carga datos de demostracion la primera vez que se arranca la API con la base vacia, para que el tablero
 * muestre informacion real: incidentes en todos los estados, activos con fallas recurrentes y SLA vencidos.
 *
 * Se desactiva con la propiedad {@code app.datos-demo=false} (las pruebas automaticas lo hacen).
 * Los incidentes se crean con las mismas reglas de negocio de la entidad y luego se "reubican" en el tiempo
 * para simular que ocurrieron en los ultimos dias.
 */
@Component
@ConditionalOnProperty(name = "app.datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosDemoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoInicializador.class);

    private final UsuarioRepository usuarios;
    private final IncidenteRepository incidentes;
    private final GeneradorCodigoService codigos;

    public DatosDemoInicializador(UsuarioRepository usuarios, IncidenteRepository incidentes,
                                  GeneradorCodigoService codigos) {
        this.usuarios = usuarios;
        this.incidentes = incidentes;
        this.codigos = codigos;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0 || incidentes.count() > 0) {
            return;
        }
        Usuario admin = usuario("Laura Gómez", "laura.gomez@empresa.com", Rol.ADMINISTRADOR, "Mesa de ayuda", null);
        Usuario carlos = usuario("Carlos Pérez", "carlos.perez@empresa.com", Rol.REPORTANTE, "Ventas", null);
        Usuario maria = usuario("María Rodríguez", "maria.rodriguez@empresa.com", Rol.REPORTANTE, "Contabilidad", null);
        Usuario jorge = usuario("Jorge Ramírez", "jorge.ramirez@empresa.com", Rol.REPORTANTE, "Recursos Humanos", null);
        Usuario ana = usuario("Ana Martínez", "ana.martinez@empresa.com", Rol.TECNICO, "Soporte", Categoria.RED);
        Usuario pedro = usuario("Pedro Rojas", "pedro.rojas@empresa.com", Rol.TECNICO, "Soporte", Categoria.HARDWARE);
        Usuario sofia = usuario("Sofía Castro", "sofia.castro@empresa.com", Rol.TECNICO, "Soporte", Categoria.SOFTWARE);
        Usuario diego = usuario("Diego Herrera", "diego.herrera@empresa.com", Rol.TECNICO, "Seguridad", Categoria.SEGURIDAD);

        // ---- Activo con falla recurrente: router del piso 3 (4 incidentes)
        caso("Sin conexión a internet en el piso 3", "Ningún equipo del piso 3 navega desde las 8:00.",
                Categoria.RED, Prioridad.CRITICA, "ROUTER-PISO3", carlos, 24)
                .asignar(ana, admin, 10).iniciar(ana, 15)
                .comentar(ana, "El router no responde a ping; se revisa en sitio.", 30)
                .resolver(ana, "Se reinició el router y se actualizó el firmware.", 150).cerrar(carlos, 240).guardar();
        caso("Internet intermitente en el piso 3", "La conexión se cae cada 10 minutos.",
                Categoria.RED, Prioridad.ALTA, "ROUTER-PISO3", maria, 15)
                .asignar(ana, admin, 25).iniciar(ana, 60)
                .resolver(ana, "Se cambió el canal Wi-Fi por interferencia.", 420).cerrar(maria, 600).guardar();
        caso("Red muy lenta en el piso 3", "Las páginas tardan más de un minuto en cargar.",
                Categoria.RED, Prioridad.MEDIA, "ROUTER-PISO3", jorge, 6)
                .asignar(ana, admin, 40).iniciar(ana, 90)
                .comentar(ana, "Tercera falla del mes en el mismo router: se solicita reemplazo.", 120)
                .resolver(ana, "Se limitó el ancho de banda de las descargas automáticas.", 1200).guardar();
        caso("El piso 3 vuelve a quedarse sin red", "Nuevamente no hay conexión en todo el piso.",
                Categoria.RED, Prioridad.ALTA, "ROUTER-PISO3", carlos, 1)
                .asignar(ana, admin, 15).iniciar(ana, 30).enEspera(ana, "Esperando el router de reemplazo", 120).guardar();

        // ---- Impresora de contabilidad (3 incidentes)
        caso("La impresora de contabilidad no imprime", "Los trabajos quedan en cola y no salen.",
                Categoria.IMPRESION, Prioridad.BAJA, "IMPRESORA-CONT-01", maria, 20)
                .asignar(pedro, admin, 120).iniciar(pedro, 300)
                .resolver(pedro, "Se reinstaló el controlador de la impresora.", 900).cerrar(maria, 1500).guardar();
        caso("Atasco de papel constante", "Cada 5 hojas se atasca el papel.",
                Categoria.IMPRESION, Prioridad.MEDIA, "IMPRESORA-CONT-01", maria, 9)
                .asignar(pedro, admin, 60).iniciar(pedro, 240)
                .resolver(pedro, "Se cambió el rodillo de alimentación.", 2000).cerrar(maria, 2600).guardar();
        caso("La impresora imprime hojas en blanco", "Salen hojas sin texto.",
                Categoria.IMPRESION, Prioridad.MEDIA, "IMPRESORA-CONT-01", jorge, 2)
                .asignar(pedro, admin, 45).guardar();

        // ---- PC de recepción (3 incidentes)
        caso("El PC de recepción no enciende", "No da imagen ni hace ruido al prender.",
                Categoria.HARDWARE, Prioridad.ALTA, "PC-0045", jorge, 18)
                .asignar(pedro, admin, 20).iniciar(pedro, 50)
                .resolver(pedro, "Se reemplazó la fuente de poder.", 300).cerrar(jorge, 500).guardar();
        caso("El PC de recepción se reinicia solo", "Se apaga y prende varias veces al día.",
                Categoria.HARDWARE, Prioridad.MEDIA, "PC-0045", jorge, 8)
                .asignar(pedro, admin, 30).iniciar(pedro, 200)
                .resolver(pedro, "Se limpió el disipador y se cambió la pasta térmica.", 1300).cerrar(jorge, 1700).guardar();
        caso("Pantalla azul en el PC de recepción", "Aparece pantalla azul al abrir el correo.",
                Categoria.HARDWARE, Prioridad.ALTA, "PC-0045", carlos, 0)
                .guardar();

        // ---- Otros incidentes en distintos estados
        caso("No puedo entrar al sistema contable", "Dice que mi usuario está bloqueado.",
                Categoria.ACCESOS, Prioridad.ALTA, "ERP-CONTABLE", maria, 12)
                .asignar(sofia, admin, 15).iniciar(sofia, 25)
                .resolver(sofia, "Se desbloqueó la cuenta y se restableció la contraseña.", 60).cerrar(maria, 120).guardar();
        caso("Correo sospechoso con archivo adjunto", "Llegó un correo pidiendo actualizar la clave del banco.",
                Categoria.SEGURIDAD, Prioridad.CRITICA, "CORREO-CORPORATIVO", carlos, 5)
                .asignar(diego, admin, 5).iniciar(diego, 10)
                .comentar(diego, "Se bloqueó el remitente y se avisó a todo el personal.", 40)
                .resolver(diego, "Se eliminó el correo de todos los buzones y se reforzó el filtro antiphishing.", 200)
                .cerrar(carlos, 400).guardar();
        caso("Outlook no sincroniza", "No llegan correos nuevos desde ayer.",
                Categoria.CORREO, Prioridad.MEDIA, "CORREO-CORPORATIVO", jorge, 3)
                .asignar(sofia, admin, 90).iniciar(sofia, 600).guardar();
        caso("Instalar programa de nómina", "Necesito el programa de nómina en mi equipo nuevo.",
                Categoria.SOFTWARE, Prioridad.BAJA, "PC-0112", jorge, 4)
                .asignar(sofia, admin, 300).iniciar(sofia, 900).enEspera(sofia, "Esperando la licencia", 1000).guardar();
        caso("La VPN no conecta desde casa", "Error de autenticación al conectar la VPN.",
                Categoria.RED, Prioridad.ALTA, "VPN", carlos, 2)
                .asignar(ana, admin, 30).guardar();
        caso("Solicitud de acceso a carpeta compartida", "Necesito acceso a la carpeta de Ventas.",
                Categoria.ACCESOS, Prioridad.BAJA, "SERVIDOR-ARCHIVOS", carlos, 1)
                .guardar();
        caso("El escáner no envía a correo", "El escáner da error al enviar el documento.",
                Categoria.IMPRESION, Prioridad.BAJA, "ESCANER-RRHH", jorge, 7)
                .cancelar(admin, "Duplicado del incidente reportado por teléfono", 60).guardar();

        log.info("Datos de demostración cargados: {} usuarios y {} incidentes", usuarios.count(), incidentes.count());
    }

    private Usuario usuario(String nombre, String email, Rol rol, String depto, Categoria especialidad) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        u.setDepartamento(depto);
        u.setEspecialidad(especialidad);
        return usuarios.save(u);
    }

    private Caso caso(String titulo, String descripcion, Categoria categoria, Prioridad prioridad,
                      String activo, Usuario reportante, int haceDias) {
        Instant inicio = Instant.now().minus(Duration.ofDays(haceDias)).minus(Duration.ofHours(3))
                .truncatedTo(ChronoUnit.MINUTES);
        return new Caso(titulo, descripcion, categoria, prioridad, activo, reportante, inicio);
    }

    /** Pequeno guion para recrear la vida de un incidente con fechas simuladas (minutos desde su creacion). */
    private final class Caso {
        private final Incidente inc = new Incidente();
        private final Instant inicio;

        Caso(String titulo, String descripcion, Categoria categoria, Prioridad prioridad,
             String activo, Usuario reportante, Instant inicio) {
            this.inicio = inicio;
            inc.setTitulo(titulo);
            inc.setDescripcion(descripcion);
            inc.setCategoria(categoria);
            inc.setPrioridad(prioridad);
            inc.setActivoAfectado(activo);
            inc.registrarCreacion(codigos.siguienteCodigoIncidente(), reportante.getId());
            inc.setFechaCreacion(inicio);
            inc.setFechaLimiteSla(inicio.plus(Duration.ofHours(prioridad.getHorasResolucion())));
            fechar(0, 0);
        }

        Caso asignar(Usuario tecnico, Usuario quien, long min) {
            int n = inc.getHistorial().size();
            inc.asignar(tecnico, quien.getId());
            inc.setFechaAsignacion(t(min));
            fechar(n, min);
            return this;
        }

        Caso iniciar(Usuario tecnico, long min) {
            int n = inc.getHistorial().size();
            boolean primera = inc.getFechaPrimeraRespuesta() == null;
            inc.cambiarEstado(EstadoIncidente.EN_PROGRESO, tecnico.getId(), "Inicio de la atención");
            if (primera) inc.setFechaPrimeraRespuesta(t(min));
            fechar(n, min);
            return this;
        }

        Caso enEspera(Usuario quien, String motivo, long min) {
            int n = inc.getHistorial().size();
            inc.cambiarEstado(EstadoIncidente.EN_ESPERA, quien.getId(), motivo);
            fechar(n, min);
            return this;
        }

        Caso comentar(Usuario autor, String texto, long min) {
            int n = inc.getHistorial().size();
            int c = inc.getComentarios().size();
            inc.agregarComentario(autor.getId(), texto);
            for (Comentario com : inc.getComentarios().subList(c, inc.getComentarios().size())) com.setFecha(t(min));
            fechar(n, min);
            return this;
        }

        Caso resolver(Usuario tecnico, String solucion, long min) {
            int n = inc.getHistorial().size();
            inc.resolver(solucion, tecnico.getId());
            inc.setFechaResolucion(t(min));
            fechar(n, min);
            return this;
        }

        Caso cerrar(Usuario quien, long min) {
            int n = inc.getHistorial().size();
            inc.cambiarEstado(EstadoIncidente.CERRADO, quien.getId(), "Confirmado por el usuario");
            inc.setFechaCierre(t(min));
            fechar(n, min);
            return this;
        }

        Caso cancelar(Usuario quien, String motivo, long min) {
            int n = inc.getHistorial().size();
            inc.cambiarEstado(EstadoIncidente.CANCELADO, quien.getId(), motivo);
            inc.setFechaCierre(t(min));
            fechar(n, min);
            return this;
        }

        void guardar() {
            incidentes.save(inc);
        }

        private Instant t(long min) {
            Instant fecha = inicio.plus(Duration.ofMinutes(min));
            Instant ahora = Instant.now();
            return fecha.isAfter(ahora) ? ahora : fecha;
        }

        private void fechar(int desde, long min) {
            List<EventoHistorial> nuevos = inc.getHistorial().subList(desde, inc.getHistorial().size());
            for (EventoHistorial e : nuevos) e.setFecha(t(min));
        }
    }
}
