package com.taskflow.unit;

import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaTareasServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService service;

    @Test
    void buscarPorTitulo_ordenaYTrimaLlamadoAlRepo() throws TaskValidationException {
        Task t1 = tarea(5L, "Optimizar consultas de la API", TaskStatus.TODO);
        Task t2 = tarea(9L, "Documentar la API con Swagger", TaskStatus.TODO);
        // repo devuelve en orden no alfabético
        when(taskRepository.findByTitleContainingIgnoreCase("api")).thenReturn(List.of(t1, t2));

        List<Task> resultado = service.buscarPorTitulo("  api ");

        // debe trim y llamar al repo con "api"
        verify(taskRepository).findByTitleContainingIgnoreCase("api");

        // resultado ordenado por título (Documentar... antes de Optimizar...)
        assertEquals(2, resultado.size());
        assertEquals(9L, resultado.get(0).getId());
        assertEquals(5L, resultado.get(1).getId());
    }

    @Test
    void buscarPorTitulo_null_o_vacio_lanza() {
        try {
            service.buscarPorTitulo(null);
        } catch (TaskValidationException e) {
            assertEquals("El parámetro 'q' es obligatorio.", e.getMessage());
        }

        try {
            service.buscarPorTitulo("   ");
        } catch (TaskValidationException e) {
            assertEquals("El parámetro 'q' es obligatorio.", e.getMessage());
        }
    }

    private Task tarea(Long id, String title, TaskStatus status) {
        try {
            return new Task(id, title, "desc", status, Priority.MED, 1L, 1L, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException(e);
        }
    }
}
