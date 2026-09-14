# V4.5.5 – Exclusión operacional de “Tratamiento o Curación”

## Objetivo
Ajustar el Visor Integrado de Urgencia de acuerdo con la validación formal entregada por RAYEN: las admisiones cuyo `motivoConsulta` corresponde a **TRATAMIENTO O CURACIÓN** pertenecen a un flujo que sólo considera admisión y no continúa con categorización, atención médica ni alta.

## Cambio funcional
Los DAU cuyo `motivoConsulta`, luego de una normalización conservadora, sea exactamente:

```text
TRATAMIENTO O CURACION
```

quedan **excluidos únicamente de la capa operacional** del Visor Integrado.

La normalización tolera:
- mayúsculas/minúsculas;
- `CURACIÓN` / `CURACION`;
- espacios iniciales/finales o espacios repetidos.

No se realizan coincidencias parciales. Por ejemplo, una descripción clínica que contenga la palabra `curación` no queda excluida a menos que el valor completo normalizado sea exactamente `TRATAMIENTO O CURACION`.

## Impacto
Se excluyen de:
- Pacientes en espera.
- Pacientes en atención.
- C1–C5 y S/C operacional.
- Tiempo promedio y máximo de espera.
- Vista 1 de sala de espera.
- Vista 2 de red comunal.
- Perfil 2 de gestión comunal/regional y sus KPI.
- Auditoría ADMIN del visor, para que siga cuadrando exactamente con las cantidades visibles.

Se conservan sin cambios en:
- `dau_eventos_recibidos`.
- `dau_atenciones_consolidadas`.
- Monitor DAU / trazabilidad histórica.
- API `GET /api/patient/{rut}`.
- Eventos y auditoría histórica.

## Implementación
La regla se incorpora dentro de `PantallaApsService.esActivoOperacional(...)`, por lo que todas las vistas y agregaciones que reutilizan esa función reciben el ajuste de forma consistente.

No se generan altas artificiales, no se borran registros y no se modifica el estado clínico consolidado.

## Despliegue
Esta versión modifica sólo backend:

```bash
cd /var/www/html/convergente-regional-dau-dssm/backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api
sudo systemctl status convergente-dau-api --no-pager
```

No es necesario recompilar Angular para este cambio.
