# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** Xeomus · **Usuario de GitHub:** Xeomus

## 1. Qué construí

> Marca **una** fila con `x` y borra las otras dos.

|     | Feature                                           | Especificación                          |
| --- | ------------------------------------------------- | --------------------------------------- |
| [x] | `GET /tasks/search?q=` — buscar tareas por título | [`specs/search.md`](../specs/search.md) |

## 2. El pull request

- **URL del PR (mergeado):** https://github.com/Xeomus/taskFlow-Copilot/pull/6
- **Commit del merge en `main`:** 2c5f51d Merge pull request #6 from Xeomus/fix/search-endpoint

## 3. Cómo lo hice

| Paso              | Qué hice                                                                | Evidencia                                                                                                                      |
| ----------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Rama y spec       | `git switch -c feature/search` y copié la spec a `specs/`               | `git log --oneline main..feature/search` (antes del merge)                                                                     |
| Implementación    | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini`              | `semana6/sesion-implementacion.md` (incluye el registro de la sesión con la skill cargada)                                     |
| Revisión          | agente `revisor` sobre `semana6/proyecto-final.diff`                    | `semana6/revision.md` / `semana6/correccion-revision.md`                                                                       |
| Tests             | `mvn test` en verde                                                     | Tests totales: Tests run: 72, Failures: 0, Errors: 0, Skipped: 0 (`evidencia/dia2/suite-main.txt` / `target/surefire-reports`) |
| Comprobación REST | `pwsh -NoProfile -File .github/skills/verificar-taskflow/verificar.ps1` | salida completa en sección 5 (archivo `evidencia/dia4/verificar.txt`)                                                          |
| Code review       | Copilot en el PR                                                        | cambios aplicados: `src/main/java/com/taskflow/controller/TaskController.java` (commit 46110ef)                                |

## 4. Qué hizo el agente y qué corregí yo

| #   | Qué hizo mal el agente (archivo)                                                         | Quién lo detectó           | Cómo quedó corregido                                                                           |
| --- | ---------------------------------------------------------------------------------------- | -------------------------- | ---------------------------------------------------------------------------------------------- |
| 1   | Ajustes de seguridad/autoridad en controllers (p. ej. @PreAuthorize y reglas de negocio) | Revisor (agente `revisor`) | Aplicado: se ajustó @PreAuthorize y se verificó el uso de Task.estaVencida(); commit `b2c3dcc` |
| 2   | Documentación y firma del endpoint de búsqueda (Javadoc / validación q)                  | Copilot code review        | Aplicado: se actualizó `TaskController.searchTasks` y la javadoc; commit `46110ef`             |

Lo que el agente hizo bien a la primera: la implementación base del endpoint de búsqueda en `TaskService` y los mappers `TaskMapper` no requirieron cambios manuales.

## 5. Comprobaciones REST

Salida completa de la ejecución registrada (archivo `evidencia/dia4/verificar.txt`):

```text
Repositorio: C:\Users\navae\Downloads\taskflow-IA
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 27728). Esperando a que /info responda...
App lista en 8 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[FALLA] GET /projects/1/summary
        esperaba: HTTP 200 projectId=1 totalTasks=5 TODO=3 IN_PROGRESS=1 DONE=1 overdue=0 projectNameOk=True
        obtuve:   HTTP 200 projectId=1 totalTasks=5 TODO=3 IN_PROGRESS=1 DONE=1 overdue=1 projectNameOk=True
[FALLA] GET /projects/2/summary
        esperaba: HTTP 200 projectId=2 totalTasks=4 TODO=1 IN_PROGRESS=2 DONE=1 overdue=1 projectNameOk=True
        obtuve:   HTTP 200 projectId=2 totalTasks=4 TODO=1 IN_PROGRESS=2 DONE=1 overdue=2 projectNameOk=True
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
App detenida (PID 27728).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 2 de 8 con FALLA. Logs de la app: C:\Users\navae\Downloads\taskflow-IA\target\verificar-8080-app.log
```

## 6. Créditos de la semana

> Notas: los "AI credits" exactos salen del panel de facturación de GitHub (https://github.com/settings/billing) o de la salida del comando `copilot -p` al final de cada sesión (línea `AI Credits: ...`). Se han extraído los valores disponibles en la carpeta evidencia; el resto queda como N/D si no hay registro.

| Qué                                                                                        |                                        AI credits (extraído/observado) |
| ------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------: |
| Usados en septiembre según github.com (incluye semanas anteriores si usaste Copilot antes) |                                                             224.03 AIC |
| Implementación con la skill (`AI Credits` del PF-2)                                        |                  101 AIC (evidencia/dia2/usage.txt: "101 / 1,500 AIC") |
| Revisión del `revisor` (`AI Credits` del PF-3)                                             | 0.00 AIC (evidencia/dia1/usage.txt: "AI Credits del integrador: 0.00") |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`)                                   |                                                                    N/D |

Notas finales (evidencias y commits relevantes):

- Commit de merge en main: `2c5f51dc59ac0ba2921ddffa0753d8a024f526cb` (merge PR #6)
- Commits relevantes: `88b9eef` (feat: add task search endpoint), `46110ef` (review: comentarios de Copilot atendidos), `b2c3dcc` (fix: hallazgos del revisor).
- Verificación REST ejecutada y registrada en `evidencia/dia4/verificar.txt`.
- Tests (suite principal) en `evidencia/dia2/suite-main.txt`: `Tests run: 72, Failures: 0, Errors: 0, Skipped: 0`.

## Datos

Nombre: Esteban Nava

feature: search

url PR: https://github.com/Xeomus/taskFlow-Copilot/commit/2c5f51dc59ac0ba2921ddffa0753d8a024f526cb
