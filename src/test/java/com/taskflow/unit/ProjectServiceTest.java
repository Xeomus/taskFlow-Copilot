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
