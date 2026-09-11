# Convergente Regional DAU DSSM – V4.4.5

## Objetivo

Evolución de la V4.4.4 orientada a convertir la Pantalla APS en una vista regional dinámica y a facilitar la investigación administrativa de DAU abiertos sin nuevos eventos, sin modificar ni cerrar datos clínicos.

## Cambios principales

### 1. Pantalla APS multestablecimiento dinámica

- La pantalla inicia en **Red completa / Todos los establecimientos**.
- Nuevo endpoint `GET /api/pantallas/aps/establecimientos`.
- El listado se obtiene desde los códigos de establecimiento presentes en `dau_atenciones_consolidadas`.
- El frontend deja de depender de una lista fija para la Pantalla APS.
- Se eliminan los botones rápidos de establecimientos; quedan únicamente el selector y el botón Recargar.
- Al seleccionar un establecimiento se utiliza el parámetro `establecimiento`; al elegir Red completa el parámetro se omite y se consulta toda la red.

### 2. Menú lateral colapsable

- El menú lateral puede contraerse a una barra de iconos y volver a expandirse.
- El estado se guarda en `localStorage` con la clave `sidebarCollapsed`.
- Los iconos muestran tooltip en modo contraído.
- En móvil se mantiene el comportamiento de menú superpuesto.

### 3. Detalle de DAU sin nuevos eventos

Los KPI de 3, 6, 12 y 24 horas del Dashboard son navegables.

Nuevos endpoints:

- `GET /api/dau/sin-eventos?horas=24&page=0&size=20`
- `GET /api/dau/sin-eventos/resumen?horas=24`

La vista muestra:

- ID DAU / ID Atención.
- Establecimiento.
- Estado actual.
- Categoría.
- Fecha del último evento.
- Horas aproximadas sin novedades.
- Acceso al detalle del DAU.
- Resumen de casos por establecimiento.

La lógica considera sólo estados `ADMISION`, `CATEGORIZADA` y `ATENCION_MEDICA`, sin fecha de alta, y utiliza `fecha_ultimo_evento`.

## Seguridad de datos

Esta versión **no**:

- genera altas automáticas;
- elimina DAU;
- modifica `estado_actual`;
- modifica ni religa `dau_eventos_recibidos`;
- cambia la regla operacional de 24 horas de la Pantalla APS.

Los nuevos indicadores son exclusivamente de monitoreo y diagnóstico administrativo.

## Despliegue

Backend:

```bash
cd /var/www/html/convergente-regional-dau-dssm/backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api
sudo systemctl status convergente-dau-api --no-pager
```

Frontend:

```bash
cd /var/www/html/convergente-regional-dau-dssm/frontend
npm install
npm run build
sudo systemctl reload apache2
```

Después del despliegue, realizar recarga forzada del navegador.
