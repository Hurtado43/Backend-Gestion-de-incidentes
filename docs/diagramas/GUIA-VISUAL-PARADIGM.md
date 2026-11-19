# Diagramas en Visual Paradigm

## Dónde están

Los 5 diagramas están **guardados en tu VP Workspace** (Visual Paradigm Online → *My artifacts* → proyecto **Draft**),
hechos con figuras UML nativas de VP, así que se editan como cualquier diagrama de Visual Paradigm:

| Archivo en VP Online | Tipo de diagrama en VP |
|---|---|
| 1 - Vista de contexto (Incidentes TI) | Data Flow Diagram (diagrama de contexto: proceso central + entidades externas) |
| 2 - Vista funcional - Componentes (Incidentes TI) | Component Diagram |
| 3 - Vista logica - Clases (Incidentes TI) | Class Diagram (clases con atributos, operaciones, estereotipos y multiplicidades) |
| 4 - Vista de desarrollo - Paquetes (Incidentes TI) | Package Diagram |
| 5 - Vista de despliegue (Incidentes TI) | Deployment Diagram («device», «executionEnvironment», «artifact») |

Para abrirlos: <https://online.visual-paradigm.com> → inicia sesión → *My artifacts* → doble clic en el diagrama.
Desde el editor puedes exportarlos (*Export* → PNG, JPG, SVG o PDF) para el informe.

Los diagramas 2 (componentes), 4 (paquetes) y 5 (despliegue) ya incluyen la capa del frontend (Vaadin), igual que
las versiones de `plantuml/` y `png/`.

## Copias en esta carpeta

- `plantuml/*.puml`: el mismo diseño en código PlantUML.
- `png/*.png`: imágenes generadas desde el PlantUML (son las que usa la presentación).
- `ENLACES-VPASCODE.md`: un enlace por diagrama que lo abre en VPasCode, el editor de diagramas como código de VP.

Con la cuenta gratuita de VP Online **sí** se pueden crear, guardar y exportar estos diagramas. Lo que exige una
licencia de pago (VP Online Combo o superior, o VP Desktop Professional con mantenimiento vigente) es guardar
artefactos de VPasCode y su botón "Editar en VP Online".

## Si los necesitas en Visual Paradigm Desktop

- **Diagrama de clases:** *Tools → Code → Instant Reverse…* → Java → carpeta
  `incidentes-api/src/main/java/com/empresa/incidentes/model`, y arrastra las clases desde el *Model Explorer*.
- **El resto:** recréalos usando como referencia los diagramas de VP Online o los PNG.

## Regenerar los PNG

Con `plantuml.jar` (no necesita Graphviz porque los diagramas usan `!pragma layout smetana`):

```bash
java -jar plantuml.jar -charset UTF-8 -tpng -o ../png plantuml/*.puml
```
