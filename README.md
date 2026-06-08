# Convergente Regional DAU DSSM v3.1

Proyecto backend + frontend para recepción, consolidación y monitoreo regional de eventos DAU.

## Producción DSSM

- API backend: `https://convergente-api.dssm.cl`
- Frontend: `https://convergente.dssm.cl`
- Servidor/IP: `10.8.74.156`
- Endpoint recepción DAU: `POST https://convergente-api.dssm.cl/api/integration/dau/eventos`
- Swagger: `https://convergente-api.dssm.cl/swagger-ui/index.html`

## Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/convergente-regional-dau-0.0.1-SNAPSHOT.jar
```

Variables recomendadas en producción:

```bash
export SERVER_PORT=8086
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=convergente_dau_dssm_db
export DB_USER=convergente_dau_user
export DB_PASS='CAMBIAR_CLAVE_PRODUCTIVA'
export JWT_SECRET='CAMBIAR_POR_SECRETO_PRODUCTIVO_MUY_LARGO_DE_64_CARACTERES_MINIMO'
export CORS_ALLOWED_ORIGINS='https://convergente.dssm.cl'
export JPA_DDL_AUTO=update
export JPA_SHOW_SQL=false
```

## Frontend

El frontend Angular Material apunta por defecto a:

```text
https://convergente-api.dssm.cl/api
```

Para desarrollo local, antes de iniciar sesión en el navegador puede ejecutarse:

```js
localStorage.setItem('apiUrl', 'http://localhost:8086/api')
```

Compilación:

```bash
cd frontend
npm install
npm run build
```

## Usuarios base

- `admin` / `admin123`
- `HCM_Integracion` / `Hcm2026*`
- `RAYEN_Integracion` / `Rayen2026*`

Cambiar claves antes de producción real.

## Configuración local y producción del Frontend

Esta versión detecta automáticamente el origen desde donde se abre el frontend:

- Si se abre en `http://localhost:4200`, usa automáticamente `http://localhost:8086/api`.
- Si se abre en `https://convergente.dssm.cl`, usa automáticamente `https://convergente-api.dssm.cl/api`.
- Si se abre por IP interna `10.8.74.156`, intenta usar `http(s)://10.8.74.156:8086/api` para pruebas internas.

También se puede forzar manualmente la API desde la consola del navegador:

```js
localStorage.setItem('apiUrl', 'http://localhost:8086/api');
```

Para volver al modo automático:

```js
localStorage.removeItem('apiUrl');
```

## CORS backend

El backend viene configurado para aceptar por defecto:

```properties
https://convergente.dssm.cl
http://convergente.dssm.cl
http://localhost:4200
http://127.0.0.1:4200
http://10.8.74.156
https://10.8.74.156
```

En producción se recomienda definir explícitamente la variable de entorno `CORS_ALLOWED_ORIGINS` con los dominios finales autorizados.
# convergente-regional-dau-dssm
