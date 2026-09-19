package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.dto.TaskResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusquedaTareasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void search_con_q_devuelve200ConListaYCampos() throws Exception {
        Task t1 = tarea(9L, "Documentar la API con Swagger", TaskStatus.TODO);
        Task t2 = tarea(5L, "Optimizar consultas de la API", TaskStatus.TODO);

        when(taskService.buscarPorTitulo("api")).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/tasks/search?q=api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].title").value("Documentar la API con Swagger"))
                .andExpect(jsonPath("$[1].id").value(5))
                .andExpect(jsonPath("$[1].title").value("Optimizar consultas de la API"));
    }

    @Test
    void search_sin_q_devuelve400() throws Exception {
        when(taskService.buscarPorTitulo(null)).thenThrow(new TaskValidationException("El parámetro 'q' es obligatorio."));

        mockMvc.perform(get("/tasks/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'q' es obligatorio."));
    }

    private Task tarea(Long id, String title, TaskStatus status) {
        try {
            return new Task(id, title, "desc", status, null, 1L, 1L, LocalDate.now().plusDays(1));
        } catch (com.taskflow.exception.TaskValidationException e) {
            throw new IllegalStateException(e);
        }
    }
}
