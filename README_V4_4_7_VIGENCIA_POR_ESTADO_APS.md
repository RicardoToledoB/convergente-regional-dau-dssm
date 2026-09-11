# V4.4.7 – Vigencia por Estado en Pantalla APS

Esta versión ajusta exclusivamente la regla de vigencia de la Pantalla APS. No modifica datos consolidados ni eventos recibidos.

## Regla operacional

- **ADMISION**: visible sólo cuando `idAtencion == idDau` (identificador temporal aún no progresado).
- **ATENCION_MEDICA**: visible sólo si `fechaUltimoEvento` está dentro de las últimas **3 horas**.
- **CATEGORIZADA**: mantiene temporalmente la regla V4.4.6: admisión hoy o último evento hoy.
- **ALTA_MEDICA / ERROR / fechaAlta informada**: excluidos.

## Alcance y seguridad

- No altera `dau_atenciones_consolidadas`.
- No altera `dau_eventos_recibidos`.
- No genera `ALTA_MEDICA` artificial.
- No elimina registros.
- El cambio sólo afecta la vista operacional `/api/pantallas/aps`.

## Objetivo de validación

En SAR Juan Damianovic (126801), la simulación previa mostró:

- ADMISION: 3 visibles, coincidente con la pantalla RAYEN observada.
- ATENCION_MEDICA: el umbral de 3 horas busca aproximar los 3 pacientes en atención mostrados por RAYEN.
- CATEGORIZADA: se mantiene sin un filtro adicional hasta identificar con evidencia la condición que RAYEN utiliza para retirar categorizados sin enviar alta.
