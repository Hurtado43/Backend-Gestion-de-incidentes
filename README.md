# API REST – Sistema de Gestión de Incidentes de TI

Plataforma para que una empresa reporte y gestione problemas tecnológicos: se asignan a técnicos,
se priorizan, se hace seguimiento hasta su solución y se guarda un historial para detectar
problemas recurrentes y medir los tiempos de respuesta.

**Tecnologías:** Java 21 · Spring Boot 3.5 · Spring Data MongoDB · Bean Validation · springdoc-openapi (Swagger) ·
JUnit 5 + MongoDB embebido (Flapdoodle) para las pruebas.

```
Cliente (frontend Vaadin "Sala de control" / Postman)  --HTTP/JSON-->  Servidor Spring Boot (esta API)  --TCP 27017-->  MongoDB
```

El frontend está en su propio repositorio ([frontend-Gestion-de-inicidentes](https://github.com/Hurtado43/frontend-Gestion-de-inicidentes)): una interfaz interactiva en Java (Vaadin) que consume
esta API. Los diagramas de diseño (contexto, funcional, lógico, desarrollo y despliegue) están en [`docs/diagramas`](docs/diagramas)
y las historias de usuario en [`docs/HISTORIAS-DE-USUARIO.md`](docs/HISTORIAS-DE-USUARIO.md).

## Cómo ejecutarla

Requisitos: JDK 21, Maven 3.9 y un MongoDB accesible.

1. Tener MongoDB corriendo. Opciones:
   - **Local:** instalar MongoDB Community Server (queda en `mongodb://localhost:27017`).
   - **MongoDB Atlas (nube, gratis):** crear un clúster y copiar la cadena de conexión.
2. Arrancar la API desde la carpeta `incidentes-api`:

```bash
mvn spring-boot:run
```

Con Atlas u otro servidor, define la variable `MONGODB_URI` antes de arrancar
(por ejemplo `mongodb+srv://usuario:clave@cluster0.xxxx.mongodb.net/incidentes_db`).
Si el puerto 8080 está ocupado, define `PORT` (por ejemplo `PORT=8081`).

3. Abrir la documentación interactiva: <http://localhost:8080/swagger-ui.html>
4. Importar en Postman `postman/Incidentes-TI.postman_collection.json` y ejecutar las carpetas en orden
   (1 → 4). La colección guarda sola los ids y el código creados en variables y usa emails con `{{$timestamp}}`,
   así que se puede ejecutar varias veces, también sobre los datos de demostración.

### Datos de demostración

La primera vez que arranca con la base vacía, la API carga 8 usuarios (1 administrador, 3 empleados y 4 técnicos) y
17 incidentes repartidos en los últimos 25 días, en todos los estados, con activos que fallan varias veces
(`ROUTER-PISO3`, `PC-0045`, `IMPRESORA-CONT-01`) y algunos SLA vencidos, para que los reportes muestren información.
Se crean con las mismas reglas de negocio de la API. Para arrancar sin ellos: `APP_DATOS_DEMO=false`.
Para volver a cargarlos, borra la base `incidentes_db` y reinicia.

### Pruebas automáticas

```bash
mvn test
```

Ejecuta 6 pruebas unitarias del ciclo de vida del incidente y una prueba de integración que recorre todos
los endpoints contra un MongoDB embebido (la primera vez descarga el binario oficial de MongoDB, ~120 MB).

## Modelo de dominio

| Documento (colección) | Contenido |
|---|---|
| `usuarios` | Reportantes, técnicos (con especialidad) y administradores. Baja lógica (`activo`). |
| `incidentes` | Código `INC-000001`, título, descripción, categoría, prioridad, estado, activo afectado, reportante, técnico, fechas (creación, asignación, primera respuesta, resolución, cierre, límite SLA), solución. **Historial** y **comentarios** van embebidos. |
| `contadores` | Secuencia atómica para generar los códigos `INC-…`. |

**Ciclo de vida** (validado en `EstadoIncidente` / `Incidente`):

```
ABIERTO → ASIGNADO → EN_PROGRESO ⇄ EN_ESPERA → RESUELTO → CERRADO
                                       ↑______________|  (reabrir)
Cualquier estado no final → CANCELADO
```

**SLA por prioridad** (horas de respuesta / resolución): BAJA 24/72 · MEDIA 8/24 · ALTA 2/8 · CRÍTICA 1/4.

## Endpoints

Como el proyecto no implementa autenticación, cada operación indica en el cuerpo quién la hace
(`usuarioId` / `autorId`), y eso queda en el historial.

### Usuarios – `/api/usuarios`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/usuarios` | Registrar usuario `{nombre, email, rol, departamento, especialidad}` |
| GET | `/api/usuarios` | Listar usuarios |
| GET | `/api/usuarios/tecnicos` | Técnicos activos |
| GET | `/api/usuarios/{id}` | Consultar |
| PUT | `/api/usuarios/{id}` | Actualizar |
| DELETE | `/api/usuarios/{id}` | Desactivar (baja lógica) |

### Incidentes – `/api/incidentes`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/incidentes` | Reportar `{titulo, descripcion, categoria, prioridad, activoAfectado, reportadoPorId}` |
| GET | `/api/incidentes?estado=&prioridad=&categoria=&tecnicoId=&reportadoPorId=&texto=` | Listar con filtros |
| GET | `/api/incidentes/{id o código}` | Consultar (acepta `INC-000001`) |
| PUT | `/api/incidentes/{id}` | Actualizar datos |
| PATCH | `/api/incidentes/{id}/asignar` | Asignar técnico `{tecnicoId, usuarioId}` |
| PATCH | `/api/incidentes/{id}/asignar-automatico?usuarioId=` | Asigna al técnico con menos carga (prefiere especialistas) |
| PATCH | `/api/incidentes/{id}/estado` | Cambiar estado `{estado, usuarioId, motivo}` |
| PATCH | `/api/incidentes/{id}/prioridad` | Cambiar prioridad (recalcula SLA) |
| POST | `/api/incidentes/{id}/comentarios` | Comentar `{autorId, texto}` |
| PATCH | `/api/incidentes/{id}/resolver` | Registrar solución `{solucion, usuarioId}` |
| GET | `/api/incidentes/{id}/historial` | Historial completo |
| GET | `/api/incidentes/{id}/antecedentes` | Incidentes resueltos antes sobre el mismo activo |
| DELETE | `/api/incidentes/{id}` | Eliminar (solo ABIERTO o CANCELADO) |

### Reportes – `/api/reportes`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/reportes/resumen` | Totales por estado, prioridad y categoría |
| GET | `/api/reportes/problemas-recurrentes?dias=30&minimo=3` | Activos con incidentes repetidos de la misma categoría (agregación MongoDB) |
| GET | `/api/reportes/tiempos?dias=30` | Promedio de minutos de respuesta / resolución y % de cumplimiento de SLA por prioridad |
| GET | `/api/reportes/tecnicos` | Carga y desempeño por técnico |

### Errores
Respuesta uniforme `{status, error, mensaje, ruta, detalles, fecha}`:
`400` datos inválidos · `404` no encontrado · `409` regla de negocio violada (por ejemplo, una transición de estado no permitida).

## Estructura del código

```
com.empresa.incidentes
├── controller   UsuarioController, IncidenteController, ReporteController
├── service      UsuarioService, IncidenteService, ReporteService, GeneradorCodigoService
├── repository   UsuarioRepository, IncidenteRepository (+ Custom/Impl con MongoTemplate)
├── model        Usuario, Incidente, EventoHistorial, Comentario, Contador + enums
├── dto          Solicitudes, IncidenteResponse, Reportes, ProblemaRecurrente, FiltroIncidentes, ErrorResponse
├── exception    ManejadorGlobalErrores, ReglaNegocioException, RecursoNoEncontradoException
└── config       ConfiguracionApi (OpenAPI + CORS)
```

Los diagramas de diseño (contexto, funcional, lógico, desarrollo y despliegue) están en `../diagramas`.
