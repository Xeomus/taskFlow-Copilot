# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `f1cc32fe-57a6-44c5-95be-57886bf35511`  
> - **Started:** 9/18/2026, 11:27:12 PM  
> - **Duration:** 1m 12s  
> - **Exported:** 9/18/2026, 11:28:25 PM  

---

<sub>2s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/\<feature>.md.

---

<sub>8s</sub>

### Copilot

Leer el diff y el directorio de specs en paralelo para identificar la especificación referida y los archivos modificados; así se puede comparar cada archivo del diff con su versión actual y con la especificación. Ejecutando dos lecturas: semana6/proyecto-final.diff y la carpeta specs.

---

<sub>8s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\semana6\proyecto-final.diff.**

C:\Users\navae\Downloads\taskflow-IA\semana6\proyecto-final.diff

<details>
<summary>35 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ProjectController.java b/src/main/java/com/taskflow/controller/ProjectController.java
index 00b6805..3144979 100644
--- a/src/main/java/com/taskflow/controller/ProjectController.java
+++ b/src/main/java/com/taskflow/controller/ProjectController.java
@@ -134,10 +134,11 @@ public class ProjectController {
      */
     @Operation(summary = "Borra un proyecto y sus tareas",
             description = "Solo ADMIN o el owner. Un USER no-owner recibe 403.")
-    @PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")
+    @PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#p0, authentication.name)")
     @DeleteMapping("/projects/{id}")
     public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) {
         projectService.eliminar(id);
         return ResponseEntity.noContent().build();
     }
 }
+
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index 76d7de1..c094633 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -116,10 +116,12 @@ public class ProjectService {
         int overdue = 0;
         for (Task t : tareas) {
             byStatus.put(t.getStatus(), byStatus.get(t.getStatus()) + 1);
-            if (t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now())) {
+            if (t.estaVencida()) {
                 overdue++;
             }
         }
         return new com.taskflow.dto.ProjectSummaryResponse(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
     }
 }
+
+
```

</details>


---

<sub>8s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\specs.**

C:\Users\navae\Downloads\taskflow-IA\specs

```
overdue.md
search.md
summary.md
unassigned.md
```


---

<sub>13s</sub>

### Copilot

Leyendo la especificación specs/summary.md y los archivos actuales modificados (ProjectController.java y ProjectService.java) para comparar contra el diff. Ejecutando 3 lecturas en paralelo.

---

<sub>13s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\specs\summary.md.**

C:\Users\navae\Downloads\taskflow-IA\specs\summary.md

<details>
<summary>52 lines</summary>

````
## Qué se pide

Un endpoint nuevo, `GET /projects/{id}/summary`, que resume un proyecto: **cuántas tareas tiene en
cada estado y cuántas están vencidas**. Sirve para pintar un tablero sin descargar todas las tareas del
proyecto. Pide el token de `POST /auth/login`, como todo lo que no es público.

## Respuesta 200

```json
{
  "projectId": 2,
  "projectName": "App Móvil",
  "totalTasks": 4,
  "byStatus": { "TODO": 1, "IN_PROGRESS": 2, "DONE": 1 },
  "overdue": 1
}
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto pedido |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `byStatus` | objeto | una clave por cada valor de `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`), **siempre las tres**, aunque valgan 0 |
| `overdue` | número | cuántas tareas del proyecto cumplen `Task.estaVencida()` |

## Reglas

1. **Vencida** es exactamente lo que ya dice `Task.estaVencida()`: tiene fecha límite, la fecha ya pasó y
   el estado no es `DONE`. Se reutiliza ese método; no se reescribe la regla en el servicio.
2. Un proyecto **sin tareas** responde 200 con `totalTasks` 0, las tres claves de `byStatus` en 0 y `overdue` 0.
3. Un proyecto **que no existe** responde **404** con el `ErrorResponse` uniforme de siempre
   (`ProjectNotFoundException`, igual que `GET /projects/{id}`).
4. **Sin token** responde **401**, como todo lo que no es público.
5. Cualquier usuario autenticado puede pedir el resumen de cualquier proyecto (igual que `GET /projects/{id}/tasks`).

## Resultado esperado con la semilla del perfil `h2`

| Petición | `totalTasks` | `byStatus` | `overdue` |
|---|---|---|---|
| `GET /projects/1/summary` | 5 | `TODO` 3 · `IN_PROGRESS` 1 · `DONE` 1 | 0 (la tarea 2 tiene fecha pasada, pero está `DONE`) |
| `GET /projects/2/summary` | 4 | `TODO` 1 · `IN_PROGRESS` 2 · `DONE` 1 | 1 (la tarea 7) |
| `GET /projects/3/summary` | 0 | `TODO` 0 · `IN_PROGRESS` 0 · `DONE` 0 | 0 |
| `GET /projects/99/summary` | — | — | 404 |

Si antes de probar creaste tareas en un proyecto (por la UI o con el servidor MCP), `totalTasks` y
`TODO` de ese proyecto suben en la misma cantidad. Al reiniciar la app con el perfil `h2` vuelven a
ser los de la tabla.

## Cómo se construye (convenciones de TaskFlow)

- DTO de salida: un `record` en `com.taskflow.dto`, `ProjectSummaryResponse`.
- La lógica de conteo va en el servicio (`ProjectService`), no en el controller.
- El controller solo traduce HTTP: busca el proyecto (404 si no existe) y delega.
- Tests:
  - **unit** del servicio con Mockito: un proyecto con tareas en los tres estados y una vencida; un proyecto sin tareas.
  - **slice** `@WebMvcTest` del controller: 200 con el JSON de arriba y 404 con un proyecto inexistente.
- `mvn -q test` en verde al terminar, sin modificar tests existentes.

## Criterios de aceptación

- [ ] `GET /projects/2/summary` con el token de `ana` devuelve el JSON del ejemplo.
- [ ] `GET /projects/3/summary` devuelve ceros en los cinco números.
- [ ] `GET /projects/99/summary` devuelve 404.
- [ ] `mvn -q test` en verde: los tests de antes más los nuevos.
````

</details>


---

<sub>13s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\controller\ProjectController.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\controller\ProjectController.java

<details>
<summary>131 lines</summary>

```
package com.taskflow.controller;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.TaskResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.mapper.ProjectMapper;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * ProjectController — la puerta HTTP de los proyectos. HOY (integrador) crece a CRUD completo, en
 * DTOs (ProjectRequest / ProjectResponse); las tareas de un proyecto salen como TaskResponse.
 *
 * Repetición deliberada del patrón de TaskController: mismos gestos (201 + Location, @Valid, 404 vía
 * orElseThrow -> advice). El lado Project cierra la tabla de endpoints del capstone (menos /auth/*).
 */
@RestController
@Tag(name = "Projects", description = "CRUD de proyectos y sus tareas. Borrar un proyecto arrastra sus tareas (cascada).")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** GET /projects — todos los proyectos como ProjectResponse. */
    @Operation(summary = "Lista todos los proyectos",
            description = "Devuelve todos los proyectos como ProjectResponse (lista vacía si no hay ninguno).")
    @GetMapping("/projects")
    public List<ProjectResponse> getProjects() {
        return projectService.listar().stream()
                .map(ProjectMapper::aResponse)
                .toList();
    }

    /** GET /projects/{id} — 200 con ProjectResponse; 404 uniforme si no existe. */
    @Operation(summary = "Obtiene un proyecto por id",
            description = "200 con el ProjectResponse; 404 uniforme si el id no existe.")
    @GetMapping("/projects/{id}")
    public ProjectResponse getProject(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return ProjectMapper.aResponse(proyecto);
    }

    /**
     * GET /projects/{id}/tasks — las tareas de un proyecto como TaskResponse. Conserva la distinción
     * de D2: proyecto inexistente -> 404 (orElseThrow); proyecto sin tareas -> 200 con []. El filtro
     * ?status= es STRETCH (mismo enum que /tasks). Sigue delegando en ProjectService.tareasDe.
     */
    @Operation(summary = "Lista las tareas de un proyecto",
            description = "Tareas del proyecto como TaskResponse; 404 si el proyecto no existe, 200 con [] si no tiene tareas. Filtro opcional ?status= (stretch).")
    @GetMapping("/projects/{id}/tasks")
    public List<TaskResponse> getTareasDeProyecto(
            @PathVariable("id") Long id,
            @RequestParam(name = "status", required = false) TaskStatus status) {   // status: STRETCH
        projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        List<Task> tareas = projectService.tareasDe(id);
        if (status != null) {                          // STRETCH: filtro opcional por estado
            tareas = tareas.stream().filter(t -> t.getStatus() == status).toList();
        }
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    @Operation(summary = "Resumen del proyecto",
            description = "Devuelve un resumen con recuentos de tareas por estado y cuántas están vencidas. 404 si el proyecto no existe.")
    @GetMapping("/projects/{id}/summary")
    public com.taskflow.dto.ProjectSummaryResponse getProjectSummary(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return projectService.resumenDe(proyecto);
    }

    /**
     * POST /projects — 201 + Location a /projects/{id}. @Valid dispara Bean Validation (400 si falla).
     * MP-9: el owner sale del JWT — el Authentication (inyectado por Spring Security) trae el username
     * del token; el service lo resuelve a ownerId. El dueño es QUIEN crea, no una constante.
     */
    @Operation(summary = "Crea un proyecto",
            description = "El owner se toma del usuario autenticado (el username del JWT).")
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request,
                                                         Authentication authentication) {
        Project creado = projectService.crear(request, authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/projects/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProjectMapper.aResponse(creado));
    }

    /** PUT /projects/{id} — reemplazo COMPLETO. 200 con ProjectResponse; 404 si no existe. */
    @Operation(summary = "Reemplaza un proyecto",
            description = "Conserva id, owner y createdAt; name y description vienen del cuerpo.")
    @PutMapping("/projects/{id}")
    public ProjectResponse updateProject(@PathVariable("id") Long id,
                                         @Valid @RequestBody ProjectRequest request) {
        Project actualizado = projectService.reemplazar(id, request);
        return ProjectMapper.aResponse(actualizado);
    }

    /**
     * DELETE /projects/{id} — 204 No Content; 404 si no existe. Borra en cascada las tareas del
     * proyecto (regla "no Task sin Project").
     *
     * MP-9: la regla del capstone EN SERIO. @PreAuthorize se evalúa ANTES del método: pasa si el
     * usuario es ADMIN o es el owner del proyecto (bean @projectSecurity, data-driven). Un USER que no
     * es owner -> 403 (la API SÍ sabe quién es, por eso 403 y no 401). Proyecto inexistente ->
     * esOwner devuelve true a propósito para que hable el 404 del servicio.
     */
    @Operation(summary = "Borra un proyecto y sus tareas",
            description = "Solo ADMIN o el owner. Un USER no-owner recibe 403.")
    @PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#p0, authentication.name)")
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

</details>


---

<sub>13s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\service\ProjectService.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\service\ProjectService.java

<details>
<summary>113 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Cambios de HOY (S2D4) — y por qué la promesa del día NO lo cubre:
 *   - MP-4: el modelo Project se aplanó (User owner -> Long ownerId), así que 'crear' y 'reemplazar'
 *     manejan un ownerId (Long), no un objeto User. Antes 'crear' construía con 0L (convención del
 *     InMemory de "aún sin id"); ahora construye con null y la BD asigna el id (IDENTITY).
 *   - MP-6: 'tareasDe' cambió su stream-filter de findAll() por taskRepository.findByProjectId(id)
 *     — la nota de D3 ("en D4 esto se vuelve un query method") se paga AQUÍ. La promesa "no tocar el
 *     servicio" protege a TaskService (que no se tocó), no a ProjectService (que ya se tocó en MP-4).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;   // D5: para resolver el owner desde el username del JWT

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Todos los proyectos. */
    public List<Project> listar() {
        return projectRepository.findAll();
    }

    /** Un proyecto por id (el Optional sube tal cual: el controller decide 404 vs 200). */
    public Optional<Project> buscarPorId(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Las tareas de un proyecto: MP-6 sustituye el stream-filter de D2 por la derived query
     * taskRepository.findByProjectId(projectId) — el LIKE... perdón, el WHERE project_id = ? lo hace
     * la BD (viajan solo las filas del proyecto). "no existe el proyecto" (404) lo decide el
     * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
     * [] (-> 200 con []). No son lo mismo.
     */
    public List<Task> tareasDe(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Crea un proyecto (POST): el request trae name y description; el ownerId ya NO es una constante
     * (murió el 1L fijo de D3) — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
     * Authentication). El dueño no lo decide el cliente ni una semilla: es QUIEN llama. createdAt = hoy;
     * id == null -> la BD lo asigna (IDENTITY).
     */
    public Project crear(ProjectRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new UsernameNotFoundException(ownerUsername));
        Project nuevo = new Project(null, request.name(), request.description(),
                owner.getId(), LocalDate.now());
        return projectRepository.save(nuevo);
    }

    /**
     * Reemplazo COMPLETO (PUT): existe -> reconstruye con el MISMO id, conservando ownerId y createdAt
     * (el cliente no los toca); name y description vienen del request. save con id presente = update.
     * No existe -> 404.
     */
    public Project reemplazar(Long id, ProjectRequest request) {
        Project actual = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        Project reemplazo = new Project(actual.getId(), request.name(), request.description(),
                actual.getOwnerId(), actual.getCreatedAt());
        return projectRepository.save(reemplazo);
    }

    /**
     * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
     * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
     * una violación de integridad) y luego el proyecto. No existe -> 404. La regla owner/ADMIN llega
     * en D5 (hoy queda abierto, documentado en el @Operation del controller).
     */
    public void eliminar(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
        projectRepository.deleteById(id);
    }

    /**
     * Resumen del proyecto: cuenta tareas por estado y cuántas están vencidas.
     * Reusa Task.estaVencida().
     */
    public com.taskflow.dto.ProjectSummaryResponse resumenDe(Project proyecto) {
        List<Task> tareas = tareasDe(proyecto.getId());
        int total = tareas.size();
        java.util.Map<com.taskflow.model.TaskStatus, Integer> byStatus = new java.util.EnumMap<>(com.taskflow.model.TaskStatus.class);
        for (com.taskflow.model.TaskStatus s : com.taskflow.model.TaskStatus.values()) {
            byStatus.put(s, 0);
        }
        int overdue = 0;
        for (Task t : tareas) {
            byStatus.put(t.getStatus(), byStatus.get(t.getStatus()) + 1);
            if (t.estaVencida()) {
                overdue++;
            }
        }
        return new com.taskflow.dto.ProjectSummaryResponse(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
    }
}
```

</details>


---

<sub>24s</sub>

### Copilot

Comprobando si existe el DTO ProjectSummaryResponse y si se añadieron tests nuevos: listar el paquete dto y el árbol de tests para encontrar archivos relacionados al resumen. Ejecutando dos lecturas en paralelo.

---

<sub>24s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\dto.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\dto

<details>
<summary>8 lines</summary>

```
ErrorResponse.java
ProjectRequest.java
ProjectResponse.java
ProjectSummaryResponse.java
TaskRequest.java
TaskResponse.java
TaskStatusUpdateRequest.java
auth
```

</details>


---

<sub>25s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\dto\ProjectSummaryResponse.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\dto\ProjectSummaryResponse.java

<details>
<summary>10 lines</summary>

```
package com.taskflow.dto;

import com.taskflow.model.TaskStatus;
import java.util.Map;

/**
 * DTO para el resumen de un proyecto.
 */
public record ProjectSummaryResponse(Long projectId, String projectName,
                                     int totalTasks, Map<TaskStatus, Integer> byStatus,
                                     int overdue) {
}
```

</details>


---

<sub>31s</sub>

### Copilot

Leyendo la carpeta de tests para detectar tests nuevos relacionados con "summary" y revisar si el diff tocó tests. Ejecutando lectura de src/test/java (listado).

---

<sub>31s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java

```
com
```


---

<sub>32s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow (lines 1-200)

```
integration
slice
unit
```


---

<sub>35s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\slice.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\slice (lines 1-200)

```
ProjectControllerTest.java
ProjectSummaryControllerTest.java
TaskControllerTest.java
TaskRepositoryTest.java
```


---

<sub>36s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\slice\ProjectSummaryControllerTest.java.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\slice\ProjectSummaryControllerTest.java (lines 1-400)

<details>
<summary>53 lines</summary>

```
package com.taskflow.slice;

import com.taskflow.controller.ProjectController;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Project;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getResumen_proyectoExistente_devuelve200yJSON() throws Exception {
        Project proyecto = new Project(2L, "App Móvil", "d", 2L, null);
        ProjectSummaryResponse resumen = new ProjectSummaryResponse(2L, "App Móvil", 4,
                Map.of(TaskStatus.TODO,1, TaskStatus.IN_PROGRESS,2, TaskStatus.DONE,1), 1);

        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(proyecto));
        when(projectService.resumenDe(proyecto)).thenReturn(resumen);

        mockMvc.perform(get("/projects/2/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(2))
                .andExpect(jsonPath("$.projectName").value("App Móvil"))
                .andExpect(jsonPath("$.totalTasks").value(4))
                .andExpect(jsonPath("$.byStatus.TODO").value(1))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.byStatus.DONE").value(1))
                .andExpect(jsonPath("$.overdue").value(1));
    }

    @Test
    void getResumen_proyectoInexistente_devuelve404() throws Exception {
        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/99/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
```

</details>


---

<sub>45s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\unit.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\unit (lines 1-200)

```
ProjectServiceTest.java
ReportServiceTest.java
TaskServiceTest.java
TaskValidationTest.java
```


---

<sub>47s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\unit\ProjectServiceTest.java.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\unit\ProjectServiceTest.java (lines 1-400)

<details>
<summary>58 lines</summary>

```
package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void resumen_conTareas_calculaContadores() throws Exception {
        Project proyecto = new Project(2L, "App Móvil", "d", 2L, null);
        Task t1 = new Task(1L, "T-1", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
        Task t2 = new Task(2L, "T-2", "d", TaskStatus.IN_PROGRESS, Priority.HIGH, 2L, 1L, LocalDate.now().minusDays(2));
        Task t3 = new Task(3L, "T-3", "d", TaskStatus.IN_PROGRESS, Priority.LOW, 2L, null, null);
        Task t4 = new Task(4L, "T-4", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now().minusDays(5));

        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1,t2,t3,t4));

        ProjectSummaryResponse resumen = projectService.resumenDe(proyecto);

        assertEquals(4, resumen.totalTasks());
        assertEquals(1, resumen.byStatus().get(TaskStatus.TODO));
        assertEquals(2, resumen.byStatus().get(TaskStatus.IN_PROGRESS));
        assertEquals(1, resumen.byStatus().get(TaskStatus.DONE));
        assertEquals(1, resumen.overdue()); // only t2 is overdue and not DONE
    }

    @Test
    void resumen_proyectoSinTareas_devuelveCeros() throws Exception {
        Project proyecto = new Project(3L, "Empty", "d", 1L, null);
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        ProjectSummaryResponse resumen = projectService.resumenDe(proyecto);

        assertEquals(0, resumen.totalTasks());
        assertEquals(0, resumen.byStatus().get(TaskStatus.TODO));
        assertEquals(0, resumen.byStatus().get(TaskStatus.IN_PROGRESS));
        assertEquals(0, resumen.byStatus().get(TaskStatus.DONE));
        assertEquals(0, resumen.overdue());
    }
}
```

</details>


---

<sub>1m 12s</sub>

### Copilot

Resumen breve: leída la diff semana6/proyecto-final.diff y la spec specs/summary.md; los cambios son menores y la implementación sigue la spec. Revisión:

| # | Gravedad | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/controller/ProjectController.java:134 (hunk) | 1 Alcance | El diff cambia la expresión @PreAuthorize de "#id" a "#p0" (ajuste de SPEL, no relacionado con /projects/{id}/summary). | Aceptable como corrección; preferible mover a commit independiente o justificar en la PR. |
| 2 | Sugerencia | src/main/java/com/taskflow/service/ProjectService.java:116 (hunk) | 3 Reglas del dominio | Se sustituyó la comprobación manual de dueDate por t.estaVencida(), reutilizando la regla del dominio. | Correcto — cumple la regla 1 de la spec. No hay cambios necesarios. |

Casos sin test:
- ninguno (existen unit tests en ProjectServiceTest y slice tests en ProjectSummaryControllerTest que cubren 200 y 404; además hay test para proyecto sin tareas).

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
