# Arquitectura de TaskFlow

Este documento ofrece a un desarrollador nuevo una visión rápida y práctica del proyecto TaskFlow.

## Capas y paquetes

- Capa de presentación (HTTP): paquete `com.taskflow.controller`. Controladores exponen rutas como `/projects`, `/tasks` y `/auth`. Ejemplo: `src/main/java/com/taskflow/controller/ProjectController.java` (`ProjectController`).
- Capa de servicio (casos de uso): paquete `com.taskflow.service`. Aquí están las reglas de orquestación y coordinación entre repositorios y mappers, por ejemplo `src/main/java/com/taskflow/service/TaskService.java` (`TaskService`).
- Capa de persistencia: paquete `com.taskflow.repository` con interfaces Spring Data JPA (por ejemplo `ProjectRepository`, `TaskRepository`).
- Modelo: paquete `com.taskflow.model` contiene entidades con comportamiento de dominio, p. ej. `src/main/java/com/taskflow/model/Task.java` (`Task`).
- DTOs y mappers: paquete `com.taskflow.dto` y `com.taskflow.mapper`. Los controladores devuelven DTOs (records) mapeados por clases como `src/main/java/com/taskflow/mapper/TaskMapper.java` (`TaskMapper`).
- Config y seguridad: `com.taskflow.config` y `com.taskflow.security` (por ejemplo `src/main/java/com/taskflow/config/SecurityConfig.java`, `src/main/java/com/taskflow/security/ProjectSecurity.java`).
- Excepciones: `com.taskflow.advice` con `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` (`GlobalExceptionHandler`).

## Recorrido de POST /projects/{projectId}/tasks

1. Petición HTTP llega a: `src/main/java/com/taskflow/controller/TaskController.java` → método `createTask(projectId, request)`.
2. El controlador valida el `@RequestBody` (DTO) con `@Valid` y comprueba que el proyecto existe llamando a `ProjectService.buscarPorId(projectId)` (o lanza `ProjectNotFoundException`).
3. El controlador delega en `TaskService.crear(request, projectId)`.
4. En `TaskService.crear` se convierte el DTO a entidad mediante `TaskMapper.aEntidadNueva(request, projectId)` y se persiste con `TaskRepository.save(...)`.
5. Tras guardar, el `TaskController` construye la cabecera `Location` y responde `201 Created` con el cuerpo mapeado por `TaskMapper.aResponse(creada)`.

Notas: el flujo respeta la separación controlador → servicio → repositorio; los mappers evitan exponer entidades en la API.

## Dónde viven las reglas de negocio

- Reglas de creación y estado están en la entidad de dominio: `src/main/java/com/taskflow/model/Task.java` (`Task`). Ejemplos: el método/fábrica `crear(...)` que inicializa el estado en `TODO`, la regla `estaVencida()` y la restricción para pasar a `DONE` (requiere responsable).
- Reglas de orquestación y validaciones que necesitan datos de varios repositorios están en `TaskService` y otros servicios del paquete `com.taskflow.service`.
- Controladores solo validan estructura y permisos básicos; no contienen lógica de negocio.
- Las verificaciones transversales (autorización para borrar proyectos, por ejemplo) usan `@PreAuthorize` combinado con `ProjectSecurity` en `com.taskflow.security`.

## Seguridad con JWT

- La aplicación usa JWT sin estado. La configuración está en `src/main/java/com/taskflow/config/SecurityConfig.java` y componentes de seguridad en `src/main/java/com/taskflow/security`.
- Endpoints públicos: `/auth/**`, `/info`, Swagger, consola H2 y recursos estáticos (`src/main/resources/static`). Todo lo demás requiere token.
- Flujo de autenticación:
  1. `AuthController` (`src/main/java/com/taskflow/controller/AuthController.java`) recibe credenciales y delega a un servicio de auth que valida credenciales y devuelve un JWT.
  2. El cliente incluye `Authorization: Bearer <token>` en cada petición.
  3. Un filtro/interceptor valida el JWT, extrae el principal y roles, y establece el contexto de seguridad para la petición.
- Autorización: roles y políticas expresadas con anotaciones como `@PreAuthorize` y ayudas en `ProjectSecurity` para reglas basadas en la entidad (por ejemplo: solo el dueño o `ADMIN` puede borrar un proyecto).

## Organización de tests

- Unit tests: `src/test/java/...` usan JUnit 5 y Mockito; no arrancan Spring. Se prueban servicios y lógica de dominio (por ejemplo `TaskServiceTest`, pruebas para `Task.crear`).
- Slice tests: `@WebMvcTest` o `@DataJpaTest` para capas concretas (controladores con mappers o repositorios con JPA). Estas pruebas aíslan dependencias externas.
- Integration tests: `@SpringBootTest` con perfil `test` y Testcontainers para tests `*IT.java`. Por defecto no se ejecutan con `mvn test` salvo que se active `-Ddocker.tests=true`.
- Comandos útiles:
  - Ejecutar la suite normal: `mvn -q test`
  - Ejecutar una clase de test: `mvn -q test "-Dtest=TaskServiceTest"`
  - Correr la app con datos de ejemplo: `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"` (con `DataSeeder` que llena usuarios `ana`, `luis`, `admin`).

## Referencias rápidas de archivos/clases (ejemplos)

- `src/main/java/com/taskflow/controller/ProjectController.java` — `ProjectController`
- `src/main/java/com/taskflow/controller/AuthController.java` — `AuthController`
- `src/main/java/com/taskflow/service/TaskService.java` — `TaskService`
- `src/main/java/com/taskflow/mapper/TaskMapper.java` — `TaskMapper`
- `src/main/java/com/taskflow/model/Task.java` — `Task`
- `src/main/java/com/taskflow/repository/TaskRepository.java` — `TaskRepository`
- `src/main/java/com/taskflow/config/SecurityConfig.java` — configuración de seguridad (JWT)
- `src/main/java/com/taskflow/security/ProjectSecurity.java` — reglas finas de autorización
- `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` — manejo uniforme de errores
- `src/main/java/com/taskflow/config/DataSeeder.java` — seeded data para perfil `h2`

---
Consejo práctico: al cambiar reglas de negocio, modificar la entidad del dominio (`Task`) y añadir tests unitarios para las reglas; mantén `TaskService` como orquestador y `ProjectController` como adaptador HTTP.
