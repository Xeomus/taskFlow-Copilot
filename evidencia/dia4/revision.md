# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `e533daef-ddea-413c-ad69-c731b245863d`  
> - **Started:** 9/18/2026, 4:45:17 PM  
> - **Duration:** 1m 3s  
> - **Exported:** 9/18/2026, 4:46:21 PM  

---

<sub>2s</sub>

### User

Revisa evidencia/dia4/summary.diff contra la especificación specs/summary.md.

---

<sub>8s</sub>

### Copilot

Abriendo el diff y la especificación para identificar los archivos modificados y criterios a verificar; luego abriré los archivos afectados según el diff para revisar contexto. Ejecutando lectura de evidence/dia4/summary.diff y specs/summary.md.

---

<sub>8s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\evidencia\dia4\summary.diff.**

C:\Users\navae\Downloads\taskflow-IA\evidencia\dia4\summary.diff

<details>
<summary>234 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ProjectController.java b/src/main/java/com/taskflow/controller/ProjectController.java
index 56ccccf..00b6805 100644
--- a/src/main/java/com/taskflow/controller/ProjectController.java
+++ b/src/main/java/com/taskflow/controller/ProjectController.java
@@ -86,6 +86,15 @@ public class ProjectController {
         return tareas.stream().map(TaskMapper::aResponse).toList();
     }
 
+    @Operation(summary = "Resumen del proyecto",
+            description = "Devuelve un resumen con recuentos de tareas por estado y cuántas están vencidas. 404 si el proyecto no existe.")
+    @GetMapping("/projects/{id}/summary")
+    public com.taskflow.dto.ProjectSummaryResponse getProjectSummary(@PathVariable("id") Long id) {
+        Project proyecto = projectService.buscarPorId(id)
+                .orElseThrow(() -> new ProjectNotFoundException(id));
+        return projectService.resumenDe(proyecto);
+    }
+
     /**
      * POST /projects — 201 + Location a /projects/{id}. @Valid dispara Bean Validation (400 si falla).
      * MP-9: el owner sale del JWT — el Authentication (inyectado por Spring Security) trae el username
diff --git a/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
new file mode 100644
index 0000000..0006acc
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
@@ -0,0 +1,12 @@
+package com.taskflow.dto;
+
+import com.taskflow.model.TaskStatus;
+import java.util.Map;
+
+/**
+ * DTO para el resumen de un proyecto.
+ */
+public record ProjectSummaryResponse(Long projectId, String projectName,
+                                     int totalTasks, Map<TaskStatus, Integer> byStatus,
+                                     int overdue) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index a33a95b..9a71f72 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -22,4 +22,12 @@ public final class ProjectMapper {
         return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                 p.getOwnerId(), p.getCreatedAt());
     }
+
+    /**
+     * Crea un ProjectSummaryResponse a partir de la entidad Project y los recuentos calculados.
+     */
+    public static com.taskflow.dto.ProjectSummaryResponse aSummary(Project p,
+            int totalTasks, java.util.Map<com.taskflow.model.TaskStatus, Integer> byStatus, int overdue) {
+        return new com.taskflow.dto.ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index d315e3f..8bfc782 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -101,4 +101,25 @@ public class ProjectService {
         tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
         projectRepository.deleteById(id);
     }
+
+    /**
+     * Resumen del proyecto: cuenta tareas por estado y cuántas están vencidas.
+     * Reusa Task.estaVencida().
+     */
+    public com.taskflow.dto.ProjectSummaryResponse resumenDe(Project proyecto) {
+        List<Task> tareas = tareasDe(proyecto.getId());
+        int total = tareas.size();
+        java.util.Map<com.taskflow.model.TaskStatus, Integer> byStatus = new java.util.EnumMap<>(com.taskflow.model.TaskStatus.class);
+        for (com.taskflow.model.TaskStatus s : com.taskflow.model.TaskStatus.values()) {
+            byStatus.put(s, 0);
+        }
+        int overdue = 0;
+        for (Task t : tareas) {
+            byStatus.put(t.getStatus(), byStatus.get(t.getStatus()) + 1);
+            if (t.estaVencida()) {
+                overdue++;
+            }
+        }
+        return new com.taskflow.dto.ProjectSummaryResponse(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java b/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java
new file mode 100644
index 0000000..51d8eba
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java
@@ -0,0 +1,65 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ProjectController;
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.Map;
+import java.util.Optional;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(ProjectController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ProjectSummaryControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getResumen_proyectoExistente_devuelve200yJSON() throws Exception {
+        Project proyecto = new Project(2L, "App Móvil", "d", 2L, null);
+        ProjectSummaryResponse resumen = new ProjectSummaryResponse(2L, "App Móvil", 4,
+                Map.of(TaskStatus.TODO,1, TaskStatus.IN_PROGRESS,2, TaskStatus.DONE,1), 1);
+
+        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(proyecto));
+        when(projectService.resumenDe(proyecto)).thenReturn(resumen);
+
+        mockMvc.perform(get("/projects/2/summary"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.projectId").value(2))
+                .andExpect(jsonPath("$.projectName").value("App Móvil"))
+                .andExpect(jsonPath("$.totalTasks").value(4))
+                .andExpect(jsonPath("$.byStatus.TODO").value(1))
+                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
+                .andExpect(jsonPath("$.byStatus.DONE").value(1))
+                .andExpect(jsonPath("$.overdue").value(1));
+    }
+
+    @Test
+    void getResumen_proyectoInexistente_devuelve404() throws Exception {
+        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
+
+        mockMvc.perform(get("/projects/99/summary"))
+                .andExpect(status().isNotFound())
+                .andExpect(jsonPath("$.status").value(404));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProjectServiceTest.java b/src/test/java/com/taskflow/unit/ProjectServiceTest.java
new file mode 100644
index 0000000..2c77a84
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProjectServiceTest.java
@@ -0,0 +1,73 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Optional;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ProjectServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService projectService;
+
+    @Test
+    void resumen_conTareas_calculaContadores() throws Exception {
+        Project proyecto = new Project(2L, "App Móvil", "d", 2L, null);
+        Task t1 = new Task(1L, "T-1", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
+        Task t2 = new Task(2L, "T-2", "d", TaskStatus.IN_PROGRESS, Priority.HIGH, 2L, 1L, LocalDate.now().minusDays(2));
+        Task t3 = new Task(3L, "T-3", "d", TaskStatus.IN_PROGRESS, Priority.LOW, 2L, null, null);
+        Task t4 = new Task(4L, "T-4", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now().minusDays(5));
+
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1,t2,t3,t4));
+
+        ProjectSummaryResponse resumen = projectService.resumenDe(proyecto);
+
+        assertEquals(4, resumen.totalTasks());
+        assertEquals(1, resumen.byStatus().get(TaskStatus.TODO));
+        assertEquals(2, resumen.byStatus().get(TaskStatus.IN_PROGRESS));
+        assertEquals(1, resumen.byStatus().get(TaskStatus.DONE));
+        assertEquals(1, resumen.overdue()); // only t2 is overdue and not DONE
+    }
+
+    @Test
+    void resumen_proyectoSinTareas_devuelveCeros() throws Exception {
+        Project proyecto = new Project(3L, "Empty", "d", 1L, null);
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        ProjectSummaryResponse resumen = projectService.resumenDe(proyecto);
+
+        assertEquals(0, resumen.totalTasks());
+        assertEquals(0, resumen.byStatus().get(TaskStatus.TODO));
+        assertEquals(0, resumen.byStatus().get(TaskStatus.IN_PROGRESS));
+        assertEquals(0, resumen.byStatus().get(TaskStatus.DONE));
+        assertEquals(0, resumen.overdue());
+    }
+}
```

</details>


---

<sub>8s</sub>

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

<sub>16s</sub>

### Copilot

Leyendo los archivos relevantes para comparar contexto: ProjectController, ProjectService, ProjectMapper, Task, ProjectNotFoundException y GlobalExceptionHandler. Ejecutando varias lecturas en paralelo.

---

<sub>16s</sub>

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
    @PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

</details>


---

<sub>16s</sub>

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

<sub>16s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\mapper\ProjectMapper.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\mapper\ProjectMapper.java

<details>
<summary>28 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.model.Project;

/**
 * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
 *
 * HOY se SIMPLIFICÓ (lo prometía D3): la entidad ya guarda 'ownerId' directo (se aplanó el 'User
 * owner' en MP-4), así que aResponse ya no deriva el id desde un objeto (p.getOwner().id()) — lee
 * p.getOwnerId() tal cual. El contrato de salida (ProjectResponse con ownerId Long) no cambió; el
 * mapeo se volvió trivial porque el dominio por fin coincide con la forma canónica.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. Ahora ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /**
     * Crea un ProjectSummaryResponse a partir de la entidad Project y los recuentos calculados.
     */
    public static com.taskflow.dto.ProjectSummaryResponse aSummary(Project p,
            int totalTasks, java.util.Map<com.taskflow.model.TaskStatus, Integer> byStatus, int overdue) {
        return new com.taskflow.dto.ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
    }
}
```

</details>


---

<sub>16s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\model\Task.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\model\Task.java

<details>
<summary>225 lines</summary>

```
package com.taskflow.model;

import com.taskflow.exception.TaskValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task — entidad central del dominio TaskFlow. ESTADO FINAL del Día 4 (persistida con JPA).
 *
 * La entidad ES la clase que YA teníamos (canónica S1, 8 campos): NO se escribió un "modelo nuevo",
 * se ANOTÓ el Task de D3. Novedades de HOY, todas por anotación:
 *   - @Entity + @Table(name = "tasks"): esta clase mapea a la tabla TASKS.
 *   - @Id + @GeneratedValue(IDENTITY): el id lo asigna la BD (columna autoincremental). La secuencia
 *     manual del InMemoryTaskRepository (el Math.max de S1D3) MURIÓ: el id lo pone la BD y nadie más.
 *   - @Enumerated(STRING) en status y priority: se guardan como TEXTO ('TODO'), NUNCA como ORDINAL
 *     (0,1,2): reordenar o insertar un enum corrompería datos históricos. El orden LOW,MED,HIGH de
 *     S1D3 es contrato del SORT (compareTo), no del almacenamiento.
 *   - relación @ManyToOne Task->Project (patrón "asociación de solo lectura"): la COLUMNA la sigue
 *     mandando el escalar 'projectId' (por eso Task.crear, TaskService, mappers y DTOs NO cambian);
 *     el objeto 'project' solo NAVEGA. Frase ancla: "la columna manda; el objeto navega".
 *
 * Tensión JPA vs validaciones (paga de S1D2 MP-8, fijada por el apéndice del CAPSTONE): JPA rehidrata
 * por reflexión con un constructor sin args protegido, SIN pasar por las validaciones. Está bien:
 * Task.crear(...) sigue siendo el ÚNICO camino de CREACIÓN de negocio; JPA solo REHIDRATA datos que
 * ya existían (la misma distinción crear/rehidratar de siempre).
 *
 * OJO: las columnas persistentes NO pueden ser 'final' (JPA las escribe por reflexión tras el no-arg);
 * por eso title/description/projectId/dueDate perdieron el 'final' que traían de D3. La inmutabilidad
 * de negocio la sigue garantizando la ausencia de setters públicos para esos campos.
 */
@Entity
@Table(name = "tasks")
public class Task implements Comparable<Task> {

    // Reglas de longitud del título (del capstone).
    private static final int TITULO_MIN = 3;
    private static final int TITULO_MAX = 120;

    // id nace null; lo asigna la BD (IDENTITY). Ver setId (uso interno de JPA/repo).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    private String description;                 // por convención -> columna DESCRIPTION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;                  // texto 'TODO'/'IN_PROGRESS'/'DONE' (nunca 0/1/2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;                  // texto 'LOW'/'MED'/'HIGH'

    // La COLUMNA project_id la posee este escalar (insertable/updatable por default). Sigue siendo
    // el dueño: Task.crear, el mapper y el servicio hablan con projectId, no con el objeto.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // assigneeId se queda Long a secas (juicio explícito): es nullable, NUNCA navegamos al User
    // completo al leer tareas, y las derived queries operan igual. No toda FK necesita ser objeto:
    // mapea objeto cuando NAVEGAS, deja el id cuando solo REFERENCIAS.
    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDate dueDate;                  // por convención -> columna DUE_DATE (camelCase->snake_case)

    /**
     * Asociación de SOLO LECTURA hacia el proyecto (MP-8). Comparte la MISMA columna project_id con
     * el escalar de arriba, pero marcada insertable=false/updatable=false: no escribe la columna (el
     * escalar lo hace), solo permite NAVEGAR (task.getProject().getName()). LAZY: no se carga hasta
     * que se toca — y fuera de una sesión abierta, tocarla lanza LazyInitializationException (por eso
     * la API responde con DTOs, nunca con la entidad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * Constructor NO-ARG protegido: EXCLUSIVO de JPA (rehidratación por reflexión). Nunca es un camino
     * de creación de negocio. Su existencia es obligatoria para cualquier @Entity: sin él, al arrancar
     * Hibernate lanza "No default constructor for entity: com.taskflow.model.Task".
     */
    protected Task() {
        // solo para JPA: rehidratación, nunca creación de negocio
    }

    /**
     * Constructor de REHIDRATACIÓN: reconstruye una tarea que YA existía (semilla del DataSeeder,
     * datos releídos). Valida las INVARIANTES que siempre deben cumplirse (título y proyecto), pero
     * NO la regla temporal de dueDate: una tarea vencida es un dato válido que hay que poder releer.
     */
    public Task(Long id, String title, String description, TaskStatus status,
                Priority priority, Long projectId, Long assigneeId, LocalDate dueDate)
            throws TaskValidationException {
        // Invariante 1: título obligatorio, 3-120 chars.
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("El título es obligatorio (no puede ir vacío).");
        }
        if (title.length() < TITULO_MIN || title.length() > TITULO_MAX) {
            throw new TaskValidationException(
                    "El título debe tener entre " + TITULO_MIN + " y " + TITULO_MAX
                            + " caracteres; recibí " + title.length() + ": \"" + title + "\".");
        }
        // Invariante 2: una tarea no puede existir sin proyecto (regla del capstone).
        if (projectId == null) {
            throw new TaskValidationException(
                    "Una tarea no puede existir sin proyecto (projectId == null).");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }

    /**
     * Factory de CREACIÓN de negocio: añade la regla temporal (dueDate no en el pasado) y delega en
     * el constructor. id nace null, status nace TODO. Firma canónica S1 (apéndice del CAPSTONE-SPEC):
     * la usan el mapper y el servicio tal cual — por eso NO cambia hoy pese a que Task ya sea @Entity.
     */
    public static Task crear(String title, String description, Priority priority,
                             LocalDate dueDate, Long projectId, Long assigneeId)
            throws TaskValidationException {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new TaskValidationException(
                    "La fecha límite no puede estar en el pasado: " + dueDate + ".");
        }
        return new Task(null, title, description, TaskStatus.TODO, priority, projectId, assigneeId, dueDate);
    }

    // ---- Comportamiento: vive CON los datos ----

    /** true si tiene fecha, ya pasó, y la tarea aún no está terminada. */
    public boolean estaVencida() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status != TaskStatus.DONE;
    }

    // ---- Getters ----

    public Long getId() {
        return id;
    }

    /** Uso interno (JPA/repo) para el id autoincremental. Nadie más lo llama a mano. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Navega al proyecto asociado (asociación de solo lectura). Fuera de una sesión JPA abierta puede
     * lanzar LazyInitializationException (por eso la API expone DTOs, no la entidad). En el runtime
     * nadie del camino principal lo usa; existe para NAVEGAR cuando haga falta.
     */
    public Project getProject() {
        return project;
    }

    // ---- Setters SOLO donde el dominio muta ----

    /**
     * Cambia el estado aplicando la regla del capstone: no se puede pasar a DONE una tarea sin
     * responsable (assigneeId). La regla vive AQUÍ, no en el servicio ni en el menú.
     */
    public void setStatus(TaskStatus status) throws TaskValidationException {
        if (status == TaskStatus.DONE && assigneeId == null) {
            throw new TaskValidationException(
                    "No se puede marcar como " + TaskStatus.DONE
                            + " una tarea sin responsable: \"" + title + "\".");
        }
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    // ---- Identidad de ENTIDAD por id (S1D3 MP-6 = literalmente lo que hace JPA) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task otra = (Task) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ---- Orden natural por prioridad (Comparable) ----

    @Override
    public int compareTo(Task otra) {
        return this.priority.compareTo(otra.priority);
    }

    @Override
    public String toString() {
        return "Task{id=" + id
                + ", title='" + title + '\''
                + ", status=" + status
                + ", priority=" + priority
                + ", assigneeId=" + (assigneeId == null ? "sin asignar" : assigneeId)
                + ", dueDate=" + (dueDate == null ? "sin fecha" : dueDate)
                + (estaVencida() ? " *VENCIDA*" : "")
                + '}';
    }
}
```

</details>


---

<sub>16s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\exception\ProjectNotFoundException.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow\exception\ProjectNotFoundException.java

<details>
<summary>21 lines</summary>

```
package com.taskflow.exception;

/**
 * ProjectNotFoundException — se lanza cuando se busca un proyecto por id y NO existe. Nace HOY
 * (S2D3, integrador paso 4). El GlobalExceptionHandler la mapea a 404, en el MISMO handler que
 * TaskNotFoundException (un solo @ExceptionHandler con dos tipos).
 *
 * Gemela de TaskNotFoundException (S1D4): UNCHECKED (extiende RuntimeException), guarda el id que no
 * se encontró y sube sola hasta el advice. La usan ProjectService (GET/PUT/DELETE de un proyecto
 * inexistente) y el cableo Task↔Project (POST /projects/{id}/tasks contra un proyecto que no existe).
 */
public class ProjectNotFoundException extends RuntimeException {

    private final Long id;

    public ProjectNotFoundException(Long id) {
        super("No existe proyecto con id " + id + ".");
        this.id = id;
    }

    /** El id que se buscó y no existía. */
    public Long getId() {
        return id;
    }
}
```

</details>


---

<sub>16s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow/advice/GlobalExceptionHandler.java.**

C:\Users\navae\Downloads\taskflow-IA\src\main\java\com\taskflow/advice/GlobalExceptionHandler.java

<details>
<summary>116 lines</summary>

```
package com.taskflow.advice;

import com.taskflow.dto.ErrorResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.exception.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * GlobalExceptionHandler — el INTERCEPTOR GLOBAL de excepciones de la API (PM, MP-7). Centraliza en
 * un solo lugar lo que antes estaba disperso: el 404 a mano en dos controllers, el 400 default feo
 * de Boot, y la TaskValidationException dando 500.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody: cada método devuelve el cuerpo JSON
 * directo. Cada @ExceptionHandler ataja UN tipo (o una lista de tipos) y lo traduce al ErrorResponse
 * uniforme con su status. Spring elige el handler MÁS ESPECÍFICO declarado.
 *
 * Mapa de decisión de status (T4):
 *   400 = el request está mal construido/formato   (Bean Validation, JSON malformado, tipo ilegible)
 *   404 = el recurso no existe
 *   422 = el request es impecable, pero el ESTADO del negocio dice "no" (regla de estado)
 *
 * Lección "el advice tragón" (error intencional #5): NO declaramos un handler de Exception.class
 * "por si acaso" — se tragaría los 400/404 que Spring ya lanza (HttpMessageNotReadableException,
 * NoResourceFoundException) y los volvería 500. Regla: maneja lo que CONOCES; deja que Spring
 * responda su default para lo que no. Si algún día declaras el genérico, declara TAMBIÉN explícitas
 * las de Spring que te importan (como estas de abajo) y re-prueba los casos de error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — recurso inexistente. Un solo handler para las dos excepciones de "no encontrado". */
    @ExceptionHandler({TaskNotFoundException.class, ProjectNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    /**
     * 400 — reglas de dominio violadas al CREAR/EDITAR (título fuera de rango, projectId nulo,
     * dueDate en el pasado al crear). Es la checked TaskValidationException de S1 subiendo desde
     * Task.crear/constructor: el pendiente del pizarrón ("la fecha pasada debe dar 400") se paga aquí.
     */
    @ExceptionHandler(TaskValidationException.class)
    public ResponseEntity<ErrorResponse> handleTaskValidation(TaskValidationException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    /**
     * 400 con DETALLE POR CAMPO — falló @Valid en un @RequestBody (Bean Validation en la frontera).
     * getFieldErrors() + stream map a "campo: mensaje" (streams de S1D4): el cuerpo default de Boot,
     * feo y sin detalle, se vuelve una lista accionable de qué campo falló y por qué.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "La validación de la petición falló.", errores);
    }

    /**
     * 400 — JSON malformado o valor ilegible (una coma de más, un enum que no matchea como
     * {"status":"FINISHED"}). Ocurre en la DESERIALIZACIÓN, ANTES de @Valid: por eso el parseo NUNCA
     * dispara la validación (parseo != validación). Handler EXPLÍCITO de esta excepción DE Spring.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "JSON malformado o valor ilegible.", List.of());
    }

    /**
     * 422 — el request es sintácticamente impecable, pero el ESTADO del dominio no permite la
     * operación ("no DONE sin assignee"). TaskService.cambiarStatus tradujo la TaskValidationException
     * de setStatus a esta TaskStateException. Es la distinción canónica del capstone frente al 400.
     */
    @ExceptionHandler(TaskStateException.class)
    public ResponseEntity<ErrorResponse> handleState(TaskStateException ex) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    /**
     * 409 — username duplicado al registrar (MP-5). El request es válido, pero choca con el estado
     * actual del recurso (username único). AuthService.register lanza UsernameAlreadyExistsException.
     */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameExists(UsernameAlreadyExistsException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    /**
     * 401 — credenciales inválidas en el login (MP-5). El AuthenticationManager lanza
     * BadCredentialsException DENTRO del controller /auth/login (endpoint público), así que SÍ llega
     * al advice (contraste con las excepciones del filtro JWT, que corren antes del DispatcherServlet
     * y este advice NO ve). No se filtra si falló el usuario o el password: mensaje genérico a propósito.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.", List.of());
    }

    /**
     * STRETCH — 400 para un path variable de tipo ilegible: GET /tasks/abc (id no numérico) daba un
     * 400 default sin cuerpo uniforme; aquí lo unificamos al ErrorResponse.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor ilegible.", List.of());
    }

    /** Fábrica del cuerpo uniforme: mismo formato para todos los status. */
    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String message, List<String> errors) {
        ErrorResponse cuerpo = new ErrorResponse(Instant.now(), status.value(), message, errors);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
```

</details>


---

<sub>33s</sub>

### `view`

**view the file at C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\slice\TaskControllerTest.java.**

C:\Users\navae\Downloads\taskflow-IA\src\test\java\com\taskflow\slice\TaskControllerTest.java

<details>
<summary>200 lines</summary>

```
package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TaskControllerTest — SLICE web (S3D1, MP-6). La promesa de S2D3 pagada: los @SpringBootTest lentos
 * del CRUD regresan a @WebMvcTest. Aquí NO se levanta JPA, ni el seeder, ni la seguridad real: solo la
 * capa web (controller + advice + Jackson + validación). El service es un @MockitoBean.
 *
 * Las 3 anotaciones CANÓNICAS de la migración (bloque de T6/MP-6):
 *   1. @WebMvcTest(TaskController.class) -> levanta SOLO la capa web de este controller.
 *   2. @MockitoBean del/los service(s) -> el colaborador ausente; el músculo de S2D2 (when(...)) vuelve.
 *   3. @AutoConfigureMockMvc(addFilters = false) -> la seguridad NO se prueba en el slice: se prueba
 *      donde vive, en integration/. Sin esto, el slice aplica la seguridad default de Boot y TODO da 401.
 *
 * Tropiezo canónico (a): el slice ESCANEA los Filter @Component, y JwtAuthenticationFilter arrastra
 * JwtService/JpaUserDetailsService al contexto -> "No qualifying bean of type 'JwtService'". Solución:
 * @MockitoBean JwtAuthenticationFilter (se mockea el filtro para que el slice no cargue sus dependencias).
 *
 * @Mock vs @MockitoBean (dolor #1): aquí HAY contexto Spring -> @MockitoBean (sustituye un bean). En
 * unit/ NO hay Spring -> @Mock. Regla mecánica: ¿la clase tiene una anotación @...Test de Spring? -> bean.
 *
 * El token DESAPARECIÓ del test: ya no hay filtro que lo exija (addFilters = false).
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;   // TaskController también inyecta ProjectService (cableo Task<->Project)

    // Sin esto: el slice intenta construir el Filter @Component real -> No qualifying bean 'JwtService'.
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ==================== GET (contra el stub del service, no una semilla) ====================

    @Test
    void getTasks_sinFiltro_retorna200YLista() throws Exception {
        when(taskService.listar()).thenReturn(List.of(
                tarea(1L, "Primera", TaskStatus.TODO),
                tarea(2L, "Segunda", TaskStatus.IN_PROGRESS)));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Primera"));
    }

    @Test
    void getTaskPorId_existente_retorna200ConElTitulo() throws Exception {
        when(taskService.buscarPorId(1L)).thenReturn(Optional.of(tarea(1L, "Diseñar esquema de BD", TaskStatus.TODO)));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Diseñar esquema de BD"))
                .andExpect(jsonPath("$.projectId").value(1));
    }

    @Test
    void getOverdue_retorna200YUnaTarea() throws Exception {
        Task vencida;
        try {
            vencida = new Task(7L, "Corregir bug de fechas", "desc", TaskStatus.IN_PROGRESS, Priority.MED, 1L, 1L, java.time.LocalDate.now().minusDays(1));
        } catch (com.taskflow.exception.TaskValidationException e) {
            throw new IllegalStateException(e);
        }
        when(taskService.vencidas()).thenReturn(List.of(vencida));

        mockMvc.perform(get("/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].title").value("Corregir bug de fechas"));
    }

    @Test
    void getUnassigned_retorna200YAssigneeNull() throws Exception {
                Task t4;
                Task t6;
                try {
                    t4 = new Task(4L, "Escribir tests MockMvc", "desc", TaskStatus.TODO, Priority.MED, 1L, null, java.time.LocalDate.now().plusDays(7));
                    t6 = new Task(6L, "Publicar en la tienda", "desc", TaskStatus.TODO, Priority.MED, 1L, null, java.time.LocalDate.now().plusDays(10));
                } catch (com.taskflow.exception.TaskValidationException e) {
                    throw new IllegalStateException(e);
                }
                when(taskService.sinResponsable()).thenReturn(List.of(t4, t6));

                mockMvc.perform(get("/tasks/unassigned"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2))
                        .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(4, 6)))
                        .andExpect(jsonPath("$[0].assigneeId").value(nullValue()))
                        .andExpect(jsonPath("$[1].assigneeId").value(nullValue()));
    }


    @Test
    void getTaskPorId_inexistente_retorna404() throws Exception {
        // El service dice "no está" (Optional vacío); el controller lanza -> el advice (SÍ vive en el slice) responde 404.
        when(taskService.buscarPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ==================== Escritura (la validación y el advice SÍ viven en el slice) ====================

    @Test
    void postTask_valida_devuelve201ConLocation() throws Exception {
        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "P", "d", 1L, null)));
        when(taskService.crear(any(), eq(1L)))
                .thenReturn(tareaCon(10L, "Redactar el informe trimestral", TaskStatus.TODO, 1L));

        String body = """
                {
                  "title": "Redactar el informe trimestral",
                  "description": "Con las métricas del quarter",
                  "priority": "HIGH",
                  "assigneeId": 1,
                  "dueDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/tasks/\\d+")))
                .andExpect(jsonPath("$.title").value("Redactar el informe trimestral"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    /**
     * Frontera de la API, ESPEJO del parametrizado de dominio (integrador, refactor b): títulos que
     * violan @NotBlank/@Size -> 400. La validación de forma vive en el slice; ni siquiera se llama al
     * service. 0 -> vacío; 1,2 -> cortos; 121 -> largo.
     */
    @ParameterizedTest(name = "título de longitud {0} → 400")
    @ValueSource(ints = {0, 1, 2, 121})
    void postTask_tituloInvalido_devuelve400(int longitud) throws Exception {
        String titulo = "a".repeat(longitud);
        String body = """
                { "title": "%s", "priority": "LOW" }
                """.formatted(titulo);

        mockMvc.perform(post("/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchStatus_sinAssignee_devuelve422() throws Exception {
        // El service (mockeado) traduce la regla a TaskStateException; el advice la mapea a 422.
        when(taskService.cambiarStatus(6L, TaskStatus.DONE))
                .thenThrow(new TaskStateException("No se puede marcar DONE sin responsable."));

        mockMvc.perform(patch("/tasks/6/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"DONE\" }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void deleteTask_inexistente_devuelve404() throws Exception {
        // eliminar es void -> doThrow(...).when(...) para simular el "no existe".
        doThrow(new TaskNotFoundException(999L)).when(taskService).eliminar(999L);

        mockMvc.perform(delete("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---- helpers de datos (reales, no mocks) ----

    private Task tarea(Long id, String title, TaskStatus status) {
        return tareaCon(id, title, status, 1L);
    }

    private Task tareaCon(Long id, String title, TaskStatus status, Long projectId) {
        try {
            return new Task(id, title, "desc", status, Priority.MED, projectId, 1L, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }
}
```

</details>


---

<sub>1m 3s</sub>

### Copilot

Resumen de la revisión contra specs/summary.md: se encontraron solo asuntos menores. Tabla de hallazgos:

| # | Gravedad | Archivo:línea | Punto checklist | Qué pasa | Qué haría |
|---|---|---:|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/controller/ProjectController.java:86 | 4,5 (Contrato / Seguridad) | Método GET /projects/{id}/summary devuelve el DTO y usa ProjectNotFoundException → 404; no hay anotación de seguridad en el método (pero la seguridad global aplica). | OK funcional; dejar así. (Opcional: importar el DTO en la firma en vez de usar el nombre fully-qualified para consistencia.) |
| 2 | Sugerencia | src/main/java/com/taskflow/service/ProjectService.java:101 | 3,5 (Reglas del dominio / Convenciones) | resumenDe() reutiliza Task.estaVencida() y construye el EnumMap con las 3 claves → cumple la regla. | OK; cumple spec. (Sugerencia: delegar la creación del DTO al mapper para seguir la convención "mapper manual".) |
| 3 | Sugerencia | src/main/java/com/taskflow/mapper/ProjectMapper.java:22 | 5 (Convenciones) | Existe ProjectMapper.aSummary pero el servicio no lo usa (el servicio crea el DTO directamente). | Usar ProjectMapper.aSummary desde ProjectService.resumenDe o eliminar el método si no se usará, para evitar código duplicado/inconsistencia. |
| 4 | Sugerencia | src/main/java/com/taskflow/dto/ProjectSummaryResponse.java:1 | 4 (Contrato) | DTO como record con Map\<TaskStatus,Integer> — serializa las claves ENUM como strings; tests JSON usan esas claves. | OK; válido. (Nada que cambiar.) |
| 5 | Sugerencia | src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java:1 | 6 (Tests) | Slice test y Unit test añadidos cubren los casos requeridos (existente→200 JSON; inexistente→404). | OK; bien escritos. |

Casos sin test:
- Ninguno: la especificación pedía unidad (proyecto con tareas y sin tareas) y slice (200 y 404); ambos están presentes.

Veredicto: APROBADO

Notas finales (breves):
- Cumple la especificación funcional (campos, conteo, uso de Task.estaVencida(), 404 para proyecto inexistente).
- Recomiendo aplicar la pequeña limpieza sugerida (usar/eliminar ProjectMapper.aSummary y tidy en la firma del controller) antes de merge para mantener consistencia con las convenciones del repo.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
