# Instrucciones de Copilot para TaskFlow API (mejoradas)

Este archivo lo lee Copilot en cada sesión para entender el proyecto, sus convenciones y los comandos
exactos que debe usar. Si hay contradicción, este archivo tiene preferencia.

## Idioma y estilo

- Responder siempre en **español**.
- Comentarios, javadoc y mensajes de commit en español. Mantener nombres de clases/métodos tal como
  están en el código (mezcla `camelCase` / español-inglés aceptada).

## Comandos (build, test, run)

- Ejecutar la suite completa (silencioso):

  mvn -q test

- Ejecutar una sola clase de pruebas (útil para debugging rápido):

  mvn -q test "-Dtest=NombreDeLaClaseTest"

  Ejemplo: mvn -q test "-Dtest=TaskServiceTest"

- Ejecutar `verify` (incluye JaCoCo/quality gate):

  mvn verify

- Ejecutar integración con Testcontainers (añade *IT.java*):

  mvn test -Ddocker.tests=true

- Empaquetar sin tests:

  mvn -q package -DskipTests

- Correr la app en local con H2 y datos semilla (no requiere Postgres ni Docker):

  mvn spring-boot:run "-Dspring-boot.run.profiles=h2"

- Levantar con Docker Compose (imagen + Postgres):

  docker compose up --build

- Parar contenedores manteniendo datos:

  docker compose down

- Parar y borrar volumenes (arranque desde cero):

  docker compose down -v

Nota: no hay linter configurado en el pom; seguir las convenciones del repo.

## Arquitectura (alto nivel)

- Paquete raíz: `com.taskflow`.
- Capas:
  - controller: adaptadores HTTP (DTOs, validación `@Valid`) — `com.taskflow.controller`.
  - service: casos de uso / orquestación — `com.taskflow.service`.
  - repository: Spring Data JPA — `com.taskflow.repository`.
  - model: entidades/ruletas de dominio con comportamiento (reglas) — `com.taskflow.model`.
  - dto/mapper: DTOs (`record`) y mappers manuales (`com.taskflow.mapper`) para no exponer entidades.
  - config/security: `SecurityConfig`, JwtFilter, `ProjectSecurity` para reglas basadas en datos.
  - advice: `GlobalExceptionHandler` para respuestas JSON uniformes.

- Regla práctica: las reglas de negocio van en las entidades (ej. `Task.crear`, `Task.setStatus`,
  `Task.estaVencida()`); los servicios orquestan y traducen excepciones a estados HTTP.

## Convenciones clave y patrones específicos

- DTOs = `record` con Bean Validation; controladores usan `@Valid` en `@RequestBody`.
- Inyección por constructor en todas las clases con dependencias; no usar Lombok.
- POST que crea → responder `201 Created` + encabezado `Location` apuntando a la URL de lectura.
- DELETE → `204 No Content`.
- No devolver entidades JPA directamente en controladores; usar mappers a DTOs.
- Autoría/seguridad: JWT stateless; `/auth/**`, `/info`, Swagger y H2-console son públicos.
- Borrar proyecto: solo OWNER o ADMIN — se combina `@PreAuthorize` con el bean `ProjectSecurity`.
- No añadir un manejador global forzando catch de `Exception` (el proyecto ya mapea códigos concretos).

## Tests y reglas especiales

- Suite normal: `mvn -q test` (67 tests actuales).
- Quality gate JaCoCo: `mvn verify` (gate en profile `cobertura`, mínimo 70% líneas).
- Testcontainers: activar con `-Ddocker.tests=true` (añade *IT.java*). El `docker-java` client
  requiere `api.version=1.41` — el pom ya tiene soporte para esto en el profile `docker-it`.
- Ejecutar solo la clase de test con `-Dtest=...` para ahorrar tiempo.

## Archivos de referencia y reglas ocultas a recordar

- `Task` (src/main/java/com/taskflow/model/Task.java) contiene reglas importantes: fábrica `crear(...)`,
  `setStatus(...)` (no pasar a DONE sin assignee) y `estaVencida()` (fecha existe y pasada y no DONE).
- `DataSeeder` (src/main/java/com/taskflow/config/DataSeeder.java) siembra usuarios `ana/luis/admin` y
  proyectos/tareas en el perfil `h2`.
- `SecurityConfig` controla entry point (401) y access denied handler (403) — ambos deben existir.

## Reglas para el agente (copilot) — conducta operativa

- Responder en español.
- No modificar tests existentes para que pasen; arreglar código o reportar la razón.
- Tras cambiar código Java: ejecutar `mvn -q test` y reportar solo si pasó o falló.
- No hacer commits ni pushes sin autorización explícita.
- No introducir secretos en el repositorio.
- Evitar cambios fuera del alcance pedido; si se requieren, pedir permiso primero.

## Integración con otros asistentes / configs

- Se buscó configuración para otros asistentes (CLAUDE.md, .cursorrules, AGENTS.md, .windsurfrules,
  CONVENTIONS.md, AIDER_CONVENTIONS.md, .clinerules) y no se encontraron archivos relevantes.

---

ACTUALIZACIÓN: este archivo reemplaza/expande la versión previa con comandos más explícitos y
puntos clave sobre pruebas, JaCoCo y Testcontainers.

¿Quieres que aplique este cambio sustituyendo el archivo existente ahora? (si confirmas, lo escribo).
