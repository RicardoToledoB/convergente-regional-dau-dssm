# V4.4.6 – Vigencia Operacional Diaria APS

## Objetivo

Ajustar exclusivamente la **Pantalla APS** para evitar que atenciones antiguas que siguen abiertas en la integración, pero sin nuevas novedades, continúen apareciendo como pacientes operativamente activos.

## Regla implementada

Una atención se considera visible en Pantalla APS cuando:

- no está en `ALTA_MEDICA` ni `ERROR`;
- no tiene `fechaAlta`; y
- fue admitida **hoy**, o bien su `fecha_ultimo_evento` corresponde **a hoy**.

En pseudológica:

```text
VISIBLE_APS = abierta AND (admision_hoy OR ultimo_evento_hoy)
```

Esto permite mantener visible una atención iniciada el día anterior cuando sigue recibiendo eventos durante la jornada actual, sin arrastrar casos antiguos que no han recibido novedades.

## Alcance y seguridad

La versión **no modifica**:

- `dau_atenciones_consolidadas`;
- `dau_eventos_recibidos`;
- `estado_actual`;
- altas, abandonos ni cierres;
- Dashboard, Monitor DAU o trazabilidad histórica.

No se crean altas artificiales ni se eliminan registros. La regla se aplica únicamente al método `PantallaApsService.esActivoOperacional(...)`.

## Cambio respecto de V4.4.5.1

Se elimina para Pantalla APS la regla genérica de ventana móvil de 24 horas basada sólo en la admisión. El parámetro `pantalla.aps.max-active-hours` deja de controlar esta vista, porque podía excluir atenciones legítimas largas o mantener hasta 24 horas casos sin novedades.

## Validación esperada

Después del despliegue, comparar nuevamente la pantalla oficial RAYEN con Convergente. El objetivo de esta versión es eliminar el arrastre evidente de jornadas anteriores; **no fuerza artificialmente que los conteos coincidan** si la integración no entrega el evento/condición de cierre utilizado internamente por RAYEN.
