# V4.5.4 · Auditoría ADMIN por RUT en Visor Integrado

## Objetivo
Permitir que un usuario con rol `ADMIN` audite las cantidades mostradas en las tres vistas del **Visor Integrado de Urgencia** sin exponer RUT a los perfiles de sala de espera o gestores.

## Comportamiento
Al posicionar el mouse sobre una cantidad auditable se muestra una ventana flotante con los pacientes que componen ese número:

- RUT
- ID DAU
- ID Atención (cuando existe)
- Categorización
- Estado consolidado

La ventana se mantiene visible mientras el puntero esté sobre ella y se cierra al retirarlo. La rotación automática de la sala de espera cierra la auditoría antes de cambiar de vista.

## Vista 1 · Mi urgencia
Auditable por ADMIN:

- Pacientes en espera
- Pacientes en atención
- Cantidad de pacientes de C1, C2, C3, C4, C5 y S/C

## Vista 2 · Red de mi comuna
Auditable por ADMIN en cada establecimiento:

- Espera
- Atención

## Perfil 2 · Gestión
Auditable por ADMIN:

- Espera de cada establecimiento
- Atención de cada establecimiento
- KPI Total en espera
- KPI En atención

Los KPI consolidados respetan el alcance actualmente seleccionado: comuna o región completa.

## Endpoint nuevo

```http
GET /api/pantallas/aps/auditoria
```

Parámetros:

- `establecimiento` opcional
- `comuna` opcional
- `grupo`: `ACTIVOS`, `ESPERA` o `ATENCION`
- `categoria` opcional: C1-C5/S/C o equivalentes 01-05

Ejemplo:

```http
GET /api/pantallas/aps/auditoria?establecimiento=126801&grupo=ESPERA&categoria=C4
```

### Seguridad
El endpoint está restringido mediante:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Aunque `VISOR_APS`, `GESTOR_COMUNAL` y `GESTOR_REGIONAL` pueden consumir las pantallas operacionales, **no pueden obtener los RUT de auditoría**.

## Consistencia de datos
La auditoría utiliza exactamente las mismas reglas que la pantalla:

- `esActivoOperacional(...)`
- `estaEnEspera(...)`
- `estaEnAtencion(...)`
- categorización consolidada

Por lo tanto, el número de RUT devuelto debe coincidir con la cantidad sobre la cual se posicionó el mouse.

## Privacidad
No se incorporan RUT a las respuestas normales del visor. La información identificable se obtiene sólo bajo demanda por el endpoint ADMIN de auditoría.

## Archivos principales modificados

- `backend/src/main/java/cl/dssm/dau/controller/PantallaApsController.java`
- `backend/src/main/java/cl/dssm/dau/service/PantallaApsService.java`
- `backend/src/main/java/cl/dssm/dau/config/SecurityConfig.java`
- `backend/src/main/java/cl/dssm/dau/dto/PantallaApsAuditoriaResponse.java`
- `frontend/src/main.ts`
- `frontend/src/styles.css`
