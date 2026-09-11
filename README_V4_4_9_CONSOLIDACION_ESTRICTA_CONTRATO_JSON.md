# V4.4.9 – Consolidación estricta según contrato JSON DAU

## Objetivo
Alinear el estado operacional del Convergente con la documentación de integración RCE–DAU.

## Reglas
- `ALTA_MEDICA`: requiere `fechaAlta` **y** `horaAlta`.
- `ATENCION_MEDICA`: requiere `fechaAtencion` **y** `horaAtencion`.
- `CATEGORIZADA`: existe `primeraCategorizacion` o `ultimaCategorizacion`.
- En otro caso: `ADMISION`.
- El tipo de evento inferido/nombre de archivo se conserva para auditoría, pero no fuerza el estado consolidado.
- Los `null` no eliminan información previamente consolidada.
- El estado continúa siendo monotónico: un evento anterior reenviado no hace retroceder la atención.
- Pantalla APS: no aplica ventana artificial de 24 horas.
- Permanecen activos `ADMISION`, `CATEGORIZADA` y `ATENCION_MEDICA`; salen al consolidar `ALTA_MEDICA`.
- La clasificación `En atención` exige fecha y hora de atención.
- No modifica ni elimina eventos de `dau_eventos_recibidos`.

## Nota operacional
Si RAYEN deja de mostrar una atención pero no envía datos de alta/cierre, Convergente la mantendrá abierta. Esto permite evidenciar diferencias entre el estado operacional del sistema fuente y el contrato de integración recibido.
