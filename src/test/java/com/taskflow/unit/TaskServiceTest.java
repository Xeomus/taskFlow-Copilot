package com.taskflow.unit;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskServiceTest — UNIT PURO con Mockito (S3D1, MP-1/MP-2/MP-3/MP-5). El renacimiento del parche
 * @SpringBootTest heredado de S2D4: aquí NO arranca Spring — la clase corre en MILISEGUNDOS.
 *
 * Por qué Mockito HOY (narrativa AM-1): en S2D1 este mismo test usaba el InMemoryTaskRepository REAL
 * (un *fake*, rápido y determinista — era legítimo). S2D4 eliminó las clases InMemory*; desde entonces
 * TaskRepository es una interfaz cuya única implementación es un proxy de Spring Data respaldado por una
 * BD. Para volver a probar el service EN AISLAMIENTO ya no hay implementación de juguete que instanciar
 * -> por eso existe Mockito: el mock SUSTITUYE al fake cuando el colaborador real exige infraestructura.
 *
 * Reglas del día aplicadas:
 *   - @ExtendWith(MockitoExtension.class): sin ella, @Mock queda null (dolor #1).
 *   - Los DATOS (Task) se construyen DE VERDAD con el constructor de rehidratación; SOLO el COLABORADOR
 *     (el repository) se mockea (tabla mockear/no-mockear, T3).
 *   - MockitoExtension corre en strict-stubs: un when(...) que ningún camino usa FALLA la clase
 *     (UnnecessaryStubbingException, dolor #2) -> aquí cada stub tiene propósito.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private com.taskflow.service.TaskService service;

    private static final Long PROYECTO = 1L;

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        void crear_valida_delegaEnSaveYDevuelveTareaConId() throws TaskValidationException {
            // El request pasa por TaskMapper.aEntidadNueva -> Task.crear (status=TODO, id=null). El
            // repositorio, mockeado, "devuelve" una tarea YA con id (lo que la BD real haría en save).
            TaskRequest request = new TaskRequest("Redactar informe", "desc", Priority.HIGH, 5L, null);
            Task tareaConId = new Task(99L, "Redactar informe", "desc",
                    TaskStatus.TODO, Priority.HIGH, PROYECTO, 5L, null);
            when(repository.save(any(Task.class))).thenReturn(tareaConId);

            Task creada = service.crear(request, PROYECTO);

            assertNotNull(creada.getId());
            assertEquals(99L, creada.getId());
        }

        @Test
        @DisplayName("thenThrow: el mock simula una BD caída — lo que el colaborador real no te da nunca")
        void crear_conBdCaida_propagaLaExcepcion() {
            // thenThrow: con la BD real NO puedes provocar una caída a demanda; el mock sí.
            TaskRequest request = new TaskRequest("Tarea válida", "desc", Priority.MED, 5L, null);
            when(repository.save(any(Task.class)))
                    .thenThrow(new RuntimeException("Conexión a la BD perdida"));

            assertThrows(RuntimeException.class, () -> service.crear(request, PROYECTO));
        }
    }

    @Nested
    @DisplayName("cambiarStatus")
    class CambiarStatus {

        @Test
        void cambiarStatus_conAssignee_guardaDone() {
            // El dato se construye REAL (constructor de rehidratación), con assignee -> SÍ puede pasar a
            // DONE. Solo el repository (colaborador) se mockea.
            Task tareaConAssignee = tarea(1L, "Con responsable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(tareaConAssignee));
            when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            service.cambiarStatus(1L, TaskStatus.DONE);

            // ArgumentCaptor (MP-3): capturar el Task que VIAJÓ a save. El retorno no basta cuando lo que
            // importa es el OBJETO MUTADO que se persistió.
            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(repository).save(captor.capture());
            assertEquals(TaskStatus.DONE, captor.getValue().getStatus());
        }

        @Test
        void cambiarStatus_idInexistente_lanzaTaskNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> service.cambiarStatus(999L, TaskStatus.DONE));
            // No se llegó a guardar nada.
            verify(repository, never()).save(any(Task.class));
        }

        @Test
        void cambiarStatus_sinAssignee_lanzaTaskStateExceptionYNoGuarda() {
            // Sin assignee, setStatus(DONE) lanza la checked; cambiarStatus la TRADUCE a TaskStateException.
            // La traducción de S2D3 MP-9, ahora probada EN AISLAMIENTO (sin HTTP, sin BD).
            Task sinAssignee = tarea(2L, "Sin responsable", null);
            when(repository.findById(2L)).thenReturn(Optional.of(sinAssignee));

            assertThrows(TaskStateException.class,
                    () -> service.cambiarStatus(2L, TaskStatus.DONE));
            // El never() es el assert más valioso del bloque: la regla PROHIBIÓ persistir.
            verify(repository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        void eliminar_existente_llamaDeleteById() {
            Task existente = tarea(1L, "Borrable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            // verify sobre una operación MUDA (void): el estado no lo delata, la interacción sí.
            verify(repository).deleteById(1L);
        }

        @Test
        void eliminar_inexistente_lanzaTaskNotFoundExceptionYNoBorra() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> service.eliminar(999L));
            // never() + anyLong(): NO se borró nada. (Regla "todos matchers o ninguno": aquí anyLong()).
            verify(repository, never()).deleteById(anyLong());
        }
    }

    /** Fabrica una Task de rehidratación REAL (dato, no mock). assigneeId null = sin responsable. */
    private Task tarea(Long id, String title, Long assigneeId) {
        try {
            return new Task(id, title, "desc", TaskStatus.TODO, Priority.MED, PROYECTO, assigneeId, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }

    @Nested
    @DisplayName("SinResponsable")
    class SinResponsable {

        @Test
        void sinResponsable_filtraYOrdenaPorFecha() {
            Task diez;
            Task conAssignee;
            Task sinFecha;
            Task dos;
            try {
                diez = new Task(201L, "Diez dias", "d", TaskStatus.TODO, Priority.MED, PROYECTO, null, LocalDate.now().plusDays(10));
                conAssignee = new Task(202L, "Con responsable", "d", TaskStatus.TODO, Priority.MED, PROYECTO, 5L, LocalDate.now().plusDays(20));
                sinFecha = new Task(203L, "Sin fecha", "d", TaskStatus.TODO, Priority.MED, PROYECTO, null, null);
                dos = new Task(204L, "Dos dias", "d", TaskStatus.TODO, Priority.MED, PROYECTO, null, LocalDate.now().plusDays(2));
            } catch (TaskValidationException e) {
                throw new IllegalStateException(e);
            }

            // El repositorio devuelve (en este orden): sin responsable con fecha en 10 días, con responsable,
            // sin responsable sin fecha, sin responsable con fecha en 2 días
            when(repository.findAll()).thenReturn(List.of(diez, conAssignee, sinFecha, dos));

            List<Task> res = service.sinResponsable();

            // Debe quedar solo las tres sin responsable, ordenadas por dueDate asc (2d, 10d, sin fecha)
            List<Long> ids = res.stream().map(Task::getId).toList();
            assertEquals(List.of(204L, 201L, 203L), ids);
        }

        @Test
        void sinResponsable_soloConAssignee_devuelveVacio() {
            Task t1 = tarea(1L, "Con1", 5L);
            Task t2 = tarea(2L, "Con2", 6L);
            when(repository.findAll()).thenReturn(List.of(t1, t2));

            List<Task> res = service.sinResponsable();

            assertEquals(0, res.size());
        }
    }

    @Nested
    @DisplayName("vencidas")
    class Vencidas {

        @Test
        void vencidas_filtraYOrdena() {
            // El repositorio devuelve, en este orden: una vencida hace 1 día, una con fecha en 3 días,
            // una DONE vencida hace 10 días, una sin dueDate, y una vencida hace 5 días.
            Task unaDia;
            Task tresDiasFuturo;
            Task doneDiezDias;
            Task sinFecha;
            Task cincoDias;
            try {
                unaDia = new Task(101L, "Hace 1 dia", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, LocalDate.now().minusDays(1));
                tresDiasFuturo = new Task(102L, "En 3 dias", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, LocalDate.now().plusDays(3));
                doneDiezDias = new Task(103L, "Done antiguo", "d", TaskStatus.DONE, Priority.HIGH, PROYECTO, 1L, LocalDate.now().minusDays(10));
                sinFecha = new Task(104L, "Sin fecha", "d", TaskStatus.IN_PROGRESS, Priority.HIGH, PROYECTO, 1L, null);
                cincoDias = new Task(105L, "Hace 5 dias", "d", TaskStatus.IN_PROGRESS, Priority.HIGH, PROYECTO, 1L, LocalDate.now().minusDays(5));
            } catch (TaskValidationException e) {
                throw new IllegalStateException(e);
            }

            when(repository.findAll()).thenReturn(List.of(unaDia, tresDiasFuturo, doneDiezDias, sinFecha, cincoDias));

            List<Task> res = service.vencidas();

            // Deben aparecer solo las dos vencidas (cincoDias y unaDia), y ordenadas por fecha asc: cincoDias (hace 5 días) primero
            assertEquals(2, res.size());
            assertEquals(105L, res.get(0).getId());
            assertEquals(101L, res.get(1).getId());
        }
    }
}
