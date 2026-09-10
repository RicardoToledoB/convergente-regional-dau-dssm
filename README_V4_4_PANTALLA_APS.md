# v4.4 - Pantalla APS en Red

Se agrega una nueva vista para monitoreo visual en pantallas de establecimientos APS.

## Funcionalidades

- Nueva opción de menú: **Pantalla APS**.
- Selector superior de establecimiento a monitorear.
- Botones rápidos para cambiar entre establecimientos.
- KPIs visibles para pantalla:
  - Pacientes en espera.
  - Pacientes en atención.
  - Tiempo promedio de espera.
  - Tiempo máximo de espera.
- Banda de tiempos por categorización: C1, C2, C3, C4, C5 y S/C.
- Tabla pública/anónima de pacientes en espera o atención.
- Paginación visual para pantallas.
- Gráficos inferiores:
  - Distribución por establecimiento.
  - Distribución por categorización.
  - Pacientes por tramo horario.
- Diseño responsive para escritorio, TV, tablet y celular.

## Backend

Nuevo endpoint:

```text
GET /api/pantallas/aps?establecimiento=201079
```

Respuesta protegida contra exposición de datos personales: la pantalla no entrega RUN, nombre completo ni identificadores clínicos sensibles. El paciente se muestra anonimizado como `Paciente XXX`.

## Archivos agregados

```text
backend/src/main/java/cl/dssm/dau/controller/PantallaApsController.java
backend/src/main/java/cl/dssm/dau/service/PantallaApsService.java
backend/src/main/java/cl/dssm/dau/dto/PantallaApsResponse.java
docs/panel_de_monitoreo_aps_en_red.png
```

## Archivos modificados

```text
backend/src/main/java/cl/dssm/dau/repository/DauAttentionRepository.java
backend/src/main/java/cl/dssm/dau/config/SecurityConfig.java
frontend/src/main.ts
frontend/src/styles.css
```

## Compilación backend

```bash
cd backend
mvn clean package -DskipTests
```

## Ejecución local frontend

```bash
cd frontend
npm install
ng serve -o
```

## Producción

```bash
cd /var/www/html/convergente-regional-dau-dssm
git pull

cd backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api

cd ../frontend
npm install
npm run build
sudo systemctl reload apache2
```

## Observación

La pantalla calcula pacientes activos considerando atenciones que no estén en estado `ALTA_MEDICA`. Para establecimientos con solo atenciones finalizadas, la vista mostrará cero pacientes activos, lo que es esperable.
