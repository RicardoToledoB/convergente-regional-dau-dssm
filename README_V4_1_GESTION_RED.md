# v4.1 - Gestión Red de Urgencia

Mejoras incorporadas sobre v4.0:

## Frontend
- Se separa visualmente Gestión Red en dos bloques:
  - Consultas en la Red
  - Tiempos de Espera
- Consultas en la Red contiene:
  - Demanda
  - Atenciones
- Tiempos de Espera contiene:
  - Categorización
  - Atención médica
  - Egreso total
- Filtros múltiples con resumen mediante chips seleccionados.
- Cards KPI diferenciadas por tipo de reporte:
  - Consultas: DAU/Atenciones, promedio por periodo, establecimientos, categoría frecuente, estado frecuente.
  - Tiempos: total, promedio, mediana, mínimo y máximo en minutos.
- Gráfico simple de barras para serie agrupada por día, semana o mes.
- Mantiene exportación CSV.
- Mantiene edición de usuarios.
- Mantiene corrección del módulo Eventos.

## Ejecución local
Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
ng serve -o
```

## Producción
En el servidor:

```bash
cd /var/www/html/convergente-regional-dau-dssm
cd backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api

cd ../frontend
npm install
npm run build
sudo systemctl reload apache2
```
