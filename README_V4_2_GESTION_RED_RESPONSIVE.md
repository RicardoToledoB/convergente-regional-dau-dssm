# Convergente Regional DAU DSSM v4.2

Versión de ajuste fino para Gestión Red y módulos técnicos.

## Mejoras incluidas

1. Bitácora de eventos corregida
   - Ajuste de ordenamiento backend usando propiedades JPA correctas (`fechaRecepcion` y `fechaActualizacion`).
   - Validación del endpoint `/api/dau/eventos` con paginación y filtros.

2. Filtros multi-selección reales
   - Dispositivo consulta.
   - Establecimiento.
   - Sexo.
   - Categorización.
   - Origen.
   - Grupo diagnóstico.
   - Tramo horario.
   - Resumen de selección visible con chips.

3. Botones Buscar / Limpiar ordenados
   - Se alinean horizontalmente en escritorio.
   - Se adaptan verticalmente en celulares.

4. Nombres descriptivos
   - Establecimientos muestran nombre descriptivo.
   - Categorías muestran C1/C2/C3/C4/C5 con descripción.
   - Estados se muestran en lenguaje usuario: Alta médica, Atención médica, etc.

5. Serie agrupada mejorada
   - Barras visuales responsivas por día/semana/mes.

6. Exportación CSV
   - Exportación por cada tab de Gestión Red.
   - Incluye BOM UTF-8 para mejor apertura en Excel.

7. Responsividad completa
   - Menú lateral tipo overlay en pantallas pequeñas.
   - Botón hamburguesa móvil.
   - Filtros a una columna en celulares.
   - Tablas con scroll horizontal seguro.
   - Cards y gráficos adaptados a móvil.

## Prueba local

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

```bash
cd backend
mvn clean package -DskipTests
sudo systemctl restart convergente-dau-api

cd ../frontend
npm install
npm run build
sudo systemctl reload apache2
```

## Nota

No elimina ni cambia el flujo de recepción DAU. Solo mejora monitoreo, reportería, UI y responsividad.
