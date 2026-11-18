package com.empresa.incidentes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configuracion transversal: documentacion OpenAPI y CORS para clientes web. */
@Configuration
public class ConfiguracionApi implements WebMvcConfigurer {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("API - Sistema de Gestion de Incidentes de TI")
                .version("1.0.0")
                .description("Reporte de problemas tecnologicos, asignacion a tecnicos, prioridades, "
                        + "seguimiento hasta la solucion e historial para detectar problemas recurrentes."));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
