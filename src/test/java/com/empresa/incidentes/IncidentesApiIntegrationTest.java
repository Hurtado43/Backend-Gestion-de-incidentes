package com.empresa.incidentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta de la API contra un MongoDB embebido (Flapdoodle):
 * recorre todos los endpoints igual que lo haria Postman.
 */
@SpringBootTest(properties = {"de.flapdoodle.mongodb.embedded.version=7.0.14", "app.datos-demo=false"})
@AutoConfigureMockMvc
class IncidentesApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void flujoCompletoDeGestionDeIncidentes() throws Exception {
        // ---- usuarios
        String admin = crearUsuario("""
                {"nombre":"Laura Admin","email":"laura@empresa.com","rol":"ADMINISTRADOR","departamento":"TI"}""");
        String reportante = crearUsuario("""
                {"nombre":"Carlos Ventas","email":"carlos@empresa.com","rol":"REPORTANTE","departamento":"Ventas"}""");
        String tecRed = crearUsuario("""
                {"nombre":"Ana Redes","email":"ana@empresa.com","rol":"TECNICO","especialidad":"RED"}""");
        String tecHw = crearUsuario("""
                {"nombre":"Pedro Hardware","email":"pedro@empresa.com","rol":"TECNICO","especialidad":"HARDWARE"}""");

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\",\"email\":\"laura@empresa.com\",\"rol\":\"REPORTANTE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"email\":\"no-es-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasSize(3)));
        mvc.perform(get("/api/usuarios/tecnicos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(put("/api/usuarios/" + reportante).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Carlos Ventas\",\"email\":\"carlos@empresa.com\",\"rol\":\"REPORTANTE\",\"departamento\":\"Comercial\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.departamento").value("Comercial"));

        // ---- reportar incidente
        JsonNode inc = cuerpo(mvc.perform(post("/api/incidentes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Sin conexion a internet","descripcion":"No carga ninguna pagina",
                                 "categoria":"RED","prioridad":"ALTA","activoAfectado":" router-piso3 ",
                                 "reportadoPorId":"%s"}""".formatted(reportante)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("INC-000001"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.activoAfectado").value("ROUTER-PISO3")));
        String id = inc.get("id").asText();

        // busqueda por codigo y filtros
        mvc.perform(get("/api/incidentes/INC-000001")).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/incidentes").param("estado", "ABIERTO").param("texto", "internet"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/incidentes").param("estado", "PATATA")).andExpect(status().isBadRequest());

        // transicion invalida
        cambiarEstado(id, "EN_PROGRESO", tecRed).andExpect(status().isConflict());

        // asignacion automatica -> especialista en RED
        mvc.perform(patch("/api/incidentes/" + id + "/asignar-automatico").param("usuarioId", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoAsignadoId").value(tecRed))
                .andExpect(jsonPath("$.estado").value("ASIGNADO"));

        // reasignacion manual y cambio de prioridad
        asignar(id, tecHw, admin).andExpect(jsonPath("$.tecnicoAsignadoId").value(tecHw));
        asignar(id, reportante, admin).andExpect(status().isConflict());
        mvc.perform(patch("/api/incidentes/" + id + "/prioridad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prioridad\":\"CRITICA\",\"usuarioId\":\"%s\"}".formatted(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prioridad").value("CRITICA"));

        // seguimiento
        cambiarEstado(id, "EN_PROGRESO", tecHw).andExpect(status().isOk())
                .andExpect(jsonPath("$.minutosPrimeraRespuesta").value(0));
        mvc.perform(post("/api/incidentes/" + id + "/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\":\"%s\",\"texto\":\"Revisando el router\"}".formatted(tecHw)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.comentarios", hasSize(1)));
        mvc.perform(put("/api/incidentes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Sin internet en todo el piso 3\",\"usuarioId\":\"%s\"}".formatted(tecHw)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/incidentes/" + id + "/resolver").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucion\":\"Se reinicio y actualizo el firmware del router\",\"usuarioId\":\"%s\"}".formatted(tecHw)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.slaIncumplido").value(false));
        cambiarEstado(id, "CERRADO", reportante).andExpect(status().isOk());
        mvc.perform(delete("/api/incidentes/" + id)).andExpect(status().isConflict());

        mvc.perform(get("/api/incidentes/" + id + "/historial")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", hasItem("RESOLUCION")))
                .andExpect(jsonPath("$[*].tipo", hasItem("CAMBIO_PRIORIDAD")));

        // ---- recurrencia: dos incidentes mas sobre el mismo router
        String segundo = null;
        for (int n = 0; n < 2; n++) {
            segundo = cuerpo(mvc.perform(post("/api/incidentes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"titulo":"Internet intermitente","descripcion":"Se cae la conexion",
                             "categoria":"RED","prioridad":"MEDIA","activoAfectado":"ROUTER-PISO3",
                             "reportadoPorId":"%s"}""".formatted(reportante)))
                    .andExpect(status().isCreated())).get("id").asText();
        }
        mvc.perform(get("/api/incidentes/" + segundo + "/antecedentes"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("INC-000001"));
        mvc.perform(get("/api/reportes/problemas-recurrentes").param("minimo", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].activoAfectado").value("ROUTER-PISO3"))
                .andExpect(jsonPath("$[0].cantidad").value(3));

        // ---- reportes de tiempos
        mvc.perform(get("/api/reportes/tiempos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[3].prioridad").value("CRITICA"))
                .andExpect(jsonPath("$[3].resueltos").value(1));
        mvc.perform(get("/api/reportes/tecnicos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/reportes/resumen")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.porEstado.CERRADO").value(1))
                .andExpect(jsonPath("$.porEstado.ABIERTO").value(2));

        // ---- cancelar y eliminar un incidente abierto, baja logica de usuario
        mvc.perform(delete("/api/incidentes/" + segundo)).andExpect(status().isNoContent());
        mvc.perform(get("/api/incidentes/" + segundo)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/" + tecRed)).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/tecnicos")).andExpect(jsonPath("$", hasSize(1)));

        // documentacion OpenAPI disponible
        mvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    private String crearUsuario(String cuerpoJson) throws Exception {
        return cuerpo(mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(cuerpoJson))
                .andExpect(status().isCreated())).get("id").asText();
    }

    private ResultActions cambiarEstado(String id, String estado, String usuarioId) throws Exception {
        return mvc.perform(patch("/api/incidentes/" + id + "/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"%s\",\"usuarioId\":\"%s\"}".formatted(estado, usuarioId)));
    }

    private ResultActions asignar(String id, String tecnicoId, String usuarioId) throws Exception {
        return mvc.perform(patch("/api/incidentes/" + id + "/asignar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":\"%s\",\"usuarioId\":\"%s\"}".formatted(tecnicoId, usuarioId)));
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }
}
