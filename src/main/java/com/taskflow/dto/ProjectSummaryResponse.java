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
