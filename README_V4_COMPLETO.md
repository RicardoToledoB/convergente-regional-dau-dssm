# Convergente Regional DAU DSSM v4.0 - Gestión Red

Versión completa basada en v3.2 con incorporación inicial de módulos de gestión.

## Incluye

### Backend
- Nuevo paquete `cl.dssm.dau.reports`
- Endpoints:
  - POST `/api/reportes/consultas-red/demanda/buscar`
  - POST `/api/reportes/consultas-red/atenciones/buscar`
  - POST `/api/reportes/tiempos-espera/categorizacion/buscar`
  - POST `/api/reportes/tiempos-espera/atencion/buscar`
  - POST `/api/reportes/tiempos-espera/egreso/buscar`
  - POST `/api/reportes/{modulo}/{submodulo}/exportar-csv`

### Frontend
- Nueva opción de menú: `Gestión Red`
- Tabs:
  - Demanda
  - Atenciones
  - T. Categorización
  - T. Atención
  - T. Egreso
- Filtros múltiples:
  - Dispositivo consulta
  - Establecimiento
  - Sexo
  - Categorización
  - Origen
  - Grupo diagnóstico
  - Tramo horario
  - Edad desde/hasta
  - Fecha desde/hasta
  - Agrupar por día/semana/mes
- Cards resumen
- Tabla paginada
- Exportación CSV
- Botón editar usuario en módulo Usuarios

## Instalación sugerida

```bash
cd /var/www/html/convergente-regional-dau-dssm

git pull
# Reemplazar contenido del proyecto por esta versión o aplicar cambios sobre la rama actual.

cd backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api

cd ../frontend
npm install
npm run build
sudo systemctl reload apache2
```

## Validación rápida API reportes

```bash
curl -k -X POST https://convergente-api.dssm.cl/api/reportes/consultas-red/demanda/buscar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"establecimientos":[126100],"fechaDesde":"2026-06-01","fechaHasta":"2026-06-30","agruparPor":"DIA","page":0,"size":20}'
```
