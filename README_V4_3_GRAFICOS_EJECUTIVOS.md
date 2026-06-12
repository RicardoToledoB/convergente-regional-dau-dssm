# v4.3 - Gestión Red: gráficos ejecutivos y filtros con chips

Mejoras incorporadas sobre v4.2.2:

- Filtros multi-selección con chips visibles dentro del selector.
- Panel de filtros aplicados visible bajo los filtros.
- Nuevos gráficos ejecutivos:
  - Distribución por establecimiento.
  - Distribución por categorización.
  - Distribución por tramo horario.
  - Distribución por grupo diagnóstico.
- Backend agrega `distribuciones` en la respuesta de reportes.
- Se mantiene serie agrupada por día/semana/mes.
- Se mantiene exportación CSV por cada tab.
- Se mantiene diseño responsivo para móvil, tablet y escritorio.

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

## Validación recomendada

1. Cargar casos de prueba con `cargar_casos_dau_prueba.sh`.
2. Entrar a Gestión Red.
3. Seleccionar varios establecimientos, categorías y tramos horarios.
4. Verificar chips visibles en los filtros y panel de filtros aplicados.
5. Validar que los gráficos ejecutivos cambien con los filtros.
6. Exportar CSV en cada tab.
