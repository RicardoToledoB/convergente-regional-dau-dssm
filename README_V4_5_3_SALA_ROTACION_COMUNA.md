# V4.5.3 – Sala de espera unificada + rotación 10 s + filtro comunal seguro

Fecha: 2026-09-14

## Objetivo

Ajustar el Perfil 1 del Visor Integrado de Urgencia para uso real en pantallas de sala de espera.

## Cambios

### 1. Vista 1 + Vista 2 pasan a ser un solo acceso

Para Perfil 1 ya no se presentan como botones independientes.

El acceso **Sala de espera** alterna automáticamente:

- Vista 1: información del establecimiento.
- Vista 2: red de urgencia de la comuna del establecimiento.

La rotación se realiza cada **10 segundos**.

El Perfil 2 de Gestión permanece separado y sin cambios funcionales.

### 2. Vista 2 filtrada obligatoriamente por comuna

La consulta de red del Perfil 1 ahora envía el código del establecimiento:

`GET /api/pantallas/aps/red?establecimiento={codigo}`

El backend deriva nuevamente la comuna a partir del código del establecimiento. De esta forma la Vista 2 no puede terminar mostrando accidentalmente toda la región.

Reglas de alcance en backend:

- `VISOR_APS`: siempre utiliza la comuna del establecimiento asociado a la cuenta.
- `GESTOR_COMUNAL`: siempre utiliza la comuna asociada a la cuenta.
- `ADMIN` en sala de espera: utiliza la comuna correspondiente al establecimiento seleccionado.
- `GESTOR_REGIONAL` / ADMIN en Perfil 2: mantiene el filtro regional/comunal existente.

### 3. Precarga y cambio de establecimiento

Al seleccionar un establecimiento se actualizan tanto la Vista 1 como la Vista 2 para que la transición automática sea inmediata y corresponda al mismo centro/comuna.

### 4. Presentación

Se agregó una transición visual breve (`fade`) entre ambas vistas.

## Archivos modificados

- `frontend/src/main.ts`
- `frontend/src/styles.css`
- `backend/src/main/java/cl/dssm/dau/controller/PantallaApsController.java`

## Compilación

Backend:

```bash
cd /var/www/html/convergente-regional-dau-dssm/backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api
```

Frontend:

```bash
cd /var/www/html/convergente-regional-dau-dssm/frontend
npm install
npm run build
sudo systemctl reload apache2
```

## Validación esperada

Para un visor asociado a `126801 - SAR Juan Damianovic`:

1. inicia mostrando la Vista 1 del SAR;
2. a los 10 s muestra únicamente los otros centros de **Punta Arenas**;
3. a los 10 s vuelve a la Vista 1;
4. repite el ciclo sin intervención del usuario.
